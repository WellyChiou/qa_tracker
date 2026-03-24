package com.example.helloworld.service.invest;

import com.example.helloworld.dto.invest.RunPriceUpdateResponseDto;
import com.example.helloworld.entity.invest.*;
import com.example.helloworld.repository.invest.PortfolioRepository;
import com.example.helloworld.repository.invest.PriceUpdateJobDetailRepository;
import com.example.helloworld.repository.invest.PriceUpdateJobLogRepository;
import com.example.helloworld.repository.invest.StockPriceDailyRepository;
import com.example.helloworld.service.invest.auth.InvestCurrentUserService;
import com.example.helloworld.service.invest.price.MarketPriceProvider;
import com.example.helloworld.service.invest.price.PriceQuoteSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class PriceUpdateService {

    private static final String JOB_NAME = "PRICE_UPDATE_HOLDINGS";
    private static final int PRICE_SCALE = 4;
    private static final int PERCENT_SCALE = 4;
    private static final ZoneId TAIPEI_ZONE = ZoneId.of("Asia/Taipei");
    private static final ZoneId NEW_YORK_ZONE = ZoneId.of("America/New_York");

    private final PortfolioRepository portfolioRepository;
    private final StockPriceDailyRepository stockPriceDailyRepository;
    private final PriceUpdateJobLogRepository priceUpdateJobLogRepository;
    private final PriceUpdateJobDetailRepository priceUpdateJobDetailRepository;
    private final InvestCurrentUserService investCurrentUserService;
    private final List<MarketPriceProvider> marketPriceProviders;

    @Value("${invest.price-update.retry.max-attempts:2}")
    private int maxRetryAttempts;

    @Value("${invest.price-update.retry.delay-ms:300}")
    private long retryDelayMs;

    @Value("${invest.price-update.data-quality.stale-calendar-days:3}")
    private long staleCalendarDays;

    public PriceUpdateService(PortfolioRepository portfolioRepository,
                              StockPriceDailyRepository stockPriceDailyRepository,
                              PriceUpdateJobLogRepository priceUpdateJobLogRepository,
                              PriceUpdateJobDetailRepository priceUpdateJobDetailRepository,
                              InvestCurrentUserService investCurrentUserService,
                              List<MarketPriceProvider> marketPriceProviders) {
        this.portfolioRepository = portfolioRepository;
        this.stockPriceDailyRepository = stockPriceDailyRepository;
        this.priceUpdateJobLogRepository = priceUpdateJobLogRepository;
        this.priceUpdateJobDetailRepository = priceUpdateJobDetailRepository;
        this.investCurrentUserService = investCurrentUserService;
        this.marketPriceProviders = marketPriceProviders;
    }

    public RunPriceUpdateResponseDto runForCurrentUser() {
        String userUid = investCurrentUserService.resolveCurrentUserUid();
        PriceUpdateJobLog jobLog = executeForUser(
            userUid,
            Set.of(),
            PriceUpdateRunModeCode.MANUAL,
            "手動觸發（當前登入者）"
        );
        return toResponse(jobLog);
    }

    public SchedulerExecutionResult runForAllActiveUsers(Set<String> markets,
                                                         PriceUpdateRunModeCode runMode,
                                                         String triggerDescription) {
        Set<String> normalizedMarkets = normalizeMarkets(markets);
        List<String> userIds = portfolioRepository.findDistinctActiveUserIds();

        int jobCount = 0;
        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;

        for (String userId : userIds) {
            PriceUpdateJobLog jobLog = executeForUser(userId, normalizedMarkets, runMode, triggerDescription);
            jobCount += 1;
            totalCount += safe(jobLog.getTotalCount());
            successCount += safe(jobLog.getSuccessCount());
            failCount += safe(jobLog.getFailCount());
        }

        return new SchedulerExecutionResult(
            userIds.size(),
            jobCount,
            totalCount,
            successCount,
            failCount
        );
    }

    private PriceUpdateJobLog executeForUser(String userUid,
                                             Set<String> normalizedMarkets,
                                             PriceUpdateRunModeCode runMode,
                                             String triggerDescription) {
        LocalDateTime startedAt = LocalDateTime.now(TAIPEI_ZONE);
        String batchId = "BATCH-" + UUID.randomUUID().toString().replace("-", "");

        PriceUpdateJobLog jobLog = new PriceUpdateJobLog();
        jobLog.setJobName(JOB_NAME);
        jobLog.setBatchId(batchId);
        jobLog.setUserId(userUid);
        jobLog.setRunMode(runMode);
        jobLog.setStatus(PriceUpdateJobStatusCode.RUNNING);
        jobLog.setStartedAt(startedAt);
        jobLog.setTotalCount(0);
        jobLog.setSuccessCount(0);
        jobLog.setFailCount(0);
        jobLog.setMessage("持股行情更新執行中");
        jobLog = priceUpdateJobLogRepository.save(jobLog);

        int successCount = 0;
        int failCount = 0;
        int retriedStockCount = 0;
        int retryRecoveredCount = 0;
        int retryFailedCount = 0;

        try {
            List<Portfolio> activePortfolios = portfolioRepository.findByUserIdAndIsActiveTrue(userUid);
            Map<Long, Stock> uniqueStocks = collectUniqueStocks(activePortfolios, normalizedMarkets);
            int totalCount = uniqueStocks.size();
            jobLog.setTotalCount(totalCount);

            if (totalCount == 0) {
                jobLog.setStatus(PriceUpdateJobStatusCode.SUCCESS);
                jobLog.setMessage("目前沒有可更新的持股股票");
                jobLog.setFinishedAt(LocalDateTime.now(TAIPEI_ZONE));
                return priceUpdateJobLogRepository.save(jobLog);
            }

            for (Stock stock : uniqueStocks.values()) {
                PriceUpdateJobDetail detail = new PriceUpdateJobDetail();
                detail.setJobLog(jobLog);
                detail.setStock(stock);
                detail.setTicker(stock.getTicker());

                StockUpdateAttemptResult attemptResult = fetchWithRetry(stock);
                if (attemptResult.attemptCount() > 1) {
                    retriedStockCount += 1;
                }

                if (attemptResult.success()) {
                    upsertStockPriceDaily(stock, attemptResult.quote(), batchId);
                    detail.setStatus(PriceUpdateDetailStatusCode.SUCCESS);
                    detail.setTradeDate(attemptResult.quote().getTradeDate());
                    detail.setFetchedAt(attemptResult.quote().getFetchedAt());
                    detail.setReason(attemptResult.reason());
                    successCount += 1;
                    if (attemptResult.attemptCount() > 1) {
                        retryRecoveredCount += 1;
                    }
                } else {
                    detail.setStatus(PriceUpdateDetailStatusCode.FAILED);
                    detail.setReason(attemptResult.reason());
                    detail.setTradeDate(null);
                    detail.setFetchedAt(LocalDateTime.now(TAIPEI_ZONE));
                    failCount += 1;
                    if (attemptResult.attemptCount() > 1) {
                        retryFailedCount += 1;
                    }
                }

                priceUpdateJobDetailRepository.save(detail);
            }

            jobLog.setSuccessCount(successCount);
            jobLog.setFailCount(failCount);
            jobLog.setStatus(resolveFinalStatus(successCount, failCount));
            jobLog.setMessage(buildFinalMessage(
                successCount,
                failCount,
                triggerDescription,
                retriedStockCount,
                retryRecoveredCount,
                retryFailedCount
            ));
            jobLog.setFinishedAt(LocalDateTime.now(TAIPEI_ZONE));
            return priceUpdateJobLogRepository.save(jobLog);
        } catch (Exception e) {
            jobLog.setSuccessCount(successCount);
            jobLog.setFailCount(failCount);
            jobLog.setStatus(PriceUpdateJobStatusCode.FAILED);
            jobLog.setMessage("執行失敗：" + truncate(e.getMessage(), 900));
            jobLog.setFinishedAt(LocalDateTime.now(TAIPEI_ZONE));
            return priceUpdateJobLogRepository.save(jobLog);
        }
    }

    private Map<Long, Stock> collectUniqueStocks(List<Portfolio> portfolios, Set<String> normalizedMarkets) {
        Map<Long, Stock> uniqueStocks = new LinkedHashMap<>();
        for (Portfolio portfolio : portfolios) {
            if (portfolio.getStock() == null || portfolio.getStock().getId() == null) {
                continue;
            }
            String market = normalizeMarket(portfolio.getStock().getMarket());
            if (!normalizedMarkets.isEmpty() && !normalizedMarkets.contains(market)) {
                continue;
            }
            uniqueStocks.put(portfolio.getStock().getId(), portfolio.getStock());
        }
        return uniqueStocks;
    }

    private StockUpdateAttemptResult fetchWithRetry(Stock stock) {
        int attempts = Math.max(1, maxRetryAttempts);
        List<String> errors = new ArrayList<>();

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                Optional<MarketPriceProvider> providerOptional = resolveProvider(stock.getMarket());
                if (providerOptional.isEmpty()) {
                    throw new RuntimeException("不支援的 market: " + stock.getMarket());
                }

                Optional<PriceQuoteSnapshot> quoteOptional = providerOptional.get().fetchLatestQuote(stock);
                if (quoteOptional.isEmpty()) {
                    throw new RuntimeException("查無可用行情");
                }

                String reason = buildRetrySuccessReason(errors, attempt);
                return StockUpdateAttemptResult.success(quoteOptional.get(), attempt, reason);
            } catch (Exception ex) {
                errors.add("第" + attempt + "次失敗：" + truncate(ex.getMessage(), 140));
                if (attempt < attempts && retryDelayMs > 0) {
                    sleepQuietly(retryDelayMs);
                }
            }
        }

        return StockUpdateAttemptResult.failed(
            attempts,
            "重試 " + attempts + " 次仍失敗：" + String.join("；", errors)
        );
    }

    private Optional<MarketPriceProvider> resolveProvider(String market) {
        if (market == null) {
            return Optional.empty();
        }
        return marketPriceProviders.stream()
            .filter(provider -> provider.supportsMarket(market))
            .findFirst();
    }

    private void upsertStockPriceDaily(Stock stock, PriceQuoteSnapshot quote, String batchId) {
        StockPriceDaily entity = stockPriceDailyRepository.findByStockIdAndTradeDate(stock.getId(), quote.getTradeDate())
            .orElseGet(StockPriceDaily::new);

        BigDecimal closePrice = scalePrice(quote.getClosePrice());
        BigDecimal openPrice = scalePrice(orDefault(quote.getOpenPrice(), closePrice));
        BigDecimal highPrice = scalePrice(orDefault(quote.getHighPrice(), closePrice));
        BigDecimal lowPrice = scalePrice(orDefault(quote.getLowPrice(), closePrice));

        entity.setStock(stock);
        entity.setTradeDate(quote.getTradeDate());
        entity.setOpenPrice(openPrice);
        entity.setHighPrice(highPrice);
        entity.setLowPrice(lowPrice);
        entity.setClosePrice(closePrice);
        entity.setVolume(quote.getVolume() == null ? 0L : quote.getVolume());
        entity.setChangeAmount(scaleNullablePrice(quote.getChangeAmount()));
        entity.setChangePercent(scaleNullablePercent(quote.getChangePercent()));
        // dataSource 是行情來源（TWSE/TPEX/US_PROVIDER），非持股估值語意。
        entity.setDataSource(quote.getDataSource());
        entity.setFetchedAt(quote.getFetchedAt() == null ? LocalDateTime.now(TAIPEI_ZONE) : quote.getFetchedAt());
        entity.setLatencyType(quote.getLatencyType());
        // updateBatchId 與 price_update_job_log.batch_id 必須同批次一致。
        entity.setUpdateBatchId(batchId);
        entity.setDataQuality(resolveDataQuality(stock, quote));

        stockPriceDailyRepository.save(entity);
    }

    private String resolveDataQuality(Stock stock, PriceQuoteSnapshot quote) {
        if (quote.getTradeDate() == null) {
            return "MISSING";
        }
        LocalDate referenceDate = LocalDate.now(resolveZoneByMarket(stock.getMarket()));
        long staleDays = ChronoUnit.DAYS.between(quote.getTradeDate(), referenceDate);
        if (staleDays > staleCalendarDays) {
            return "STALE";
        }
        if (quote.getChangeAmount() == null || quote.getChangePercent() == null) {
            return "PARTIAL";
        }
        return "GOOD";
    }

    private ZoneId resolveZoneByMarket(String market) {
        if ("US".equalsIgnoreCase(market)) {
            return NEW_YORK_ZONE;
        }
        return TAIPEI_ZONE;
    }

    private Set<String> normalizeMarkets(Set<String> markets) {
        if (markets == null || markets.isEmpty()) {
            return Set.of();
        }
        return markets.stream()
            .filter(Objects::nonNull)
            .map(this::normalizeMarket)
            .filter(value -> !value.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeMarket(String market) {
        return market == null ? "" : market.trim().toUpperCase(Locale.ROOT);
    }

    private String buildRetrySuccessReason(List<String> errors, int attempt) {
        if (attempt <= 1 || errors.isEmpty()) {
            return null;
        }
        return "重試後成功（第 " + attempt + " 次）：" + String.join("；", errors);
    }

    private String buildFinalMessage(int successCount,
                                     int failCount,
                                     String triggerDescription,
                                     int retriedStockCount,
                                     int retryRecoveredCount,
                                     int retryFailedCount) {
        String baseMessage;
        if (failCount > 0 && successCount > 0) {
            baseMessage = "部分股票更新失敗，請查看原因";
        } else if (failCount > 0) {
            baseMessage = "持股行情更新失敗";
        } else {
            baseMessage = "持股行情更新完成";
        }

        String retrySummary = String.format(
            "重試觸發 %d 檔，重試後成功 %d 檔，重試後仍失敗 %d 檔",
            retriedStockCount, retryRecoveredCount, retryFailedCount
        );
        return baseMessage + "（" + triggerDescription + "）。" + retrySummary + "。";
    }

    private PriceUpdateJobStatusCode resolveFinalStatus(int successCount, int failCount) {
        if (failCount == 0) {
            return PriceUpdateJobStatusCode.SUCCESS;
        }
        if (successCount > 0) {
            return PriceUpdateJobStatusCode.PARTIAL_FAILED;
        }
        return PriceUpdateJobStatusCode.FAILED;
    }

    private RunPriceUpdateResponseDto toResponse(PriceUpdateJobLog jobLog) {
        RunPriceUpdateResponseDto dto = new RunPriceUpdateResponseDto();
        dto.setJobLogId(jobLog.getId());
        dto.setJobName(jobLog.getJobName());
        dto.setBatchId(jobLog.getBatchId());
        dto.setRunMode(jobLog.getRunMode() == null ? null : jobLog.getRunMode().name());
        dto.setStatus(jobLog.getStatus() == null ? null : jobLog.getStatus().name());
        dto.setTotalCount(jobLog.getTotalCount());
        dto.setSuccessCount(jobLog.getSuccessCount());
        dto.setFailCount(jobLog.getFailCount());
        dto.setStartedAt(jobLog.getStartedAt());
        dto.setFinishedAt(jobLog.getFinishedAt());
        dto.setMessage(jobLog.getMessage());
        return dto;
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal orDefault(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private BigDecimal scalePrice(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
        }
        return value.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleNullablePrice(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleNullablePercent(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public record SchedulerExecutionResult(
        int processedUserCount,
        int jobCount,
        int totalCount,
        int successCount,
        int failCount
    ) {}

    private record StockUpdateAttemptResult(
        boolean success,
        PriceQuoteSnapshot quote,
        int attemptCount,
        String reason
    ) {
        private static StockUpdateAttemptResult success(PriceQuoteSnapshot quote, int attemptCount, String reason) {
            return new StockUpdateAttemptResult(true, quote, attemptCount, reason);
        }

        private static StockUpdateAttemptResult failed(int attemptCount, String reason) {
            return new StockUpdateAttemptResult(false, null, attemptCount, reason);
        }
    }
}
