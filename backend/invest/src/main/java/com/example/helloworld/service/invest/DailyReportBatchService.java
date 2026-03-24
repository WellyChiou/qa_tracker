package com.example.helloworld.service.invest;

import com.example.helloworld.dto.invest.DailyReportSummaryDto;
import com.example.helloworld.dto.invest.DailyReportTopRiskHoldingDto;
import com.example.helloworld.dto.invest.DashboardDistributionItemDto;
import com.example.helloworld.dto.invest.PortfolioRiskResultDetailDto;
import com.example.helloworld.entity.invest.*;
import com.example.helloworld.repository.invest.DailyReportRepository;
import com.example.helloworld.repository.invest.PortfolioDailySnapshotRepository;
import com.example.helloworld.repository.invest.PortfolioRepository;
import com.example.helloworld.repository.invest.SchedulerJobLogRepository;
import com.example.helloworld.repository.invest.StockPriceDailyRepository;
import com.example.helloworld.service.invest.auth.InvestCurrentUserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class DailyReportBatchService {

    public static final String JOB_NAME_DAILY_PORTFOLIO_RISK_REPORT = "DAILY_PORTFOLIO_RISK_REPORT";
    private static final String DISCLAIMER = "本系統為輔助工具，不保證獲利，分析結果不可取代自主判斷。";
    private static final List<String> NOVICE_REMINDERS = List.of(
        "本系統為輔助工具，不保證獲利",
        "系統結果不可取代自主判斷",
        "單一訊號不代表一定上漲或下跌",
        "高波動股票不適合只看短線漲跌做決策",
        "不要因短線漲跌而情緒化操作",
        "不要盲目攤平",
        "不要追高"
    );

    private static final int PRICE_SCALE = 4;
    private static final int MONEY_SCALE = 2;

    private final PortfolioRepository portfolioRepository;
    private final StockPriceDailyRepository stockPriceDailyRepository;
    private final PortfolioDailySnapshotRepository portfolioDailySnapshotRepository;
    private final DailyReportRepository dailyReportRepository;
    private final SchedulerJobLogRepository schedulerJobLogRepository;
    private final PortfolioRiskResultService portfolioRiskResultService;
    private final InvestCurrentUserService investCurrentUserService;
    private final InvestFxConversionService investFxConversionService;
    private final ObjectMapper objectMapper;

    public DailyReportBatchService(PortfolioRepository portfolioRepository,
                                   StockPriceDailyRepository stockPriceDailyRepository,
                                   PortfolioDailySnapshotRepository portfolioDailySnapshotRepository,
                                   DailyReportRepository dailyReportRepository,
                                   SchedulerJobLogRepository schedulerJobLogRepository,
                                   PortfolioRiskResultService portfolioRiskResultService,
                                   InvestCurrentUserService investCurrentUserService,
                                   InvestFxConversionService investFxConversionService,
                                   ObjectMapper objectMapper) {
        this.portfolioRepository = portfolioRepository;
        this.stockPriceDailyRepository = stockPriceDailyRepository;
        this.portfolioDailySnapshotRepository = portfolioDailySnapshotRepository;
        this.dailyReportRepository = dailyReportRepository;
        this.schedulerJobLogRepository = schedulerJobLogRepository;
        this.portfolioRiskResultService = portfolioRiskResultService;
        this.investCurrentUserService = investCurrentUserService;
        this.investFxConversionService = investFxConversionService;
        this.objectMapper = objectMapper;
    }

    public BatchExecutionResult runForCurrentUser(LocalDate reportDate) {
        String userId = investCurrentUserService.resolveCurrentUserUid();
        return runForUsers(reportDate, List.of(userId), "手動觸發（當前登入者）");
    }

    public BatchExecutionResult runForAllActiveUsers(LocalDate reportDate) {
        List<String> userIds = portfolioRepository.findDistinctActiveUserIds();
        return runForUsers(reportDate, userIds, "排程觸發（全部啟用持股使用者）");
    }

    private BatchExecutionResult runForUsers(LocalDate reportDate, List<String> userIds, String triggerDescription) {
        LocalDate effectiveReportDate = reportDate == null
            ? LocalDate.now(ZoneId.of("Asia/Taipei"))
            : reportDate;

        SchedulerJobLog jobLog = new SchedulerJobLog();
        jobLog.setJobName(JOB_NAME_DAILY_PORTFOLIO_RISK_REPORT);
        jobLog.setRunDate(effectiveReportDate);
        jobLog.setStatus(SchedulerJobStatusCode.RUNNING);
        jobLog.setStartedAt(LocalDateTime.now());
        jobLog.setMessage("批次執行中：" + triggerDescription);
        jobLog = schedulerJobLogRepository.save(jobLog);

        int processedUserCount = 0;
        int snapshotCount = 0;
        int riskResultCount = 0;
        int reportCount = 0;
        Long latestReportId = null;

        try {
            for (String userId : userIds) {
                UserReportGenerationResult perUser = generateForUser(userId, effectiveReportDate);
                processedUserCount += 1;
                snapshotCount += perUser.snapshotCount();
                riskResultCount += perUser.riskResultCount();
                if (perUser.reportId() != null) {
                    reportCount += 1;
                    latestReportId = perUser.reportId();
                }
            }

            jobLog.setStatus(SchedulerJobStatusCode.SUCCESS);
            jobLog.setFinishedAt(LocalDateTime.now());
            jobLog.setMessage(String.format(
                "批次完成（%s）。使用者 %d 位，快照 %d 筆，風險結果 %d 筆，報告 %d 筆。",
                triggerDescription, processedUserCount, snapshotCount, riskResultCount, reportCount
            ));
            schedulerJobLogRepository.save(jobLog);

            return new BatchExecutionResult(
                jobLog.getId(),
                jobLog.getJobName(),
                effectiveReportDate,
                jobLog.getStatus().name(),
                processedUserCount,
                snapshotCount,
                riskResultCount,
                reportCount,
                latestReportId,
                jobLog.getMessage()
            );
        } catch (Exception e) {
            jobLog.setStatus(SchedulerJobStatusCode.FAILED);
            jobLog.setFinishedAt(LocalDateTime.now());
            jobLog.setMessage("批次失敗：" + e.getMessage());
            schedulerJobLogRepository.save(jobLog);
            throw e;
        }
    }

    private UserReportGenerationResult generateForUser(String userId, LocalDate reportDate) {
        List<Portfolio> portfolios = portfolioRepository.findByUserIdAndIsActiveTrue(userId);

        BigDecimal totalCost = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalMarketValue = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalPnL = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        LocalDate dataAsOfTradeDate = null;

        int snapshotCount = 0;
        int riskResultCount = 0;

        Map<String, Long> riskLevelCount = initRiskLevelCount();
        Map<String, Long> recommendationCount = initRecommendationCount();
        List<DailyReportTopRiskHoldingDto> topRiskHoldings = new ArrayList<>();

        for (Portfolio portfolio : portfolios) {
            StockPriceDaily latestPrice = stockPriceDailyRepository.findTopByStockIdOrderByTradeDateDesc(portfolio.getStock().getId())
                .orElse(null);
            if (latestPrice == null) {
                continue;
            }

            LocalDate tradeDate = latestPrice.getTradeDate();
            if (dataAsOfTradeDate == null || tradeDate.isAfter(dataAsOfTradeDate)) {
                dataAsOfTradeDate = tradeDate;
            }

            BigDecimal closePrice = latestPrice.getClosePrice().setScale(PRICE_SCALE, RoundingMode.HALF_UP);
            String assetCurrency = investFxConversionService.resolveAssetCurrencyByMarket(portfolio.getStock().getMarket());
            BigDecimal marketValueInAssetCurrency = closePrice.multiply(portfolio.getQuantity()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal marketValue = investFxConversionService.convertToBaseCurrency(marketValueInAssetCurrency, assetCurrency);
            BigDecimal totalCostInBase = investFxConversionService.convertToBaseCurrency(portfolio.getTotalCost(), assetCurrency);
            BigDecimal pnl = marketValue.subtract(totalCostInBase).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal pnlPercent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (totalCostInBase.compareTo(BigDecimal.ZERO) > 0) {
                pnlPercent = pnl.divide(totalCostInBase, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            }

            PortfolioDailySnapshot snapshot = portfolioDailySnapshotRepository
                .findByPortfolioIdAndTradeDate(portfolio.getId(), tradeDate)
                .orElseGet(PortfolioDailySnapshot::new);
            snapshot.setPortfolio(portfolio);
            snapshot.setTradeDate(tradeDate);
            snapshot.setClosePrice(closePrice);
            snapshot.setMarketValue(marketValue);
            snapshot.setUnrealizedProfitLoss(pnl);
            snapshot.setUnrealizedProfitLossPercent(pnlPercent);
            portfolioDailySnapshotRepository.save(snapshot);
            snapshotCount += 1;

            PortfolioRiskResultDetailDto riskResult = portfolioRiskResultService.recalculateForBatch(portfolio.getId(), tradeDate);
            riskResultCount += 1;

            String riskLevel = normalizeCode(riskResult.getRiskLevel());
            String recommendation = normalizeCode(riskResult.getRecommendation());
            riskLevelCount.put(riskLevel, riskLevelCount.getOrDefault(riskLevel, 0L) + 1);
            recommendationCount.put(recommendation, recommendationCount.getOrDefault(recommendation, 0L) + 1);

            DailyReportTopRiskHoldingDto holding = new DailyReportTopRiskHoldingDto();
            holding.setPortfolioId(portfolio.getId());
            holding.setTicker(portfolio.getStock().getTicker());
            holding.setStockName(portfolio.getStock().getName());
            holding.setRiskScore(riskResult.getRiskScore());
            holding.setRiskLevel(riskResult.getRiskLevel());
            holding.setRecommendation(riskResult.getRecommendation());
            holding.setSummary(riskResult.getSummary());
            holding.setReasons(
                Optional.ofNullable(riskResult.getReasons()).orElse(List.of())
                    .stream()
                    .map(r -> r.getReasonTitle())
                    .limit(3)
                    .toList()
            );
            topRiskHoldings.add(holding);

            totalCost = totalCost.add(totalCostInBase).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            totalMarketValue = totalMarketValue.add(marketValue).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            totalPnL = totalPnL.add(pnl).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        topRiskHoldings = topRiskHoldings.stream()
            .sorted(Comparator.comparing(DailyReportTopRiskHoldingDto::getRiskScore, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(5)
            .toList();

        BigDecimal totalPnLPercent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            totalPnLPercent = totalPnL.divide(totalCost, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        }

        DailyReportSummaryDto summary = new DailyReportSummaryDto();
        summary.setReportDate(reportDate);
        summary.setReportType(DailyReportTypeCode.PORTFOLIO_RISK_DAILY.name());
        summary.setStatus(DailyReportStatusCode.SUCCESS.name());
        summary.setDataAsOfTradeDate(dataAsOfTradeDate);
        summary.setHoldingCount((long) portfolios.size());
        summary.setSnapshotCount((long) snapshotCount);
        summary.setTotalCost(totalCost);
        summary.setTotalMarketValue(totalMarketValue);
        summary.setTotalPnL(totalPnL);
        summary.setTotalPnLPercent(totalPnLPercent);
        summary.setHighRiskCount(riskLevelCount.getOrDefault(RiskLevelCode.HIGH.name(), 0L));
        summary.setCriticalRiskCount(riskLevelCount.getOrDefault(RiskLevelCode.CRITICAL.name(), 0L));
        summary.setRiskDistribution(toDistribution(riskLevelCount, List.of("LOW", "MEDIUM", "HIGH", "CRITICAL")));
        summary.setRecommendationDistribution(
            toDistribution(recommendationCount, List.of("HOLD", "WATCH", "REDUCE", "STOP_LOSS_CHECK"))
        );
        summary.setTopRiskHoldings(topRiskHoldings);
        summary.setNoviceReminders(NOVICE_REMINDERS);
        summary.setDisclaimer(DISCLAIMER);

        String summaryJson = toSummaryJson(summary);

        DailyReport report = dailyReportRepository
            .findByUserIdAndReportDateAndReportType(userId, reportDate, DailyReportTypeCode.PORTFOLIO_RISK_DAILY)
            .orElseGet(DailyReport::new);
        report.setUserId(userId);
        report.setReportDate(reportDate);
        report.setReportType(DailyReportTypeCode.PORTFOLIO_RISK_DAILY);
        report.setSummaryJson(summaryJson);
        report.setStatus(DailyReportStatusCode.SUCCESS);
        DailyReport saved = dailyReportRepository.save(report);

        return new UserReportGenerationResult(saved.getId(), snapshotCount, riskResultCount);
    }

    private String toSummaryJson(DailyReportSummaryDto summary) {
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化每日報告摘要失敗：" + e.getMessage(), e);
        }
    }

    private Map<String, Long> initRiskLevelCount() {
        Map<String, Long> map = new LinkedHashMap<>();
        map.put("LOW", 0L);
        map.put("MEDIUM", 0L);
        map.put("HIGH", 0L);
        map.put("CRITICAL", 0L);
        return map;
    }

    private Map<String, Long> initRecommendationCount() {
        Map<String, Long> map = new LinkedHashMap<>();
        map.put("HOLD", 0L);
        map.put("WATCH", 0L);
        map.put("REDUCE", 0L);
        map.put("STOP_LOSS_CHECK", 0L);
        return map;
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    private List<DashboardDistributionItemDto> toDistribution(Map<String, Long> countMap, List<String> orderedKeys) {
        return orderedKeys.stream()
            .map(key -> new DashboardDistributionItemDto(key, countMap.getOrDefault(key, 0L)))
            .toList();
    }

    public record BatchExecutionResult(
        Long jobLogId,
        String jobName,
        LocalDate runDate,
        String status,
        Integer processedUserCount,
        Integer snapshotCount,
        Integer riskResultCount,
        Integer reportCount,
        Long reportId,
        String message
    ) {}

    private record UserReportGenerationResult(Long reportId, Integer snapshotCount, Integer riskResultCount) {}
}
