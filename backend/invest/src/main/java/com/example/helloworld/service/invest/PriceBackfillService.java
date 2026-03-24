package com.example.helloworld.service.invest;

import com.example.helloworld.dto.invest.RunPriceBackfillResponseDto;
import com.example.helloworld.entity.invest.*;
import com.example.helloworld.repository.invest.*;
import com.example.helloworld.service.invest.auth.InvestCurrentUserService;
import com.example.helloworld.service.invest.price.MarketPriceProvider;
import com.example.helloworld.service.invest.price.PriceQuoteSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class PriceBackfillService {

    public static final String JOB_NAME = "PRICE_BACKFILL";
    private static final int MIN_DAYS = 5;
    private static final int MAX_DAYS = 120;
    private static final int PRICE_SCALE = 4;
    private static final int PERCENT_SCALE = 4;
    private static final ZoneId TAIPEI_ZONE = ZoneId.of("Asia/Taipei");

    private final PortfolioRepository portfolioRepository;
    private final WatchlistService watchlistService;
    private final WatchlistItemRepository watchlistItemRepository;
    private final StockPriceDailyRepository stockPriceDailyRepository;
    private final PriceUpdateJobLogRepository priceUpdateJobLogRepository;
    private final PriceUpdateJobDetailRepository priceUpdateJobDetailRepository;
    private final InvestCurrentUserService investCurrentUserService;
    private final List<MarketPriceProvider> marketPriceProviders;

    public PriceBackfillService(PortfolioRepository portfolioRepository,
                                WatchlistService watchlistService,
                                WatchlistItemRepository watchlistItemRepository,
                                StockPriceDailyRepository stockPriceDailyRepository,
                                PriceUpdateJobLogRepository priceUpdateJobLogRepository,
                                PriceUpdateJobDetailRepository priceUpdateJobDetailRepository,
                                InvestCurrentUserService investCurrentUserService,
                                List<MarketPriceProvider> marketPriceProviders) {
        this.portfolioRepository = portfolioRepository;
        this.watchlistService = watchlistService;
        this.watchlistItemRepository = watchlistItemRepository;
        this.stockPriceDailyRepository = stockPriceDailyRepository;
        this.priceUpdateJobLogRepository = priceUpdateJobLogRepository;
        this.priceUpdateJobDetailRepository = priceUpdateJobDetailRepository;
        this.investCurrentUserService = investCurrentUserService;
        this.marketPriceProviders = marketPriceProviders;
    }

    public RunPriceBackfillResponseDto runForCurrentUser(Integer days, String scopeValue) {
        int safeDays = normalizeDays(days);
        MarketAnalysisScopeCode scope = MarketAnalysisScopeCode.fromNullable(scopeValue);
        String userUid = investCurrentUserService.resolveCurrentUserUid();

        LocalDateTime startedAt = LocalDateTime.now(TAIPEI_ZONE);
        String batchId = "BATCH-" + UUID.randomUUID().toString().replace("-", "");

        PriceUpdateJobLog jobLog = new PriceUpdateJobLog();
        jobLog.setJobName(JOB_NAME);
        jobLog.setBatchId(batchId);
        jobLog.setUserId(userUid);
        jobLog.setRunMode(PriceUpdateRunModeCode.MANUAL_BACKFILL);
        jobLog.setStatus(PriceUpdateJobStatusCode.RUNNING);
        jobLog.setStartedAt(startedAt);
        jobLog.setTotalCount(0);
        jobLog.setSuccessCount(0);
        jobLog.setFailCount(0);
        jobLog.setMessage("歷史行情回補執行中");
        jobLog = priceUpdateJobLogRepository.save(jobLog);

        int successCount = 0;
        int failCount = 0;
        int upsertedRowCount = 0;

        try {
            Map<Long, Stock> targetStocks = resolveTargetStocks(userUid, scope);
            jobLog.setTotalCount(targetStocks.size());

            if (targetStocks.isEmpty()) {
                jobLog.setStatus(PriceUpdateJobStatusCode.SUCCESS);
                jobLog.setMessage("目前沒有可回補的股票");
                jobLog.setFinishedAt(LocalDateTime.now(TAIPEI_ZONE));
                jobLog = priceUpdateJobLogRepository.save(jobLog);
                return toResponse(jobLog, scope, safeDays, upsertedRowCount);
            }

            for (Stock stock : targetStocks.values()) {
                PriceUpdateJobDetail detail = new PriceUpdateJobDetail();
                detail.setJobLog(jobLog);
                detail.setStock(stock);
                detail.setTicker(stock.getTicker());

                try {
                    Optional<MarketPriceProvider> provider = resolveProvider(stock.getMarket());
                    if (provider.isEmpty()) {
                        throw new RuntimeException("不支援的 market: " + stock.getMarket());
                    }

                    List<PriceQuoteSnapshot> historyRows = deduplicateAndLimit(
                        provider.get().fetchHistoricalQuotes(stock, safeDays),
                        safeDays
                    );
                    if (historyRows.isEmpty()) {
                        throw new RuntimeException("查無歷史行情資料");
                    }

                    int affectedRows = upsertHistoryRows(stock, historyRows, batchId);
                    upsertedRowCount += affectedRows;

                    detail.setStatus(PriceUpdateDetailStatusCode.SUCCESS);
                    detail.setTradeDate(historyRows.get(0).getTradeDate());
                    detail.setFetchedAt(LocalDateTime.now(TAIPEI_ZONE));
                    detail.setReason(buildSuccessReason(historyRows, affectedRows));
                    successCount += 1;
                } catch (Exception ex) {
                    detail.setStatus(PriceUpdateDetailStatusCode.FAILED);
                    detail.setTradeDate(null);
                    detail.setFetchedAt(LocalDateTime.now(TAIPEI_ZONE));
                    detail.setReason("回補失敗：" + truncate(ex.getMessage(), 420));
                    failCount += 1;
                }

                priceUpdateJobDetailRepository.save(detail);
            }

            jobLog.setSuccessCount(successCount);
            jobLog.setFailCount(failCount);
            jobLog.setStatus(resolveFinalStatus(successCount, failCount));
            jobLog.setMessage(buildFinalMessage(scope, safeDays, targetStocks.size(), successCount, failCount, upsertedRowCount));
            jobLog.setFinishedAt(LocalDateTime.now(TAIPEI_ZONE));
            jobLog = priceUpdateJobLogRepository.save(jobLog);
            return toResponse(jobLog, scope, safeDays, upsertedRowCount);
        } catch (Exception e) {
            jobLog.setSuccessCount(successCount);
            jobLog.setFailCount(failCount);
            jobLog.setStatus(PriceUpdateJobStatusCode.FAILED);
            jobLog.setMessage("執行失敗：" + truncate(e.getMessage(), 900));
            jobLog.setFinishedAt(LocalDateTime.now(TAIPEI_ZONE));
            jobLog = priceUpdateJobLogRepository.save(jobLog);
            return toResponse(jobLog, scope, safeDays, upsertedRowCount);
        }
    }

    private Map<Long, Stock> resolveTargetStocks(String userUid, MarketAnalysisScopeCode scope) {
        Map<Long, Stock> targetStocks = new LinkedHashMap<>();
        List<Portfolio> portfolios = portfolioRepository.findByUserIdAndIsActiveTrue(userUid);
        for (Portfolio portfolio : portfolios) {
            putStockIfValid(targetStocks, portfolio.getStock());
        }

        if (scope == MarketAnalysisScopeCode.HOLDINGS_AND_WATCHLIST) {
            Watchlist defaultWatchlist = watchlistService.getOrCreateDefaultWatchlist(userUid);
            List<WatchlistItem> items = watchlistItemRepository.findActiveByWatchlistId(defaultWatchlist.getId());
            for (WatchlistItem item : items) {
                putStockIfValid(targetStocks, item.getStock());
            }
        }
        return targetStocks;
    }

    private void putStockIfValid(Map<Long, Stock> map, Stock stock) {
        if (stock == null || stock.getId() == null) {
            return;
        }
        if (Boolean.FALSE.equals(stock.getIsActive())) {
            return;
        }
        map.putIfAbsent(stock.getId(), stock);
    }

    private Optional<MarketPriceProvider> resolveProvider(String market) {
        if (market == null) {
            return Optional.empty();
        }
        return marketPriceProviders.stream()
            .filter(provider -> provider.supportsMarket(market))
            .findFirst();
    }

    private List<PriceQuoteSnapshot> deduplicateAndLimit(List<PriceQuoteSnapshot> rows, int days) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Map<LocalDate, PriceQuoteSnapshot> map = new HashMap<>();
        for (PriceQuoteSnapshot row : rows) {
            if (row == null || row.getTradeDate() == null) {
                continue;
            }
            map.putIfAbsent(row.getTradeDate(), row);
        }

        return map.values().stream()
            .sorted(Comparator.comparing(PriceQuoteSnapshot::getTradeDate).reversed())
            .limit(days)
            .toList();
    }

    private int upsertHistoryRows(Stock stock, List<PriceQuoteSnapshot> rows, String batchId) {
        int affectedRows = 0;
        for (PriceQuoteSnapshot quote : rows) {
            if (quote.getTradeDate() == null || quote.getClosePrice() == null) {
                continue;
            }

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
            entity.setDataSource(quote.getDataSource());
            entity.setFetchedAt(quote.getFetchedAt() == null ? LocalDateTime.now(TAIPEI_ZONE) : quote.getFetchedAt());
            entity.setLatencyType(quote.getLatencyType());
            entity.setUpdateBatchId(batchId);
            entity.setDataQuality(resolveHistoryDataQuality(quote));

            stockPriceDailyRepository.save(entity);
            affectedRows += 1;
        }
        return affectedRows;
    }

    private int normalizeDays(Integer days) {
        int value = days == null ? 30 : days;
        if (value < MIN_DAYS || value > MAX_DAYS) {
            throw new RuntimeException("days 無效，請輸入 " + MIN_DAYS + "~" + MAX_DAYS);
        }
        return value;
    }

    private String resolveHistoryDataQuality(PriceQuoteSnapshot quote) {
        if (quote.getClosePrice() == null) {
            return "INSUFFICIENT";
        }
        if (quote.getVolume() == null || quote.getVolume() <= 0) {
            return "PARTIAL";
        }
        return "GOOD";
    }

    private String buildSuccessReason(List<PriceQuoteSnapshot> rows, int affectedRows) {
        LocalDate latest = rows.get(0).getTradeDate();
        LocalDate oldest = rows.get(rows.size() - 1).getTradeDate();
        return String.format(
            "回補 %d 筆（upsert %d 筆），區間 %s ~ %s",
            rows.size(),
            affectedRows,
            oldest,
            latest
        );
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

    private String buildFinalMessage(MarketAnalysisScopeCode scope,
                                     int days,
                                     int totalCount,
                                     int successCount,
                                     int failCount,
                                     int upsertedRows) {
        return String.format(
            "歷史行情回補完成（scope=%s, days=%d, target=%d, success=%d, fail=%d, upsertRows=%d）",
            scope.name(),
            days,
            totalCount,
            successCount,
            failCount,
            upsertedRows
        );
    }

    private RunPriceBackfillResponseDto toResponse(PriceUpdateJobLog jobLog,
                                                   MarketAnalysisScopeCode scope,
                                                   int days,
                                                   int upsertedRowCount) {
        RunPriceBackfillResponseDto dto = new RunPriceBackfillResponseDto();
        dto.setJobLogId(jobLog.getId());
        dto.setJobName(jobLog.getJobName());
        dto.setBatchId(jobLog.getBatchId());
        dto.setRunMode(jobLog.getRunMode() == null ? null : jobLog.getRunMode().name());
        dto.setStatus(jobLog.getStatus() == null ? null : jobLog.getStatus().name());
        dto.setScope(scope.name());
        dto.setDays(days);
        dto.setTargetStockCount(jobLog.getTotalCount());
        dto.setSuccessCount(jobLog.getSuccessCount());
        dto.setFailCount(jobLog.getFailCount());
        dto.setUpsertedRowCount(upsertedRowCount);
        dto.setStartedAt(jobLog.getStartedAt());
        dto.setFinishedAt(jobLog.getFinishedAt());
        dto.setMessage(jobLog.getMessage());
        return dto;
    }

    private BigDecimal scalePrice(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleNullablePrice(BigDecimal value) {
        return value == null ? null : value.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleNullablePercent(BigDecimal value) {
        return value == null ? null : value.setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal orDefault(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
