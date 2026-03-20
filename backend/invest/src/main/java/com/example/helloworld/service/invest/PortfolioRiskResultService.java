package com.example.helloworld.service.invest;

import com.example.helloworld.dto.invest.PortfolioRiskReasonDto;
import com.example.helloworld.dto.invest.PortfolioRiskResultDetailDto;
import com.example.helloworld.dto.invest.PortfolioRiskResultPagedDto;
import com.example.helloworld.entity.invest.*;
import com.example.helloworld.repository.invest.*;
import com.example.helloworld.service.invest.auth.InvestCurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class PortfolioRiskResultService {

    private static final String RULE_BREAK_COST = "BREAK_COST";
    private static final String RULE_CONSECUTIVE_DOWN = "CONSECUTIVE_DOWN";
    private static final String RULE_BELOW_MA5 = "BELOW_MA5";
    private static final String RULE_HIGH_VOLUME_DROP = "HIGH_VOLUME_DROP";
    private static final String RULE_LARGE_INTRADAY_SWING = "LARGE_INTRADAY_SWING";

    private static final String DISCLAIMER = "本系統為輔助工具，不保證獲利，分析結果不可取代你的自主判斷。";
    private static final int SCORE_CAP = 100;

    private static final Map<String, Integer> RULE_PRIORITY = Map.of(
        RULE_BREAK_COST, 1,
        RULE_CONSECUTIVE_DOWN, 2,
        RULE_BELOW_MA5, 3,
        RULE_HIGH_VOLUME_DROP, 4,
        RULE_LARGE_INTRADAY_SWING, 5
    );

    private static final List<String> RULE_ORDER = List.of(
        RULE_BREAK_COST,
        RULE_CONSECUTIVE_DOWN,
        RULE_BELOW_MA5,
        RULE_HIGH_VOLUME_DROP,
        RULE_LARGE_INTRADAY_SWING
    );

    private final PortfolioRepository portfolioRepository;
    private final StockPriceDailyRepository stockPriceDailyRepository;
    private final RiskRuleRepository riskRuleRepository;
    private final PortfolioRiskResultRepository portfolioRiskResultRepository;
    private final PortfolioRiskReasonRepository portfolioRiskReasonRepository;
    private final InvestCurrentUserService investCurrentUserService;

    public PortfolioRiskResultService(PortfolioRepository portfolioRepository,
                                      StockPriceDailyRepository stockPriceDailyRepository,
                                      RiskRuleRepository riskRuleRepository,
                                      PortfolioRiskResultRepository portfolioRiskResultRepository,
                                      PortfolioRiskReasonRepository portfolioRiskReasonRepository,
                                      InvestCurrentUserService investCurrentUserService) {
        this.portfolioRepository = portfolioRepository;
        this.stockPriceDailyRepository = stockPriceDailyRepository;
        this.riskRuleRepository = riskRuleRepository;
        this.portfolioRiskResultRepository = portfolioRiskResultRepository;
        this.portfolioRiskReasonRepository = portfolioRiskReasonRepository;
        this.investCurrentUserService = investCurrentUserService;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<PortfolioRiskResultPagedDto> getPaged(LocalDate tradeDate,
                                                      String riskLevel,
                                                      String recommendation,
                                                      int page,
                                                      int size) {
        String currentUserUid = resolveCurrentUserUid();
        RiskLevelCode levelCode = parseRiskLevel(riskLevel);
        RecommendationCode recommendationCode = parseRecommendation(recommendation);

        Pageable pageable = PageRequest.of(page, size);
        Page<PortfolioRiskResult> resultPage = portfolioRiskResultRepository.findByUserAndFilters(
            currentUserUid, tradeDate, levelCode, recommendationCode, pageable
        );

        List<Long> resultIds = resultPage.getContent().stream().map(PortfolioRiskResult::getId).toList();
        Map<Long, List<PortfolioRiskReason>> reasonMap = loadReasonMap(resultIds);

        return resultPage.map(result -> toPagedDto(result, reasonMap.getOrDefault(result.getId(), List.of())));
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public PortfolioRiskResultDetailDto getById(Long id) {
        String currentUserUid = resolveCurrentUserUid();
        PortfolioRiskResult result = portfolioRiskResultRepository.findByIdAndPortfolioUserId(id, currentUserUid)
            .orElseThrow(() -> new RuntimeException("找不到風險分析結果或無權限，id=" + id));

        List<PortfolioRiskReason> reasons = portfolioRiskReasonRepository.findByRiskResultId(result.getId());
        return toDetailDto(result, reasons);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public PortfolioRiskResultDetailDto getLatest(Long portfolioId) {
        String currentUserUid = resolveCurrentUserUid();
        portfolioRepository.findByIdAndUserId(portfolioId, currentUserUid)
            .orElseThrow(() -> new RuntimeException("找不到持股資料或無權限，id=" + portfolioId));

        PortfolioRiskResult result = portfolioRiskResultRepository
            .findTopByPortfolioIdAndPortfolioUserIdOrderByTradeDateDescCreatedAtDesc(portfolioId, currentUserUid)
            .orElseThrow(() -> new RuntimeException("該持股尚無風險分析結果，portfolioId=" + portfolioId));

        List<PortfolioRiskReason> reasons = portfolioRiskReasonRepository.findByRiskResultId(result.getId());
        return toDetailDto(result, reasons);
    }

    public PortfolioRiskResultDetailDto recalculate(Long portfolioId) {
        String currentUserUid = resolveCurrentUserUid();
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, currentUserUid)
            .orElseThrow(() -> new RuntimeException("找不到持股資料或無權限，id=" + portfolioId));

        StockPriceDaily latest = stockPriceDailyRepository
            .findTopByStockIdOrderByTradeDateDesc(portfolio.getStock().getId())
            .orElseThrow(() -> new RuntimeException("該持股尚無每日行情資料，無法進行風險分析"));
        return recalculateForPortfolio(portfolio, latest.getTradeDate());
    }

    public PortfolioRiskResultDetailDto recalculateForBatch(Long portfolioId, LocalDate tradeDate) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new RuntimeException("找不到持股資料，id=" + portfolioId));
        return recalculateForPortfolio(portfolio, tradeDate);
    }

    private PortfolioRiskResultDetailDto recalculateForPortfolio(Portfolio portfolio, LocalDate tradeDate) {
        if (tradeDate == null) {
            throw new RuntimeException("tradeDate 不可為空");
        }

        Long stockId = portfolio.getStock().getId();
        StockPriceDaily latest = stockPriceDailyRepository.findByStockIdAndTradeDate(stockId, tradeDate)
            .orElseThrow(() -> new RuntimeException("找不到指定交易日行情資料，stockId=" + stockId + ", tradeDate=" + tradeDate));

        List<StockPriceDaily> history = stockPriceDailyRepository
            .findTop30ByStockIdAndTradeDateLessThanEqualOrderByTradeDateDesc(stockId, tradeDate);
        if (history.isEmpty()) {
            throw new RuntimeException("該持股尚無每日行情資料，無法進行風險分析");
        }

        Map<String, RiskRule> ruleConfigMap = riskRuleRepository.findAllByOrderByIdAsc().stream()
            .collect(Collectors.toMap(RiskRule::getRuleCode, r -> r, (a, b) -> a, LinkedHashMap::new));

        List<CalculatedReason> calculatedReasons = new ArrayList<>();
        for (String ruleCode : RULE_ORDER) {
            RiskRule rule = ruleConfigMap.get(ruleCode);
            if (rule == null || !Boolean.TRUE.equals(rule.getEnabled())) {
                continue;
            }

            RuleEvaluation evaluation = evaluateRule(ruleCode, portfolio, latest, history);
            if (evaluation == null || evaluation.baseImpact() <= 0) {
                continue;
            }

            int weightedImpact = applyWeightedScore(evaluation.baseImpact(), rule.getScoreWeight());
            if (weightedImpact <= 0) {
                continue;
            }

            calculatedReasons.add(new CalculatedReason(
                rule.getRuleCode(),
                evaluation.title(),
                evaluation.detail(),
                rule.getSeverity(),
                weightedImpact
            ));
        }

        int totalScore = calculatedReasons.stream().mapToInt(CalculatedReason::scoreImpact).sum();
        if (totalScore > SCORE_CAP) {
            totalScore = SCORE_CAP;
        }

        RiskLevelCode riskLevel = RiskLevelCode.fromScore(totalScore);
        RecommendationCode recommendation = RecommendationCode.fromRiskLevel(riskLevel);

        List<CalculatedReason> sortedCalculatedReasons = sortCalculatedReasons(calculatedReasons);
        String summary = buildSummary(totalScore, riskLevel, recommendation, sortedCalculatedReasons);

        PortfolioRiskResult result = portfolioRiskResultRepository.findByPortfolioIdAndTradeDate(portfolio.getId(), tradeDate)
            .orElseGet(PortfolioRiskResult::new);
        result.setPortfolio(portfolio);
        result.setTradeDate(tradeDate);
        result.setRiskScore(totalScore);
        result.setRiskLevel(riskLevel);
        result.setRecommendation(recommendation);
        result.setSummaryText(summary);
        PortfolioRiskResult savedResult = portfolioRiskResultRepository.save(result);

        portfolioRiskReasonRepository.deleteByRiskResultId(savedResult.getId());
        List<PortfolioRiskReason> reasonsToSave = sortedCalculatedReasons.stream()
            .map(reason -> toRiskReasonEntity(savedResult, reason))
            .toList();
        List<PortfolioRiskReason> savedReasons = portfolioRiskReasonRepository.saveAll(reasonsToSave);

        return toDetailDto(savedResult, savedReasons);
    }

    private RuleEvaluation evaluateRule(String ruleCode,
                                        Portfolio portfolio,
                                        StockPriceDaily latest,
                                        List<StockPriceDaily> historyDesc) {
        return switch (ruleCode) {
            case RULE_BREAK_COST -> evaluateBreakCost(portfolio, latest);
            case RULE_CONSECUTIVE_DOWN -> evaluateConsecutiveDown(historyDesc);
            case RULE_BELOW_MA5 -> evaluateBelowMa5(historyDesc);
            case RULE_HIGH_VOLUME_DROP -> evaluateHighVolumeDrop(historyDesc);
            case RULE_LARGE_INTRADAY_SWING -> evaluateLargeIntradaySwing(latest);
            default -> null;
        };
    }

    private RuleEvaluation evaluateBreakCost(Portfolio portfolio, StockPriceDaily latest) {
        BigDecimal avgCost = portfolio.getAvgCost();
        BigDecimal latestClose = latest.getClosePrice();
        if (avgCost == null || latestClose == null || latestClose.compareTo(avgCost) >= 0) {
            return null;
        }

        BigDecimal lossPercent = avgCost.subtract(latestClose)
            .divide(avgCost, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
        int baseScore = breakCostBaseScore(lossPercent);
        if (baseScore <= 0) {
            return null;
        }

        String detail = String.format(
            "收盤價 %.4f 低於平均成本 %.4f，約虧損 %s%%。白話：目前價格已跌破你的持股成本。新手提醒：先確認原始持股理由，不要因為短線下跌就盲目攤平。",
            latestClose, avgCost, lossPercent
        );
        return new RuleEvaluation("跌破持股成本", detail, baseScore);
    }

    private RuleEvaluation evaluateConsecutiveDown(List<StockPriceDaily> historyDesc) {
        if (historyDesc.size() < 3) {
            return null;
        }

        boolean down3 = isStrictlyLower(historyDesc.get(0).getClosePrice(), historyDesc.get(1).getClosePrice())
            && isStrictlyLower(historyDesc.get(1).getClosePrice(), historyDesc.get(2).getClosePrice());
        if (!down3) {
            return null;
        }

        int baseScore = 15;
        String dayText = "最近 3 個交易日收盤連續走低";
        if (historyDesc.size() >= 4
            && isStrictlyLower(historyDesc.get(2).getClosePrice(), historyDesc.get(3).getClosePrice())) {
            baseScore = 20;
            dayText = "最近 4 個交易日收盤連續走低";
        }

        String detail = dayText + "。白話：短線賣壓仍在延續。新手提醒：先觀察是否止跌，不要因短期波動情緒化操作。";
        return new RuleEvaluation("短期連續下跌", detail, baseScore);
    }

    private RuleEvaluation evaluateBelowMa5(List<StockPriceDaily> historyDesc) {
        if (historyDesc.size() < 5) {
            return null;
        }

        BigDecimal latestClose = historyDesc.get(0).getClosePrice();
        BigDecimal ma5 = historyDesc.subList(0, 5).stream()
            .map(StockPriceDaily::getClosePrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(5), 6, RoundingMode.HALF_UP);

        if (latestClose.compareTo(ma5) >= 0) {
            return null;
        }

        BigDecimal deviationPercent = ma5.subtract(latestClose)
            .divide(ma5, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);

        if (deviationPercent.compareTo(BigDecimal.valueOf(2)) < 0) {
            return null;
        }

        int baseScore = deviationPercent.compareTo(BigDecimal.valueOf(5)) >= 0 ? 25 : 20;
        String detail = String.format(
            "最新收盤 %.4f 低於 MA5 %.4f，偏離 %s%%。白話：短線趨勢轉弱。新手提醒：單一訊號不代表一定續跌，請搭配資金控管。",
            latestClose, ma5.setScale(4, RoundingMode.HALF_UP), deviationPercent
        );
        return new RuleEvaluation("跌破短期均線", detail, baseScore);
    }

    private RuleEvaluation evaluateHighVolumeDrop(List<StockPriceDaily> historyDesc) {
        if (historyDesc.size() < 6) {
            return null;
        }

        StockPriceDaily latest = historyDesc.get(0);
        BigDecimal dropPercent = resolveLatestDropPercent(historyDesc);
        if (dropPercent == null || dropPercent.compareTo(BigDecimal.valueOf(-3)) > 0) {
            return null;
        }

        BigDecimal avgPrev5Volume = historyDesc.subList(1, 6).stream()
            .map(StockPriceDaily::getVolume)
            .map(BigDecimal::valueOf)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(5), 6, RoundingMode.HALF_UP);

        if (avgPrev5Volume.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal volumeRatio = BigDecimal.valueOf(latest.getVolume())
            .divide(avgPrev5Volume, 4, RoundingMode.HALF_UP);

        if (volumeRatio.compareTo(BigDecimal.valueOf(1.8)) < 0) {
            return null;
        }

        String detail = String.format(
            "當日跌幅 %s%%，成交量為近 5 日均量的 %s 倍。白話：放量下跌通常代表短線風險升高。新手提醒：避免追價或急著攤平。",
            dropPercent.setScale(2, RoundingMode.HALF_UP),
            volumeRatio.setScale(2, RoundingMode.HALF_UP)
        );
        return new RuleEvaluation("爆量下跌", detail, 25);
    }

    private RuleEvaluation evaluateLargeIntradaySwing(StockPriceDaily latest) {
        if (latest.getClosePrice() == null || latest.getClosePrice().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal swingPercent = latest.getHighPrice().subtract(latest.getLowPrice())
            .divide(latest.getClosePrice(), 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);

        if (swingPercent.compareTo(BigDecimal.valueOf(7)) < 0) {
            return null;
        }

        int baseScore = swingPercent.compareTo(BigDecimal.valueOf(10)) >= 0 ? 25 : 20;
        String detail = String.format(
            "單日振幅 %s%%。白話：價格在一天內上下震盪明顯偏大。新手提醒：高波動股票不適合只看短線漲跌做決策。",
            swingPercent
        );
        return new RuleEvaluation("單日大幅震盪", detail, baseScore);
    }

    private int breakCostBaseScore(BigDecimal lossPercent) {
        if (lossPercent.compareTo(BigDecimal.valueOf(15)) >= 0) {
            return 30;
        }
        if (lossPercent.compareTo(BigDecimal.valueOf(12)) >= 0) {
            return 25;
        }
        if (lossPercent.compareTo(BigDecimal.valueOf(8)) >= 0) {
            return 20;
        }
        if (lossPercent.compareTo(BigDecimal.valueOf(5)) >= 0) {
            return 15;
        }
        if (lossPercent.compareTo(BigDecimal.valueOf(3)) >= 0) {
            return 10;
        }
        return 0;
    }

    private int applyWeightedScore(int baseScore, BigDecimal scoreWeight) {
        BigDecimal weight = scoreWeight == null ? BigDecimal.ONE : scoreWeight;
        return BigDecimal.valueOf(baseScore)
            .multiply(weight)
            .setScale(0, RoundingMode.HALF_UP)
            .intValue();
    }

    private String buildSummary(int score,
                                RiskLevelCode riskLevel,
                                RecommendationCode recommendation,
                                List<CalculatedReason> sortedReasons) {
        if (sortedReasons.isEmpty()) {
            return String.format(
                "目前風險分數 %d（%s），建議 %s。尚未出現明顯風險訊號，仍需持續追蹤。",
                score, riskLevel.name(), recommendation.name()
            );
        }

        String reasonText = sortedReasons.stream()
            .limit(2)
            .map(CalculatedReason::reasonTitle)
            .collect(Collectors.joining("、"));

        return String.format(
            "目前風險分數 %d（%s），建議 %s。主要風險原因：%s。",
            score, riskLevel.name(), recommendation.name(), reasonText
        );
    }

    private Map<Long, List<PortfolioRiskReason>> loadReasonMap(List<Long> resultIds) {
        if (resultIds.isEmpty()) {
            return Map.of();
        }
        return portfolioRiskReasonRepository.findByRiskResultIdIn(resultIds).stream()
            .collect(Collectors.groupingBy(
                reason -> reason.getRiskResult().getId(),
                Collectors.collectingAndThen(Collectors.toList(), this::sortReasons)
            ));
    }

    private List<PortfolioRiskReason> sortReasons(List<PortfolioRiskReason> reasons) {
        return reasons.stream()
            .sorted(Comparator
                .comparingInt(PortfolioRiskReason::getScoreImpact).reversed()
                .thenComparingInt(r -> RULE_PRIORITY.getOrDefault(r.getRuleCode(), Integer.MAX_VALUE))
                .thenComparing(r -> normalize(r.getRuleCode())))
            .toList();
    }

    private List<CalculatedReason> sortCalculatedReasons(List<CalculatedReason> reasons) {
        return reasons.stream()
            .sorted(Comparator
                .comparingInt(CalculatedReason::scoreImpact).reversed()
                .thenComparingInt(r -> RULE_PRIORITY.getOrDefault(r.ruleCode(), Integer.MAX_VALUE))
                .thenComparing(r -> normalize(r.ruleCode())))
            .toList();
    }

    private PortfolioRiskReason toRiskReasonEntity(PortfolioRiskResult riskResult, CalculatedReason reason) {
        PortfolioRiskReason entity = new PortfolioRiskReason();
        entity.setRiskResult(riskResult);
        entity.setRuleCode(reason.ruleCode());
        entity.setReasonTitle(reason.reasonTitle());
        entity.setReasonDetail(reason.reasonDetail());
        entity.setSeverity(reason.severity());
        entity.setScoreImpact(reason.scoreImpact());
        return entity;
    }

    private PortfolioRiskResultPagedDto toPagedDto(PortfolioRiskResult result, List<PortfolioRiskReason> sortedReasons) {
        PortfolioRiskResultPagedDto dto = new PortfolioRiskResultPagedDto();
        dto.setId(result.getId());
        dto.setPortfolioId(result.getPortfolio().getId());
        dto.setTicker(result.getPortfolio().getStock().getTicker());
        dto.setStockName(result.getPortfolio().getStock().getName());
        dto.setTradeDate(result.getTradeDate());
        dto.setRiskScore(result.getRiskScore());
        dto.setRiskLevel(result.getRiskLevel().name());
        dto.setRecommendation(result.getRecommendation().name());
        dto.setSummary(result.getSummaryText());
        dto.setReasons(sortedReasons.stream().limit(3).map(this::toReasonDto).toList());
        dto.setDisclaimer(DISCLAIMER);
        dto.setCreatedAt(result.getCreatedAt());
        return dto;
    }

    private PortfolioRiskResultDetailDto toDetailDto(PortfolioRiskResult result, List<PortfolioRiskReason> reasons) {
        List<PortfolioRiskReason> sortedReasons = sortReasons(reasons);

        PortfolioRiskResultDetailDto dto = new PortfolioRiskResultDetailDto();
        dto.setId(result.getId());
        dto.setPortfolioId(result.getPortfolio().getId());
        dto.setTicker(result.getPortfolio().getStock().getTicker());
        dto.setStockName(result.getPortfolio().getStock().getName());
        dto.setTradeDate(result.getTradeDate());
        dto.setRiskScore(result.getRiskScore());
        dto.setRiskLevel(result.getRiskLevel().name());
        dto.setRecommendation(result.getRecommendation().name());
        dto.setSummary(result.getSummaryText());
        dto.setReasons(sortedReasons.stream().limit(3).map(this::toReasonDto).toList());
        dto.setRiskReasons(sortedReasons.stream().map(this::toReasonDto).toList());
        dto.setDisclaimer(DISCLAIMER);
        dto.setCreatedAt(result.getCreatedAt());
        return dto;
    }

    private PortfolioRiskReasonDto toReasonDto(PortfolioRiskReason reason) {
        PortfolioRiskReasonDto dto = new PortfolioRiskReasonDto();
        dto.setRuleCode(reason.getRuleCode());
        dto.setReasonTitle(reason.getReasonTitle());
        dto.setReasonDetail(reason.getReasonDetail());
        dto.setSeverity(reason.getSeverity());
        dto.setScoreImpact(reason.getScoreImpact());
        return dto;
    }

    private RiskLevelCode parseRiskLevel(String riskLevel) {
        if (!StringUtils.hasText(riskLevel)) {
            return null;
        }
        try {
            return RiskLevelCode.valueOf(riskLevel.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("riskLevel 無效，請使用 LOW/MEDIUM/HIGH/CRITICAL");
        }
    }

    private RecommendationCode parseRecommendation(String recommendation) {
        if (!StringUtils.hasText(recommendation)) {
            return null;
        }
        try {
            return RecommendationCode.valueOf(recommendation.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("recommendation 無效，請使用 HOLD/WATCH/REDUCE/STOP_LOSS_CHECK");
        }
    }

    private BigDecimal resolveLatestDropPercent(List<StockPriceDaily> historyDesc) {
        StockPriceDaily latest = historyDesc.get(0);
        if (latest.getChangePercent() != null) {
            return latest.getChangePercent();
        }

        if (historyDesc.size() < 2) {
            return null;
        }

        BigDecimal prevClose = historyDesc.get(1).getClosePrice();
        if (prevClose == null || prevClose.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        return latest.getClosePrice().subtract(prevClose)
            .divide(prevClose, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isStrictlyLower(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) < 0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String resolveCurrentUserUid() {
        return investCurrentUserService.resolveCurrentUserUid();
    }

    private record RuleEvaluation(String title, String detail, int baseImpact) {}

    private record CalculatedReason(String ruleCode, String reasonTitle, String reasonDetail, String severity, int scoreImpact) {}
}
