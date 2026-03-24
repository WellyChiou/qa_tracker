package com.example.helloworld.service.invest.system;

import com.example.helloworld.dto.invest.StrategySettingsDto;
import com.example.helloworld.dto.invest.StrategySettingsUpdateRequestDto;
import com.example.helloworld.entity.invest.SystemSetting;
import com.example.helloworld.repository.invest.SystemSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class InvestStrategySettingService {

    private static final Logger log = LoggerFactory.getLogger(InvestStrategySettingService.class);

    public static final String KEY_VERSION = "invest.strategy.version";

    public static final String KEY_STRENGTH_STRONG_MIN = "invest.strategy.strength.strong_min";
    public static final String KEY_STRENGTH_GOOD_MIN = "invest.strategy.strength.good_min";
    public static final String KEY_STRENGTH_WEAK_MAX = "invest.strategy.strength.weak_max";

    public static final String KEY_DATA_QUALITY_MIN_HISTORY_DAYS = "invest.strategy.data_quality.min_history_days";
    public static final String KEY_DATA_QUALITY_STALE_DAYS = "invest.strategy.data_quality.stale_days";

    public static final String KEY_OPPORTUNITY_OBSERVE_MIN_SCORE = "invest.strategy.opportunity.observe_min_score";
    public static final String KEY_OPPORTUNITY_WAIT_PULLBACK_MIN_SCORE = "invest.strategy.opportunity.wait_pullback_min_score";
    public static final String KEY_OPPORTUNITY_REEVALUATE_MIN_SCORE = "invest.strategy.opportunity.reevaluate_min_score";

    // 必須與 seed-baseline/20_seed_phase3_strategy_settings_acl.sql 保持一致
    public static final int DEFAULT_STRATEGY_VERSION = 1;
    public static final int DEFAULT_STRENGTH_STRONG_MIN = 80;
    public static final int DEFAULT_STRENGTH_GOOD_MIN = 60;
    public static final int DEFAULT_STRENGTH_WEAK_MAX = 39;
    public static final int DEFAULT_DATA_QUALITY_MIN_HISTORY_DAYS = 20;
    public static final int DEFAULT_DATA_QUALITY_STALE_DAYS = 3;
    public static final int DEFAULT_OPPORTUNITY_OBSERVE_MIN_SCORE = 80;
    public static final int DEFAULT_OPPORTUNITY_WAIT_PULLBACK_MIN_SCORE = 65;
    public static final int DEFAULT_OPPORTUNITY_REEVALUATE_MIN_SCORE = 45;

    private static final List<String> STRATEGY_SETTING_KEYS = List.of(
        KEY_VERSION,
        KEY_STRENGTH_STRONG_MIN,
        KEY_STRENGTH_GOOD_MIN,
        KEY_STRENGTH_WEAK_MAX,
        KEY_DATA_QUALITY_MIN_HISTORY_DAYS,
        KEY_DATA_QUALITY_STALE_DAYS,
        KEY_OPPORTUNITY_OBSERVE_MIN_SCORE,
        KEY_OPPORTUNITY_WAIT_PULLBACK_MIN_SCORE,
        KEY_OPPORTUNITY_REEVALUATE_MIN_SCORE
    );

    private static final Set<String> READ_ONLY_KEYS = Set.of(KEY_VERSION);

    private final SystemSettingRepository systemSettingRepository;

    public InvestStrategySettingService(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public StrategySettingsDto getSettings() {
        Map<String, SystemSetting> settings = loadRequiredSettings();
        return toDto(settings);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public int getCurrentStrategyVersion() {
        Map<String, SystemSetting> settings = loadRequiredSettings();
        return parseInt(settings.get(KEY_VERSION).getSettingValue(), KEY_VERSION);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public ResolvedThresholds getResolvedThresholds() {
        List<SystemSetting> settingList = systemSettingRepository.findBySettingKeyIn(STRATEGY_SETTING_KEYS);
        Map<String, SystemSetting> settings = new LinkedHashMap<>();
        for (SystemSetting setting : settingList) {
            settings.put(setting.getSettingKey(), setting);
        }

        int strategyVersion = parseIntOrDefault(settings.get(KEY_VERSION), KEY_VERSION, DEFAULT_STRATEGY_VERSION, 1, Integer.MAX_VALUE);

        int strongMin = parseIntOrDefault(settings.get(KEY_STRENGTH_STRONG_MIN), KEY_STRENGTH_STRONG_MIN, DEFAULT_STRENGTH_STRONG_MIN, 0, 100);
        int goodMin = parseIntOrDefault(settings.get(KEY_STRENGTH_GOOD_MIN), KEY_STRENGTH_GOOD_MIN, DEFAULT_STRENGTH_GOOD_MIN, 0, 100);
        int weakMax = parseIntOrDefault(settings.get(KEY_STRENGTH_WEAK_MAX), KEY_STRENGTH_WEAK_MAX, DEFAULT_STRENGTH_WEAK_MAX, 0, 100);
        if (!(strongMin > goodMin && weakMax < goodMin)) {
            log.warn("策略 Strength 門檻設定不合法，使用預設值。strongMin={}, goodMin={}, weakMax={}", strongMin, goodMin, weakMax);
            strongMin = DEFAULT_STRENGTH_STRONG_MIN;
            goodMin = DEFAULT_STRENGTH_GOOD_MIN;
            weakMax = DEFAULT_STRENGTH_WEAK_MAX;
        }

        int minHistoryDays = parseIntOrDefault(
            settings.get(KEY_DATA_QUALITY_MIN_HISTORY_DAYS),
            KEY_DATA_QUALITY_MIN_HISTORY_DAYS,
            DEFAULT_DATA_QUALITY_MIN_HISTORY_DAYS,
            5,
            120
        );
        int staleDays = parseIntOrDefault(
            settings.get(KEY_DATA_QUALITY_STALE_DAYS),
            KEY_DATA_QUALITY_STALE_DAYS,
            DEFAULT_DATA_QUALITY_STALE_DAYS,
            1,
            30
        );

        int observeMinScore = parseIntOrDefault(
            settings.get(KEY_OPPORTUNITY_OBSERVE_MIN_SCORE),
            KEY_OPPORTUNITY_OBSERVE_MIN_SCORE,
            DEFAULT_OPPORTUNITY_OBSERVE_MIN_SCORE,
            0,
            100
        );
        int waitPullbackMinScore = parseIntOrDefault(
            settings.get(KEY_OPPORTUNITY_WAIT_PULLBACK_MIN_SCORE),
            KEY_OPPORTUNITY_WAIT_PULLBACK_MIN_SCORE,
            DEFAULT_OPPORTUNITY_WAIT_PULLBACK_MIN_SCORE,
            0,
            100
        );
        int reevaluateMinScore = parseIntOrDefault(
            settings.get(KEY_OPPORTUNITY_REEVALUATE_MIN_SCORE),
            KEY_OPPORTUNITY_REEVALUATE_MIN_SCORE,
            DEFAULT_OPPORTUNITY_REEVALUATE_MIN_SCORE,
            0,
            100
        );

        if (!(observeMinScore > waitPullbackMinScore && waitPullbackMinScore > reevaluateMinScore)) {
            log.warn(
                "策略 Opportunity 門檻設定不合法，使用預設值。observeMinScore={}, waitPullbackMinScore={}, reevaluateMinScore={}",
                observeMinScore,
                waitPullbackMinScore,
                reevaluateMinScore
            );
            observeMinScore = DEFAULT_OPPORTUNITY_OBSERVE_MIN_SCORE;
            waitPullbackMinScore = DEFAULT_OPPORTUNITY_WAIT_PULLBACK_MIN_SCORE;
            reevaluateMinScore = DEFAULT_OPPORTUNITY_REEVALUATE_MIN_SCORE;
        }

        return new ResolvedThresholds(
            strategyVersion,
            new StrengthThresholds(strongMin, goodMin, weakMax),
            new DataQualityThresholds(minHistoryDays, staleDays),
            new OpportunityThresholds(observeMinScore, waitPullbackMinScore, reevaluateMinScore)
        );
    }

    public StrategySettingsDto updateSettings(StrategySettingsUpdateRequestDto request) {
        Map<String, SystemSetting> settings = loadRequiredSettings();
        NormalizedStrategyValues normalized = normalizeAndValidate(request);
        Map<String, String> incomingValues = normalized.toMap();

        List<SystemSetting> changedEntities = new ArrayList<>();
        for (Map.Entry<String, String> entry : incomingValues.entrySet()) {
            String key = entry.getKey();
            if (READ_ONLY_KEYS.contains(key)) {
                continue;
            }

            SystemSetting setting = settings.get(key);
            String current = normalizeValue(setting.getSettingValue());
            String incoming = normalizeValue(entry.getValue());
            if (!Objects.equals(current, incoming)) {
                setting.setSettingValue(incoming);
                changedEntities.add(setting);
            }
        }

        if (!changedEntities.isEmpty()) {
            SystemSetting versionSetting = settings.get(KEY_VERSION);
            int currentVersion = parseInt(versionSetting.getSettingValue(), KEY_VERSION);
            versionSetting.setSettingValue(String.valueOf(currentVersion + 1));
            changedEntities.add(versionSetting);
            systemSettingRepository.saveAll(changedEntities);
            systemSettingRepository.flush();
        }

        return toDto(settings);
    }

    private Map<String, SystemSetting> loadRequiredSettings() {
        List<SystemSetting> settingList = systemSettingRepository.findBySettingKeyIn(STRATEGY_SETTING_KEYS);
        Map<String, SystemSetting> settings = new LinkedHashMap<>();
        for (SystemSetting setting : settingList) {
            settings.put(setting.getSettingKey(), setting);
        }

        List<String> missing = STRATEGY_SETTING_KEYS.stream()
            .filter(key -> !settings.containsKey(key))
            .toList();
        if (!missing.isEmpty()) {
            throw new RuntimeException("缺少策略設定鍵值：" + String.join(", ", missing) + "，請先執行最新 migration/seed。");
        }

        return settings;
    }

    private StrategySettingsDto toDto(Map<String, SystemSetting> settings) {
        StrategySettingsDto dto = new StrategySettingsDto();
        dto.setStrategyVersion(parseInt(settings.get(KEY_VERSION).getSettingValue(), KEY_VERSION));

        StrategySettingsDto.StrengthThresholds strength = new StrategySettingsDto.StrengthThresholds();
        strength.setStrongMin(parseInt(settings.get(KEY_STRENGTH_STRONG_MIN).getSettingValue(), KEY_STRENGTH_STRONG_MIN));
        strength.setGoodMin(parseInt(settings.get(KEY_STRENGTH_GOOD_MIN).getSettingValue(), KEY_STRENGTH_GOOD_MIN));
        strength.setWeakMax(parseInt(settings.get(KEY_STRENGTH_WEAK_MAX).getSettingValue(), KEY_STRENGTH_WEAK_MAX));
        dto.setStrength(strength);

        StrategySettingsDto.DataQualityThresholds dataQuality = new StrategySettingsDto.DataQualityThresholds();
        dataQuality.setMinHistoryDays(parseInt(
            settings.get(KEY_DATA_QUALITY_MIN_HISTORY_DAYS).getSettingValue(),
            KEY_DATA_QUALITY_MIN_HISTORY_DAYS
        ));
        dataQuality.setStaleDays(parseInt(
            settings.get(KEY_DATA_QUALITY_STALE_DAYS).getSettingValue(),
            KEY_DATA_QUALITY_STALE_DAYS
        ));
        dto.setDataQuality(dataQuality);

        StrategySettingsDto.OpportunityThresholds opportunity = new StrategySettingsDto.OpportunityThresholds();
        opportunity.setObserveMinScore(parseInt(
            settings.get(KEY_OPPORTUNITY_OBSERVE_MIN_SCORE).getSettingValue(),
            KEY_OPPORTUNITY_OBSERVE_MIN_SCORE
        ));
        opportunity.setWaitPullbackMinScore(parseInt(
            settings.get(KEY_OPPORTUNITY_WAIT_PULLBACK_MIN_SCORE).getSettingValue(),
            KEY_OPPORTUNITY_WAIT_PULLBACK_MIN_SCORE
        ));
        opportunity.setReevaluateMinScore(parseInt(
            settings.get(KEY_OPPORTUNITY_REEVALUATE_MIN_SCORE).getSettingValue(),
            KEY_OPPORTUNITY_REEVALUATE_MIN_SCORE
        ));
        dto.setOpportunity(opportunity);

        LocalDateTime maxUpdatedAt = systemSettingRepository.findMaxUpdatedAtBySettingKeyIn(STRATEGY_SETTING_KEYS);
        dto.setLastUpdatedAt(maxUpdatedAt);
        return dto;
    }

    private NormalizedStrategyValues normalizeAndValidate(StrategySettingsUpdateRequestDto request) {
        if (request == null || request.getStrength() == null || request.getDataQuality() == null || request.getOpportunity() == null) {
            throw new RuntimeException("策略設定請提供完整 strength/dataQuality/opportunity 欄位。");
        }

        int strongMin = requireRange(request.getStrength().getStrongMin(), KEY_STRENGTH_STRONG_MIN, 0, 100);
        int goodMin = requireRange(request.getStrength().getGoodMin(), KEY_STRENGTH_GOOD_MIN, 0, 100);
        int weakMax = requireRange(request.getStrength().getWeakMax(), KEY_STRENGTH_WEAK_MAX, 0, 100);
        int minHistoryDays = requireRange(request.getDataQuality().getMinHistoryDays(), KEY_DATA_QUALITY_MIN_HISTORY_DAYS, 5, 120);
        int staleDays = requireRange(request.getDataQuality().getStaleDays(), KEY_DATA_QUALITY_STALE_DAYS, 1, 30);
        int observeMinScore = requireRange(request.getOpportunity().getObserveMinScore(), KEY_OPPORTUNITY_OBSERVE_MIN_SCORE, 0, 100);
        int waitPullbackMinScore = requireRange(request.getOpportunity().getWaitPullbackMinScore(), KEY_OPPORTUNITY_WAIT_PULLBACK_MIN_SCORE, 0, 100);
        int reevaluateMinScore = requireRange(request.getOpportunity().getReevaluateMinScore(), KEY_OPPORTUNITY_REEVALUATE_MIN_SCORE, 0, 100);

        if (strongMin <= goodMin) {
            throw new RuntimeException("策略校驗失敗：strong_min 必須大於 good_min。");
        }
        if (weakMax >= goodMin) {
            throw new RuntimeException("策略校驗失敗：weak_max 必須小於 good_min。");
        }
        if (!(observeMinScore > waitPullbackMinScore && waitPullbackMinScore > reevaluateMinScore)) {
            throw new RuntimeException("策略校驗失敗：observe_min_score > wait_pullback_min_score > reevaluate_min_score。");
        }

        return new NormalizedStrategyValues(
            strongMin,
            goodMin,
            weakMax,
            minHistoryDays,
            staleDays,
            observeMinScore,
            waitPullbackMinScore,
            reevaluateMinScore
        );
    }

    private int parseInt(String value, String key) {
        try {
            return Integer.parseInt(normalizeValue(value));
        } catch (Exception ex) {
            throw new RuntimeException("策略設定值非數字：" + key);
        }
    }

    private int parseIntOrDefault(SystemSetting setting,
                                  String key,
                                  int defaultValue,
                                  int min,
                                  int max) {
        if (setting == null || setting.getSettingValue() == null || setting.getSettingValue().isBlank()) {
            log.warn("策略設定缺少 key={}，使用預設值 {}", key, defaultValue);
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(setting.getSettingValue().trim());
            if (parsed < min || parsed > max) {
                log.warn("策略設定 key={} 超出範圍（{}~{}）：{}，使用預設值 {}", key, min, max, parsed, defaultValue);
                return defaultValue;
            }
            return parsed;
        } catch (Exception e) {
            log.warn("策略設定 key={} 解析失敗，使用預設值 {}。原因：{}", key, defaultValue, e.getMessage());
            return defaultValue;
        }
    }

    private int requireRange(Integer value, String key, int min, int max) {
        if (value == null) {
            throw new RuntimeException("策略設定缺少欄位：" + key);
        }
        if (value < min || value > max) {
            throw new RuntimeException("策略設定超出範圍：" + key + "（允許範圍 " + min + "~" + max + "）");
        }
        return value;
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private record NormalizedStrategyValues(
        int strongMin,
        int goodMin,
        int weakMax,
        int minHistoryDays,
        int staleDays,
        int observeMinScore,
        int waitPullbackMinScore,
        int reevaluateMinScore
    ) {
        private Map<String, String> toMap() {
            Map<String, String> values = new LinkedHashMap<>();
            values.put(KEY_STRENGTH_STRONG_MIN, String.valueOf(strongMin));
            values.put(KEY_STRENGTH_GOOD_MIN, String.valueOf(goodMin));
            values.put(KEY_STRENGTH_WEAK_MAX, String.valueOf(weakMax));
            values.put(KEY_DATA_QUALITY_MIN_HISTORY_DAYS, String.valueOf(minHistoryDays));
            values.put(KEY_DATA_QUALITY_STALE_DAYS, String.valueOf(staleDays));
            values.put(KEY_OPPORTUNITY_OBSERVE_MIN_SCORE, String.valueOf(observeMinScore));
            values.put(KEY_OPPORTUNITY_WAIT_PULLBACK_MIN_SCORE, String.valueOf(waitPullbackMinScore));
            values.put(KEY_OPPORTUNITY_REEVALUATE_MIN_SCORE, String.valueOf(reevaluateMinScore));
            return values;
        }
    }

    public record ResolvedThresholds(
        int strategyVersion,
        StrengthThresholds strength,
        DataQualityThresholds dataQuality,
        OpportunityThresholds opportunity
    ) {
    }

    public record StrengthThresholds(
        int strongMin,
        int goodMin,
        int weakMax
    ) {
    }

    public record DataQualityThresholds(
        int minHistoryDays,
        int staleDays
    ) {
    }

    public record OpportunityThresholds(
        int observeMinScore,
        int waitPullbackMinScore,
        int reevaluateMinScore
    ) {
    }
}
