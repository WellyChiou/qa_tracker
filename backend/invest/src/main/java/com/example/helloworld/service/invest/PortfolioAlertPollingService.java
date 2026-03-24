package com.example.helloworld.service.invest;

import com.example.helloworld.entity.invest.*;
import com.example.helloworld.repository.invest.PortfolioAlertEventRepository;
import com.example.helloworld.repository.invest.PortfolioAlertSettingRepository;
import com.example.helloworld.repository.invest.PortfolioRepository;
import com.example.helloworld.repository.invest.SchedulerJobLogRepository;
import com.example.helloworld.repository.invest.StockPriceDailyRepository;
import com.example.helloworld.service.invest.auth.InvestCurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class PortfolioAlertPollingService {

    public static final String JOB_NAME_ALERT_POLLING = "PORTFOLIO_ALERT_POLLING";
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Taipei");
    private static final int PRICE_SCALE = 4;
    private static final int PERCENT_SCALE = 2;

    private final PortfolioAlertSettingRepository portfolioAlertSettingRepository;
    private final PortfolioAlertEventRepository portfolioAlertEventRepository;
    private final StockPriceDailyRepository stockPriceDailyRepository;
    private final PortfolioRepository portfolioRepository;
    private final SchedulerJobLogRepository schedulerJobLogRepository;
    private final InvestCurrentUserService investCurrentUserService;

    @Value("${invest.alert.abnormal-drop.threshold-percent:2.0}")
    private BigDecimal abnormalDropThresholdPercent;

    @Value("${invest.alert.abnormal-drop.window-minutes:10}")
    private int abnormalDropWindowMinutes;

    @Value("${invest.alert.abnormal-drop.cooldown-minutes:10}")
    private int abnormalDropCooldownMinutes;

    /**
     * Step 4 最小版：以記憶體保留輪詢基準價格，僅供 ABNORMAL_DROP 判斷。
     * 限制：
     * 1) 服務重啟後基準會重置。
     * 2) 多實例下不保證一致。
     * 3) 去重仍以資料庫事件查詢為主，不能只依賴記憶體快取。
     */
    private final Map<Long, PriceSnapshot> abnormalDropPriceCache = new ConcurrentHashMap<>();

    public PortfolioAlertPollingService(PortfolioAlertSettingRepository portfolioAlertSettingRepository,
                                        PortfolioAlertEventRepository portfolioAlertEventRepository,
                                        StockPriceDailyRepository stockPriceDailyRepository,
                                        PortfolioRepository portfolioRepository,
                                        SchedulerJobLogRepository schedulerJobLogRepository,
                                        InvestCurrentUserService investCurrentUserService) {
        this.portfolioAlertSettingRepository = portfolioAlertSettingRepository;
        this.portfolioAlertEventRepository = portfolioAlertEventRepository;
        this.stockPriceDailyRepository = stockPriceDailyRepository;
        this.portfolioRepository = portfolioRepository;
        this.schedulerJobLogRepository = schedulerJobLogRepository;
        this.investCurrentUserService = investCurrentUserService;
    }

    public BatchExecutionResult runForCurrentUser() {
        String userId = investCurrentUserService.resolveCurrentUserUid();
        return runForUsers(List.of(userId), "手動觸發（當前登入者）");
    }

    public BatchExecutionResult runForAllActiveUsers() {
        List<String> userIds = portfolioRepository.findDistinctActiveUserIds();
        return runForUsers(userIds, "排程觸發（全部啟用持股使用者）");
    }

    private BatchExecutionResult runForUsers(List<String> userIds, String triggerDescription) {
        LocalDate runDate = LocalDate.now(DEFAULT_ZONE);
        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);

        SchedulerJobLog jobLog = new SchedulerJobLog();
        jobLog.setJobName(JOB_NAME_ALERT_POLLING);
        jobLog.setRunDate(runDate);
        jobLog.setStatus(SchedulerJobStatusCode.RUNNING);
        jobLog.setStartedAt(now);
        jobLog.setMessage("警示輪詢執行中：" + triggerDescription);
        jobLog = schedulerJobLogRepository.save(jobLog);

        int processedUserCount = 0;
        int processedPortfolioCount = 0;
        int eventCount = 0;

        try {
            for (String userId : userIds) {
                processedUserCount += 1;
                List<PortfolioAlertSetting> settings = portfolioAlertSettingRepository.findEnabledByUserId(userId);
                for (PortfolioAlertSetting setting : settings) {
                    processedPortfolioCount += 1;
                    eventCount += evaluateSetting(setting, now);
                }
            }

            jobLog.setStatus(SchedulerJobStatusCode.SUCCESS);
            jobLog.setFinishedAt(LocalDateTime.now(DEFAULT_ZONE));
            jobLog.setMessage(String.format(
                "警示輪詢完成（%s）。使用者 %d 位，持股 %d 檔，新增事件 %d 筆。",
                triggerDescription, processedUserCount, processedPortfolioCount, eventCount
            ));
            schedulerJobLogRepository.save(jobLog);

            return new BatchExecutionResult(
                jobLog.getId(),
                jobLog.getJobName(),
                runDate,
                jobLog.getStatus().name(),
                processedUserCount,
                processedPortfolioCount,
                eventCount,
                jobLog.getMessage()
            );
        } catch (Exception e) {
            jobLog.setStatus(SchedulerJobStatusCode.FAILED);
            jobLog.setFinishedAt(LocalDateTime.now(DEFAULT_ZONE));
            jobLog.setMessage("警示輪詢失敗：" + e.getMessage());
            schedulerJobLogRepository.save(jobLog);
            throw e;
        }
    }

    private int evaluateSetting(PortfolioAlertSetting setting, LocalDateTime now) {
        if (!Boolean.TRUE.equals(setting.getEnabled())) {
            return 0;
        }

        Portfolio portfolio = setting.getPortfolio();
        Optional<StockPriceDaily> latestOptional = stockPriceDailyRepository
            .findTopByStockIdOrderByTradeDateDesc(portfolio.getStock().getId());
        if (latestOptional.isEmpty()) {
            return 0;
        }

        StockPriceDaily latest = latestOptional.get();
        LocalDateTime alertDayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime alertDayEnd = alertDayStart.plusDays(1);

        int createdCount = 0;
        BigDecimal currentPrice = latest.getClosePrice().setScale(PRICE_SCALE, RoundingMode.HALF_UP);

        // 空值規則：stopLossPrice = null 不檢查 STOP_LOSS
        if (setting.getStopLossPrice() != null
            && currentPrice.compareTo(setting.getStopLossPrice()) <= 0
            && !portfolioAlertEventRepository.existsByPortfolioIdAndTriggerTypeAndTriggeredAtBetween(
                portfolio.getId(), AlertTriggerTypeCode.STOP_LOSS, alertDayStart, alertDayEnd
            )) {
            String message = String.format(
                "觸發停損警示：目前價格 %.4f 已低於或等於停損價 %.4f。",
                currentPrice, setting.getStopLossPrice()
            );
            createEvent(portfolio, AlertTriggerTypeCode.STOP_LOSS, currentPrice, message, now);
            createdCount += 1;
        }

        // 空值規則：alertDropPercent = null 不檢查 DROP_PERCENT
        if (setting.getAlertDropPercent() != null
            && latest.getChangePercent() != null
            && latest.getChangePercent().compareTo(setting.getAlertDropPercent().negate()) <= 0
            && !portfolioAlertEventRepository.existsByPortfolioIdAndTriggerTypeAndTriggeredAtBetween(
                portfolio.getId(), AlertTriggerTypeCode.DROP_PERCENT, alertDayStart, alertDayEnd
            )) {
            BigDecimal dropPercent = latest.getChangePercent().abs().setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
            String message = String.format(
                "觸發跌幅警示：當日跌幅 %.2f%% 已達設定門檻 %.2f%%。",
                dropPercent, setting.getAlertDropPercent()
            );
            createEvent(portfolio, AlertTriggerTypeCode.DROP_PERCENT, dropPercent, message, now);
            createdCount += 1;
        }

        // enabled = true 時，ABNORMAL_DROP 獨立生效（不依賴 stopLossPrice / alertDropPercent）
        createdCount += evaluateAbnormalDrop(portfolio, latest, currentPrice, now);
        return createdCount;
    }

    private int evaluateAbnormalDrop(Portfolio portfolio,
                                     StockPriceDaily latest,
                                     BigDecimal currentPrice,
                                     LocalDateTime now) {
        Long portfolioId = portfolio.getId();
        PriceSnapshot previous = abnormalDropPriceCache.get(portfolioId);
        abnormalDropPriceCache.put(portfolioId, new PriceSnapshot(currentPrice, now, latest.getTradeDate()));

        if (previous == null) {
            return 0;
        }
        if (!latest.getTradeDate().equals(previous.tradeDate())) {
            return 0;
        }
        if (previous.price().compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        long minutes = Duration.between(previous.sampledAt(), now).toMinutes();
        if (minutes <= 0 || minutes > abnormalDropWindowMinutes) {
            return 0;
        }

        BigDecimal dropPercent = previous.price()
            .subtract(currentPrice)
            .divide(previous.price(), 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
        if (dropPercent.compareTo(abnormalDropThresholdPercent) < 0) {
            return 0;
        }

        LocalDateTime cooldownCutoff = now.minusMinutes(abnormalDropCooldownMinutes);
        PortfolioAlertEvent latestAbnormal = portfolioAlertEventRepository
            .findTopByPortfolioIdAndTriggerTypeOrderByTriggeredAtDesc(portfolioId, AlertTriggerTypeCode.ABNORMAL_DROP)
            .orElse(null);
        if (latestAbnormal != null && latestAbnormal.getTriggeredAt() != null
            && !latestAbnormal.getTriggeredAt().isBefore(cooldownCutoff)) {
            return 0;
        }

        String message = String.format(
            "觸發短時異常下跌警示：最近 %d 分鐘下跌 %.2f%%，超過門檻 %.2f%%。",
            abnormalDropWindowMinutes,
            dropPercent,
            abnormalDropThresholdPercent
        );
        createEvent(portfolio, AlertTriggerTypeCode.ABNORMAL_DROP, dropPercent, message, now);
        return 1;
    }

    private void createEvent(Portfolio portfolio,
                             AlertTriggerTypeCode triggerType,
                             BigDecimal triggerValue,
                             String message,
                             LocalDateTime triggeredAt) {
        PortfolioAlertEvent event = new PortfolioAlertEvent();
        event.setPortfolio(portfolio);
        event.setTriggerType(triggerType);
        event.setTriggerValue(triggerValue);
        event.setMessage(message);
        event.setTriggeredAt(triggeredAt);
        portfolioAlertEventRepository.save(event);
    }

    public record BatchExecutionResult(
        Long jobLogId,
        String jobName,
        LocalDate runDate,
        String status,
        Integer processedUserCount,
        Integer processedPortfolioCount,
        Integer eventCount,
        String message
    ) {}

    private record PriceSnapshot(BigDecimal price, LocalDateTime sampledAt, LocalDate tradeDate) {}
}
