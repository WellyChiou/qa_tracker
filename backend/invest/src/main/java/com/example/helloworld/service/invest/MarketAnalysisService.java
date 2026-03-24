package com.example.helloworld.service.invest;

import com.example.helloworld.dto.invest.OpportunitySignalReasonDto;
import com.example.helloworld.dto.invest.RunMarketAnalysisResponseDto;
import com.example.helloworld.entity.invest.*;
import com.example.helloworld.repository.invest.*;
import com.example.helloworld.service.invest.auth.InvestCurrentUserService;
import com.example.helloworld.service.invest.system.InvestStrategySettingService;
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
public class MarketAnalysisService {

    private static final int SCORE_CAP = 100;
    private static final String DISCLAIMER = "本模組為觀察輔助工具，不保證獲利，請勿把單一訊號視為買賣保證。";
    private static final ZoneId TAIPEI_ZONE = ZoneId.of("Asia/Taipei");

    private static final String QUALITY_GOOD = "GOOD";
    private static final String QUALITY_PARTIAL = "PARTIAL";
    private static final String QUALITY_STALE = "STALE";
    private static final String QUALITY_INSUFFICIENT = "INSUFFICIENT";

    private final PortfolioRepository portfolioRepository;
    private final StockPriceDailyRepository stockPriceDailyRepository;
    private final StrengthSnapshotRepository strengthSnapshotRepository;
    private final StrengthSnapshotFactorRepository strengthSnapshotFactorRepository;
    private final OpportunitySignalRepository opportunitySignalRepository;
    private final OpportunitySignalReasonRepository opportunitySignalReasonRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final InvestCurrentUserService investCurrentUserService;
    private final WatchlistService watchlistService;
    private final InvestStrategySettingService investStrategySettingService;

    public MarketAnalysisService(PortfolioRepository portfolioRepository,
                                 StockPriceDailyRepository stockPriceDailyRepository,
                                 StrengthSnapshotRepository strengthSnapshotRepository,
                                 StrengthSnapshotFactorRepository strengthSnapshotFactorRepository,
                                 OpportunitySignalRepository opportunitySignalRepository,
                                 OpportunitySignalReasonRepository opportunitySignalReasonRepository,
                                 WatchlistItemRepository watchlistItemRepository,
                                 InvestCurrentUserService investCurrentUserService,
                                 WatchlistService watchlistService,
                                 InvestStrategySettingService investStrategySettingService) {
        this.portfolioRepository = portfolioRepository;
        this.stockPriceDailyRepository = stockPriceDailyRepository;
        this.strengthSnapshotRepository = strengthSnapshotRepository;
        this.strengthSnapshotFactorRepository = strengthSnapshotFactorRepository;
        this.opportunitySignalRepository = opportunitySignalRepository;
        this.opportunitySignalReasonRepository = opportunitySignalReasonRepository;
        this.watchlistItemRepository = watchlistItemRepository;
        this.investCurrentUserService = investCurrentUserService;
        this.watchlistService = watchlistService;
        this.investStrategySettingService = investStrategySettingService;
    }

    public RunMarketAnalysisResponseDto runForCurrentUser(String scopeValue) {
        String userUid = investCurrentUserService.resolveCurrentUserUid();
        MarketAnalysisScopeCode scope = MarketAnalysisScopeCode.fromNullable(scopeValue);

        LocalDateTime startedAt = LocalDateTime.now(TAIPEI_ZONE);
        List<String> failures = new ArrayList<>();
        InvestStrategySettingService.ResolvedThresholds thresholds = investStrategySettingService.getResolvedThresholds();
        int strategyVersion = thresholds.strategyVersion();

        List<AnalysisTarget> targets = collectTargets(userUid, scope);
        List<StrengthSnapshot> snapshots = new ArrayList<>();
        int analyzedCount = 0;

        for (AnalysisTarget target : targets) {
            try {
                StrengthSnapshot snapshot = analyzeAndUpsertSnapshot(userUid, target, strategyVersion, thresholds);
                snapshots.add(snapshot);
                analyzedCount += 1;
            } catch (Exception e) {
                failures.add(target.stock().getTicker() + ": " + safeMessage(e.getMessage()));
            }
        }

        int activeSignalCount = syncOpportunitySignals(userUid, snapshots, strategyVersion, thresholds);

        RunMarketAnalysisResponseDto dto = new RunMarketAnalysisResponseDto();
        dto.setScope(scope.name());
        dto.setTargetCount(targets.size());
        dto.setAnalyzedCount(analyzedCount);
        dto.setFailCount(failures.size());
        dto.setSnapshotCount(snapshots.size());
        dto.setSignalActiveCount(activeSignalCount);
        dto.setStrategyVersion(strategyVersion);
        dto.setDataAsOfTradeDate(resolveDataAsOfTradeDate(snapshots));
        dto.setStartedAt(startedAt);
        dto.setFinishedAt(LocalDateTime.now(TAIPEI_ZONE));
        dto.setStatus(resolveRunStatus(analyzedCount, failures.size()));
        dto.setMessage(buildRunMessage(analyzedCount, failures));
        return dto;
    }

    private List<AnalysisTarget> collectTargets(String userUid, MarketAnalysisScopeCode scope) {
        Map<String, AnalysisTarget> targetMap = new LinkedHashMap<>();

        List<Portfolio> portfolios = portfolioRepository.findByUserIdAndIsActiveTrue(userUid);
        for (Portfolio portfolio : portfolios) {
            if (portfolio.getStock() == null || portfolio.getStock().getId() == null) {
                continue;
            }
            AnalysisTarget target = new AnalysisTarget(
                portfolio.getStock(),
                MarketUniverseTypeCode.HOLDING,
                null,
                0L
            );
            enforceWatchScopeRule(target);
            targetMap.put(buildTargetKey(target), target);
        }

        if (scope == MarketAnalysisScopeCode.HOLDINGS_AND_WATCHLIST) {
            Watchlist defaultWatchlist = watchlistService.getOrCreateDefaultWatchlist(userUid);
            List<WatchlistItem> watchlistItems = watchlistItemRepository.findActiveByWatchlistId(defaultWatchlist.getId());
            for (WatchlistItem item : watchlistItems) {
                if (item.getStock() == null || item.getStock().getId() == null) {
                    continue;
                }
                AnalysisTarget target = new AnalysisTarget(
                    item.getStock(),
                    MarketUniverseTypeCode.WATCHLIST,
                    defaultWatchlist,
                    defaultWatchlist.getId()
                );
                enforceWatchScopeRule(target);
                targetMap.put(buildTargetKey(target), target);
            }
        }

        return new ArrayList<>(targetMap.values());
    }

    private StrengthSnapshot analyzeAndUpsertSnapshot(String userUid,
                                                      AnalysisTarget target,
                                                      int strategyVersion,
                                                      InvestStrategySettingService.ResolvedThresholds thresholds) {
        StockPriceDaily latest = stockPriceDailyRepository.findTopByStockIdOrderByTradeDateDesc(target.stock().getId())
            .orElseThrow(() -> new RuntimeException("缺少 stock_price_daily，無法分析"));

        List<StockPriceDaily> history = stockPriceDailyRepository
            .findTop30ByStockIdAndTradeDateLessThanEqualOrderByTradeDateDesc(target.stock().getId(), latest.getTradeDate());

        String dataQuality = resolveDataQuality(latest, history, thresholds);
        List<FactorResult> factors = buildFactors(history, dataQuality, thresholds);

        Integer totalScore = null;
        StrengthLevelCode level = null;
        StrengthRecommendationCode recommendation = null;
        if (!QUALITY_INSUFFICIENT.equals(dataQuality)) {
            totalScore = clampScore(factors.stream().mapToInt(FactorResult::scoreContribution).sum());
            level = resolveStrengthLevel(totalScore, thresholds.strength());
            recommendation = StrengthRecommendationCode.fromStrengthLevel(level);
        }

        if (recommendation == null) {
            recommendation = StrengthRecommendationCode.REEVALUATE_WHEN_CONDITION_MET;
        }

        String summary = buildStrengthSummary(target, totalScore, level, dataQuality, factors, thresholds);

        StrengthSnapshot snapshot = strengthSnapshotRepository
            .findByUserIdAndStockIdAndTradeDateAndUniverseTypeAndWatchScopeId(
                userUid,
                target.stock().getId(),
                latest.getTradeDate(),
                target.universeType(),
                target.watchScopeId()
            )
            .orElseGet(StrengthSnapshot::new);

        snapshot.setUserId(userUid);
        snapshot.setStock(target.stock());
        snapshot.setTradeDate(latest.getTradeDate());
        snapshot.setUniverseType(target.universeType());
        snapshot.setWatchlist(target.watchlist());
        snapshot.setWatchScopeId(target.watchScopeId());
        snapshot.setPriceClose(scalePrice(latest.getClosePrice()));
        snapshot.setPriceTradeDate(latest.getTradeDate());
        snapshot.setStrengthScore(totalScore);
        snapshot.setStrengthLevel(level);
        snapshot.setRecommendationCode(recommendation);
        snapshot.setSummaryText(summary);
        snapshot.setDataQuality(dataQuality);
        snapshot.setStrategyVersion(strategyVersion);
        snapshot.setComputedAt(LocalDateTime.now(TAIPEI_ZONE));

        enforceWatchScopeRule(snapshot);
        StrengthSnapshot savedSnapshot = strengthSnapshotRepository.save(snapshot);

        strengthSnapshotFactorRepository.deleteBySnapshotId(savedSnapshot.getId());
        List<StrengthSnapshotFactor> factorEntities = factors.stream()
            .map(factor -> toFactorEntity(savedSnapshot, factor))
            .toList();
        strengthSnapshotFactorRepository.saveAll(factorEntities);

        return savedSnapshot;
    }

    private List<FactorResult> buildFactors(List<StockPriceDaily> historyDesc,
                                            String dataQuality,
                                            InvestStrategySettingService.ResolvedThresholds thresholds) {
        int minHistoryDays = thresholds.dataQuality().minHistoryDays();
        if (historyDesc.size() < minHistoryDays) {
            return List.of(new FactorResult(
                "DATA_INSUFFICIENT",
                "資料不足",
                "近 " + minHistoryDays + " 日行情不足",
                "至少需要 " + minHistoryDays + " 筆日行情（key: invest.strategy.data_quality.min_history_days）",
                0,
                "資料不足，暫不輸出 strength score。"
            ));
        }

        BigDecimal latestClose = nvl(historyDesc.get(0).getClosePrice());
        BigDecimal ma20 = avgClose(historyDesc, 20);
        BigDecimal ret5 = pctReturn(historyDesc, 0, 4);
        BigDecimal ret20 = pctReturn(historyDesc, 0, 19);
        BigDecimal volRatio = volumeRatio20(historyDesc);
        BigDecimal volatility10 = volatility10(historyDesc);
        BigDecimal drawdown20 = drawdownFromHigh20(historyDesc);

        List<FactorResult> factors = new ArrayList<>();
        factors.add(scoreTrendMa20(latestClose, ma20));
        factors.add(scoreMomentum(ret5, ret20));
        factors.add(scoreVolume(volRatio, ret5));
        factors.add(scoreVolatility(volatility10));
        factors.add(scoreDrawdown(drawdown20));
        factors = withSortOrder(factors);

        if (QUALITY_PARTIAL.equals(dataQuality) || QUALITY_STALE.equals(dataQuality)) {
            String suffix = QUALITY_STALE.equals(dataQuality)
                ? "（資料稍舊，建議先更新行情）"
                : "（部分欄位缺漏，請留意）";
            factors = factors.stream()
                .map(f -> f.withExplanation(f.explanation() + suffix))
                .toList();
        }

        return factors;
    }

    private FactorResult scoreTrendMa20(BigDecimal close, BigDecimal ma20) {
        BigDecimal ratioPercent = close.divide(ma20, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        int score;
        if (ratioPercent.compareTo(BigDecimal.valueOf(103)) >= 0) {
            score = 25;
        } else if (ratioPercent.compareTo(BigDecimal.valueOf(100)) >= 0) {
            score = 20;
        } else if (ratioPercent.compareTo(BigDecimal.valueOf(97)) >= 0) {
            score = 12;
        } else {
            score = 5;
        }
        String raw = "收盤/MA20=" + fmt(ratioPercent, 2) + "%";
        return new FactorResult(
            "TREND_MA20",
            "趨勢（MA20）",
            raw,
            ">=103%:25, >=100%:20, >=97%:12, 其他:5",
            score,
            "收盤價相對 MA20 越強，趨勢分數越高。"
        );
    }

    private FactorResult scoreMomentum(BigDecimal ret5, BigDecimal ret20) {
        int score;
        if (ret5.compareTo(BigDecimal.valueOf(3)) >= 0 && ret20.compareTo(BigDecimal.valueOf(8)) >= 0) {
            score = 25;
        } else if (ret5.compareTo(BigDecimal.ZERO) >= 0 && ret20.compareTo(BigDecimal.valueOf(3)) >= 0) {
            score = 18;
        } else if (ret20.compareTo(BigDecimal.ZERO) >= 0) {
            score = 12;
        } else if (ret5.compareTo(BigDecimal.valueOf(-3)) <= 0 && ret20.compareTo(BigDecimal.valueOf(-8)) <= 0) {
            score = 4;
        } else {
            score = 8;
        }

        String raw = "5日=" + fmt(ret5, 2) + "%, 20日=" + fmt(ret20, 2) + "%";
        return new FactorResult(
            "MOMENTUM_5D_20D",
            "動能（5日/20日報酬）",
            raw,
            "5日與20日報酬分段評分",
            score,
            "短中期報酬同向且為正，代表動能較穩。"
        );
    }

    private FactorResult scoreVolume(BigDecimal volRatio, BigDecimal ret5) {
        int score;
        if (volRatio.compareTo(BigDecimal.valueOf(1.5)) >= 0 && ret5.compareTo(BigDecimal.ZERO) >= 0) {
            score = 20;
        } else if (volRatio.compareTo(BigDecimal.ONE) >= 0 && ret5.compareTo(BigDecimal.ZERO) >= 0) {
            score = 15;
        } else if (volRatio.compareTo(BigDecimal.valueOf(0.7)) >= 0) {
            score = 10;
        } else if (volRatio.compareTo(BigDecimal.valueOf(1.5)) >= 0 && ret5.compareTo(BigDecimal.ZERO) < 0) {
            score = 6;
        } else {
            score = 8;
        }

        String raw = "量比=" + fmt(volRatio, 2) + " 倍";
        return new FactorResult(
            "VOLUME_20D_RATIO",
            "量能確認（20日均量）",
            raw,
            "量比與方向分段評分",
            score,
            "上漲伴隨量能放大，訊號可信度通常較高。"
        );
    }

    private FactorResult scoreVolatility(BigDecimal volatility10) {
        int score;
        if (volatility10.compareTo(BigDecimal.valueOf(1.5)) <= 0) {
            score = 15;
        } else if (volatility10.compareTo(BigDecimal.valueOf(2.5)) <= 0) {
            score = 12;
        } else if (volatility10.compareTo(BigDecimal.valueOf(4)) <= 0) {
            score = 8;
        } else if (volatility10.compareTo(BigDecimal.valueOf(6)) <= 0) {
            score = 5;
        } else {
            score = 2;
        }

        String raw = "10日波動=" + fmt(volatility10, 2) + "%";
        return new FactorResult(
            "VOLATILITY_10D",
            "穩定度（10日波動）",
            raw,
            "<=1.5:15, <=2.5:12, <=4:8, <=6:5, >6:2",
            score,
            "波動越低，代表走勢越穩定。"
        );
    }

    private FactorResult scoreDrawdown(BigDecimal drawdown20) {
        BigDecimal drawdownAbs = drawdown20.abs();
        int score;
        if (drawdownAbs.compareTo(BigDecimal.valueOf(3)) <= 0) {
            score = 15;
        } else if (drawdownAbs.compareTo(BigDecimal.valueOf(8)) <= 0) {
            score = 12;
        } else if (drawdownAbs.compareTo(BigDecimal.valueOf(15)) <= 0) {
            score = 8;
        } else if (drawdownAbs.compareTo(BigDecimal.valueOf(25)) <= 0) {
            score = 4;
        } else {
            score = 1;
        }

        String raw = "距20日高點回撤=" + fmt(drawdown20, 2) + "%";
        return new FactorResult(
            "DRAWDOWN_20D",
            "回撤（20日高點）",
            raw,
            "回撤越小分數越高",
            score,
            "距離近期高點回撤越深，表示承壓越大。"
        );
    }

    private int syncOpportunitySignals(String userUid,
                                       List<StrengthSnapshot> snapshots,
                                       int strategyVersion,
                                       InvestStrategySettingService.ResolvedThresholds thresholds) {
        Map<Long, StrengthSnapshot> bestByStock = pickBestSnapshotPerStock(snapshots);

        Set<Long> analyzedStockIds = new HashSet<>(bestByStock.keySet());
        for (StrengthSnapshot snapshot : bestByStock.values()) {
            SignalDecision decision = decideSignal(snapshot, thresholds);
            upsertSignal(userUid, snapshot, decision, strategyVersion);
        }

        expireSignalsNotInCurrentRun(userUid, analyzedStockIds);
        return (int) opportunitySignalRepository.findByUserIdAndStatus(userUid, OpportunitySignalStatusCode.ACTIVE).size();
    }

    private Map<Long, StrengthSnapshot> pickBestSnapshotPerStock(List<StrengthSnapshot> snapshots) {
        Map<Long, StrengthSnapshot> bestMap = new LinkedHashMap<>();
        for (StrengthSnapshot snapshot : snapshots) {
            if (snapshot.getStock() == null || snapshot.getStock().getId() == null) {
                continue;
            }
            Long stockId = snapshot.getStock().getId();
            StrengthSnapshot current = bestMap.get(stockId);
            if (current == null || compareSnapshotPriority(snapshot, current) < 0) {
                bestMap.put(stockId, snapshot);
            }
        }
        return bestMap;
    }

    private int compareSnapshotPriority(StrengthSnapshot a, StrengthSnapshot b) {
        int q = Integer.compare(qualityRank(a.getDataQuality()), qualityRank(b.getDataQuality()));
        if (q != 0) {
            return q;
        }
        int scoreA = a.getStrengthScore() == null ? -1 : a.getStrengthScore();
        int scoreB = b.getStrengthScore() == null ? -1 : b.getStrengthScore();
        int scoreCompare = Integer.compare(scoreB, scoreA);
        if (scoreCompare != 0) {
            return scoreCompare;
        }
        return b.getComputedAt().compareTo(a.getComputedAt());
    }

    private SignalDecision decideSignal(StrengthSnapshot snapshot,
                                        InvestStrategySettingService.ResolvedThresholds thresholds) {
        String quality = snapshot.getDataQuality();
        Integer score = snapshot.getStrengthScore();
        InvestStrategySettingService.OpportunityThresholds opportunityThresholds = thresholds.opportunity();
        String opportunityTrace = buildOpportunityThresholdTrace(opportunityThresholds);
        String dataQualityTrace = buildDataQualityThresholdTrace(thresholds.dataQuality());

        // 先依資料品質短路，避免低品質資料被誤判為可觀察訊號
        if (QUALITY_INSUFFICIENT.equals(quality) || QUALITY_STALE.equals(quality)) {
            return new SignalDecision(
                OpportunitySignalTypeCode.HIGH_RISK_AVOID_CHASE,
                StrengthRecommendationCode.NOT_SUITABLE_CHASE,
                QUALITY_STALE.equals(quality) ? 35 : 20,
                "行情資料品質不足（" + quality + "），先補齊資料再評估。[" + dataQualityTrace + "]",
                "目前資料品質不足，不建議追價。",
                List.of(
                    reason("資料品質提醒", "目前資料品質為 " + quality + "，訊號可信度降低。", 10),
                    reason("建議先觀察", "建議先完成行情更新後再重新評估。", 8)
                )
            );
        }

        int safeScore = score == null ? 0 : score;
        if (safeScore >= opportunityThresholds.observeMinScore()) {
            return new SignalDecision(
                OpportunitySignalTypeCode.MOMENTUM_OBSERVE,
                StrengthRecommendationCode.OBSERVE,
                safeScore,
                "若後續維持站上 MA20 且量能未明顯轉弱，持續觀察。[" + opportunityTrace + "]",
                "走勢偏強，但仍不等於買進指令。",
                List.of(
                    reason("強勢延續", "分數達到 " + safeScore + "，屬於相對強勢區。", 20),
                    reason("避免追高", "即使偏強，也需防短線拉回。", 12)
                )
            );
        }

        if (safeScore >= opportunityThresholds.waitPullbackMinScore()) {
            return new SignalDecision(
                OpportunitySignalTypeCode.PULLBACK_WATCH,
                StrengthRecommendationCode.WAIT_PULLBACK,
                safeScore,
                "可等待回測 MA20 附近且不破，再重新評估。[" + opportunityTrace + "]",
                "建議等待更好的風險報酬位置，不追價。",
                List.of(
                    reason("回檔觀察", "中高分區間，適合等待回檔確認。", 15),
                    reason("分批判斷", "可搭配後續量價變化再決定。", 10)
                )
            );
        }

        if (safeScore >= opportunityThresholds.reevaluateMinScore()) {
            return new SignalDecision(
                OpportunitySignalTypeCode.BREAKOUT_REEVALUATE,
                StrengthRecommendationCode.REEVALUATE_WHEN_CONDITION_MET,
                safeScore,
                "若後續量增突破近 20 日高點，再重新評估。[" + opportunityTrace + "]",
                "目前條件未完整，先觀察等待條件成立。",
                List.of(
                    reason("條件待確認", "分數中性，尚未形成明確優勢。", 12),
                    reason("等待訊號", "需等突破或量能確認後再評估。", 9)
                )
            );
        }

        return new SignalDecision(
            OpportunitySignalTypeCode.HIGH_RISK_AVOID_CHASE,
            StrengthRecommendationCode.NOT_SUITABLE_CHASE,
            safeScore,
            "目前強度偏弱，應優先控制風險。[" + opportunityTrace + "]",
            "不建議追價，先觀察是否止跌與修復。",
            List.of(
                reason("強度偏弱", "目前分數偏低，走勢不穩。", 10),
                reason("風險提醒", "先觀察，不要因短線波動追高。", 10)
            )
        );
    }

    private void upsertSignal(String userUid, StrengthSnapshot snapshot, SignalDecision decision, int strategyVersion) {
        Long stockId = snapshot.getStock().getId();
        String signalKey = decision.signalType().name();

        OpportunitySignal signal = opportunitySignalRepository.findByUserIdAndStockIdAndSignalKey(userUid, stockId, signalKey)
            .orElseGet(OpportunitySignal::new);

        signal.setUserId(userUid);
        signal.setStock(snapshot.getStock());
        signal.setTradeDate(snapshot.getTradeDate());
        signal.setSignalKey(signalKey);
        signal.setSignalType(decision.signalType());
        signal.setSignalScore(clampScore(decision.signalScore()));
        signal.setRecommendationCode(decision.recommendationCode());
        signal.setConditionText(decision.conditionText());
        signal.setSummaryText(decision.summaryText());
        signal.setDisclaimerText(DISCLAIMER + " " + decision.disclaimerText());
        signal.setStatus(OpportunitySignalStatusCode.ACTIVE);
        signal.setStrategyVersion(strategyVersion);
        signal.setSourceSnapshot(snapshot);

        OpportunitySignal savedSignal = opportunitySignalRepository.save(signal);

        opportunitySignalReasonRepository.deleteBySignalId(savedSignal.getId());
        List<OpportunitySignalReason> reasons = decision.reasons().stream()
            .limit(3)
            .map(reason -> {
                OpportunitySignalReason entity = new OpportunitySignalReason();
                entity.setSignal(savedSignal);
                entity.setReasonTitle(reason.getReasonTitle());
                entity.setReasonDetail(reason.getReasonDetail());
                entity.setScoreImpact(reason.getScoreImpact());
                return entity;
            })
            .toList();
        for (int i = 0; i < reasons.size(); i++) {
            reasons.get(i).setSortOrder(i + 1);
        }
        opportunitySignalReasonRepository.saveAll(reasons);

        List<OpportunitySignal> activeSignals = opportunitySignalRepository.findByUserAndStockAndStatus(
            userUid,
            stockId,
            OpportunitySignalStatusCode.ACTIVE
        );
        for (OpportunitySignal active : activeSignals) {
            if (active.getId().equals(savedSignal.getId())) {
                continue;
            }
            active.setStatus(OpportunitySignalStatusCode.EXPIRED);
            opportunitySignalRepository.save(active);
        }
    }

    private void expireSignalsNotInCurrentRun(String userUid, Set<Long> analyzedStockIds) {
        List<OpportunitySignal> activeSignals = opportunitySignalRepository.findByUserIdAndStatus(
            userUid,
            OpportunitySignalStatusCode.ACTIVE
        );
        for (OpportunitySignal signal : activeSignals) {
            Long stockId = signal.getStock() == null ? null : signal.getStock().getId();
            if (stockId == null || analyzedStockIds.contains(stockId)) {
                continue;
            }
            signal.setStatus(OpportunitySignalStatusCode.EXPIRED);
            opportunitySignalRepository.save(signal);
        }
    }

    private void enforceWatchScopeRule(AnalysisTarget target) {
        if (target.universeType() == MarketUniverseTypeCode.HOLDING) {
            if (target.watchScopeId() == null || target.watchScopeId() != 0L || target.watchlist() != null) {
                throw new RuntimeException("HOLDING scope 規則錯誤：watch_scope_id 必須為 0 且 watchlist_id 必須為 null");
            }
            return;
        }

        if (target.universeType() == MarketUniverseTypeCode.WATCHLIST) {
            Long watchlistId = target.watchlist() == null ? null : target.watchlist().getId();
            if (watchlistId == null || target.watchScopeId() == null || !watchlistId.equals(target.watchScopeId())) {
                throw new RuntimeException("WATCHLIST scope 規則錯誤：watch_scope_id 必須等於 watchlist_id");
            }
        }
    }

    private void enforceWatchScopeRule(StrengthSnapshot snapshot) {
        if (snapshot.getUniverseType() == MarketUniverseTypeCode.HOLDING) {
            snapshot.setWatchlist(null);
            snapshot.setWatchScopeId(0L);
            return;
        }

        if (snapshot.getUniverseType() == MarketUniverseTypeCode.WATCHLIST) {
            if (snapshot.getWatchlist() == null || snapshot.getWatchlist().getId() == null) {
                throw new RuntimeException("WATCHLIST scope 需要 watchlist_id");
            }
            snapshot.setWatchScopeId(snapshot.getWatchlist().getId());
        }
    }

    private StrengthSnapshotFactor toFactorEntity(StrengthSnapshot snapshot, FactorResult factorResult) {
        StrengthSnapshotFactor entity = new StrengthSnapshotFactor();
        entity.setSnapshot(snapshot);
        entity.setFactorCode(factorResult.factorCode());
        entity.setFactorName(factorResult.factorName());
        entity.setRawValueText(factorResult.rawValue());
        entity.setThresholdText(factorResult.threshold());
        entity.setScoreContribution(factorResult.scoreContribution());
        entity.setExplanationText(factorResult.explanation());
        entity.setSortOrder(factorResult.sortOrder());
        return entity;
    }

    private String resolveDataQuality(StockPriceDaily latest,
                                      List<StockPriceDaily> history,
                                      InvestStrategySettingService.ResolvedThresholds thresholds) {
        int minHistoryDays = thresholds.dataQuality().minHistoryDays();
        if (latest == null || history == null || history.size() < minHistoryDays) {
            return QUALITY_INSUFFICIENT;
        }

        long staleDays = ChronoUnit.DAYS.between(latest.getTradeDate(), LocalDate.now(TAIPEI_ZONE));
        if (staleDays > thresholds.dataQuality().staleDays()) {
            return QUALITY_STALE;
        }

        boolean hasPartial = history.stream().limit(minHistoryDays).anyMatch(item ->
            item.getClosePrice() == null
                || item.getVolume() == null
                || item.getVolume() <= 0
        );
        if (hasPartial) {
            return QUALITY_PARTIAL;
        }

        return QUALITY_GOOD;
    }

    private String buildStrengthSummary(AnalysisTarget target,
                                        Integer score,
                                        StrengthLevelCode level,
                                        String dataQuality,
                                        List<FactorResult> factors,
                                        InvestStrategySettingService.ResolvedThresholds thresholds) {
        if (QUALITY_INSUFFICIENT.equals(dataQuality) || score == null || level == null) {
            return "資料不足，暫無法產生完整強度分數，請先補齊行情資料。[" + buildDataQualityThresholdTrace(thresholds.dataQuality()) + "]";
        }

        String reasonText = factors.stream()
            .sorted(Comparator.comparingInt(FactorResult::scoreContribution).reversed())
            .limit(2)
            .map(FactorResult::factorName)
            .collect(Collectors.joining("、"));

        String scopeText = target.universeType() == MarketUniverseTypeCode.HOLDING ? "持股" : "觀察清單";
        return String.format(
            "%s 分析：強度 %d（%s），主要依據：%s。[%s]",
            scopeText,
            score,
            level.name(),
            reasonText,
            buildStrengthThresholdTrace(thresholds.strength())
        );
    }

    private StrengthLevelCode resolveStrengthLevel(Integer score,
                                                   InvestStrategySettingService.StrengthThresholds thresholds) {
        int safeScore = score == null ? 0 : score;
        if (safeScore >= thresholds.strongMin()) {
            return StrengthLevelCode.STRONG;
        }
        if (safeScore >= thresholds.goodMin()) {
            return StrengthLevelCode.GOOD;
        }
        if (safeScore <= thresholds.weakMax()) {
            return StrengthLevelCode.WEAK;
        }
        return StrengthLevelCode.NEUTRAL;
    }

    private String buildStrengthThresholdTrace(InvestStrategySettingService.StrengthThresholds thresholds) {
        return "key:strength S>=" + thresholds.strongMin() + "/G>=" + thresholds.goodMin() + "/W<=" + thresholds.weakMax();
    }

    private String buildDataQualityThresholdTrace(InvestStrategySettingService.DataQualityThresholds thresholds) {
        return "key:data_quality history>=" + thresholds.minHistoryDays() + ", stale<=" + thresholds.staleDays() + "d";
    }

    private String buildOpportunityThresholdTrace(InvestStrategySettingService.OpportunityThresholds thresholds) {
        return "key:opportunity OBS>=" + thresholds.observeMinScore()
            + "/WAIT>=" + thresholds.waitPullbackMinScore()
            + "/REEV>=" + thresholds.reevaluateMinScore();
    }

    private LocalDate resolveDataAsOfTradeDate(List<StrengthSnapshot> snapshots) {
        return snapshots.stream()
            .map(StrengthSnapshot::getTradeDate)
            .filter(Objects::nonNull)
            .max(LocalDate::compareTo)
            .orElse(null);
    }

    private String resolveRunStatus(int analyzedCount, int failCount) {
        if (failCount <= 0) {
            return "SUCCESS";
        }
        if (analyzedCount > 0) {
            return "PARTIAL_FAILED";
        }
        return "FAILED";
    }

    private String buildRunMessage(int analyzedCount, List<String> failures) {
        if (failures.isEmpty()) {
            return "市場分析完成";
        }

        String failedTickers = failures.stream().limit(5).collect(Collectors.joining("；"));
        if (analyzedCount > 0) {
            return "市場分析部分失敗，失敗明細：" + failedTickers;
        }
        return "市場分析失敗，失敗明細：" + failedTickers;
    }

    private int clampScore(int score) {
        if (score < 0) {
            return 0;
        }
        return Math.min(score, SCORE_CAP);
    }

    private BigDecimal avgClose(List<StockPriceDaily> historyDesc, int count) {
        return historyDesc.stream().limit(count)
            .map(StockPriceDaily::getClosePrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(count), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal pctReturn(List<StockPriceDaily> historyDesc, int startIndex, int compareIndex) {
        BigDecimal latest = nvl(historyDesc.get(startIndex).getClosePrice());
        BigDecimal previous = nvl(historyDesc.get(compareIndex).getClosePrice());
        if (previous.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return latest.subtract(previous)
            .divide(previous, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal volumeRatio20(List<StockPriceDaily> historyDesc) {
        long latestVolume = nvlLong(historyDesc.get(0).getVolume());
        BigDecimal avg = historyDesc.stream().limit(20)
            .map(StockPriceDaily::getVolume)
            .map(this::nvlLong)
            .map(BigDecimal::valueOf)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(20), 6, RoundingMode.HALF_UP);
        if (avg.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.valueOf(latestVolume).divide(avg, 6, RoundingMode.HALF_UP);
    }

    private BigDecimal volatility10(List<StockPriceDaily> historyDesc) {
        List<BigDecimal> closes = historyDesc.stream().limit(10)
            .map(StockPriceDaily::getClosePrice)
            .map(this::nvl)
            .toList();
        List<Double> returns = new ArrayList<>();
        for (int i = 0; i < closes.size() - 1; i++) {
            double cur = closes.get(i).doubleValue();
            double prev = closes.get(i + 1).doubleValue();
            if (prev == 0) {
                continue;
            }
            returns.add(((cur - prev) / prev) * 100.0);
        }
        if (returns.isEmpty()) {
            return BigDecimal.ZERO;
        }
        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0D);
        double variance = returns.stream()
            .mapToDouble(value -> Math.pow(value - mean, 2))
            .average()
            .orElse(0D);
        double std = Math.sqrt(variance);
        return BigDecimal.valueOf(std).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal drawdownFromHigh20(List<StockPriceDaily> historyDesc) {
        BigDecimal latest = nvl(historyDesc.get(0).getClosePrice());
        BigDecimal high20 = historyDesc.stream().limit(20)
            .map(StockPriceDaily::getClosePrice)
            .map(this::nvl)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
        if (high20.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return latest.subtract(high20)
            .divide(high20, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    private int qualityRank(String quality) {
        if (QUALITY_GOOD.equals(quality)) {
            return 0;
        }
        if (QUALITY_PARTIAL.equals(quality)) {
            return 1;
        }
        if (QUALITY_STALE.equals(quality)) {
            return 2;
        }
        return 3;
    }

    private BigDecimal scalePrice(BigDecimal price) {
        return nvl(price).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long nvlLong(Long value) {
        return value == null ? 0L : value;
    }

    private String fmt(BigDecimal value, int scale) {
        return nvl(value).setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }

    private String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "未知錯誤";
        }
        return message.length() > 180 ? message.substring(0, 180) : message;
    }

    private OpportunitySignalReasonDto reason(String title, String detail, int impact) {
        OpportunitySignalReasonDto dto = new OpportunitySignalReasonDto();
        dto.setReasonTitle(title);
        dto.setReasonDetail(detail);
        dto.setScoreImpact(impact);
        return dto;
    }

    private String buildTargetKey(AnalysisTarget target) {
        return target.universeType().name() + "-" + target.watchScopeId() + "-" + target.stock().getId();
    }

    private List<FactorResult> withSortOrder(List<FactorResult> factors) {
        List<FactorResult> ordered = new ArrayList<>(factors.size());
        for (int i = 0; i < factors.size(); i++) {
            ordered.add(factors.get(i).withSortOrder(i + 1));
        }
        return ordered;
    }

    private record AnalysisTarget(Stock stock,
                                  MarketUniverseTypeCode universeType,
                                  Watchlist watchlist,
                                  Long watchScopeId) {
    }

    private record FactorResult(String factorCode,
                                String factorName,
                                String rawValue,
                                String threshold,
                                int scoreContribution,
                                String explanation,
                                int sortOrder) {
        private FactorResult(String factorCode,
                             String factorName,
                             String rawValue,
                             String threshold,
                             int scoreContribution,
                             String explanation) {
            this(factorCode, factorName, rawValue, threshold, scoreContribution, explanation, 0);
        }

        private FactorResult withExplanation(String newExplanation) {
            return new FactorResult(factorCode, factorName, rawValue, threshold, scoreContribution, newExplanation, sortOrder);
        }

        private FactorResult withSortOrder(int newSortOrder) {
            return new FactorResult(factorCode, factorName, rawValue, threshold, scoreContribution, explanation, newSortOrder);
        }
    }

    private record SignalDecision(OpportunitySignalTypeCode signalType,
                                  StrengthRecommendationCode recommendationCode,
                                  int signalScore,
                                  String conditionText,
                                  String summaryText,
                                  List<OpportunitySignalReasonDto> reasons,
                                  String disclaimerText) {
        private SignalDecision(OpportunitySignalTypeCode signalType,
                               StrengthRecommendationCode recommendationCode,
                               int signalScore,
                               String conditionText,
                               String summaryText,
                               List<OpportunitySignalReasonDto> reasons) {
            this(signalType, recommendationCode, signalScore, conditionText, summaryText, reasons, "請勿將本訊號視為買進或保證獲利依據。");
        }
    }
}
