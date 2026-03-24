package com.example.helloworld.service.invest.system;

import com.example.helloworld.entity.invest.SystemSetting;
import com.example.helloworld.repository.invest.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class InvestSystemSettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    public InvestSystemSettingService(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<SystemSetting> getAllSettings() {
        return systemSettingRepository.findAllOrderByCategoryAndKey();
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<SystemSetting> getSettingsByCategory(String category) {
        return systemSettingRepository.findByCategory(category);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Optional<SystemSetting> getSettingByKey(String key) {
        return systemSettingRepository.findBySettingKey(key);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public String getSettingValue(String key, String defaultValue) {
        if (configCache.containsKey(key)) {
            return configCache.get(key);
        }

        String value = systemSettingRepository.findBySettingKey(key)
            .map(SystemSetting::getSettingValue)
            .orElse(defaultValue);
        if (value != null) {
            configCache.put(key, value);
        }
        return value;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public int getSettingValueAsInt(String key, int defaultValue) {
        String value = getSettingValue(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public boolean getSettingValueAsBoolean(String key, boolean defaultValue) {
        String value = getSettingValue(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    public SystemSetting updateSetting(String key, SystemSetting settingUpdate) {
        SystemSetting existing = systemSettingRepository.findBySettingKey(key)
            .orElseThrow(() -> new RuntimeException("系統設定不存在: " + key));

        if (!Boolean.TRUE.equals(existing.getIsEditable())) {
            throw new RuntimeException("此設定不可編輯: " + key);
        }

        if (settingUpdate.getSettingValue() != null) {
            existing.setSettingValue(settingUpdate.getSettingValue());
        }
        if (settingUpdate.getDescription() != null) {
            existing.setDescription(settingUpdate.getDescription());
        }
        if (settingUpdate.getSettingType() != null && !settingUpdate.getSettingType().trim().isEmpty()) {
            existing.setSettingType(settingUpdate.getSettingType().trim());
        }

        SystemSetting updated = systemSettingRepository.save(existing);
        refreshConfig(key);
        return updated;
    }

    public SystemSetting createSetting(SystemSetting setting) {
        if (setting.getSettingKey() == null || setting.getSettingKey().trim().isEmpty()) {
            throw new RuntimeException("settingKey 為必填");
        }

        String settingKey = setting.getSettingKey().trim();
        if (systemSettingRepository.existsBySettingKey(settingKey)) {
            throw new RuntimeException("系統設定已存在: " + settingKey);
        }

        setting.setSettingKey(settingKey);
        if (setting.getSettingType() == null || setting.getSettingType().trim().isEmpty()) {
            setting.setSettingType("string");
        }
        if (setting.getCategory() == null || setting.getCategory().trim().isEmpty()) {
            setting.setCategory(resolveCategory(settingKey));
        }
        if (setting.getIsEditable() == null) {
            setting.setIsEditable(true);
        }

        SystemSetting saved = systemSettingRepository.save(setting);
        refreshConfig(settingKey);
        return saved;
    }

    public void refreshConfig() {
        configCache.clear();
    }

    public void refreshConfig(String key) {
        String value = systemSettingRepository.findBySettingKey(key)
            .map(SystemSetting::getSettingValue)
            .orElse(null);
        if (value == null) {
            configCache.remove(key);
        } else {
            configCache.put(key, value);
        }
    }

    private String resolveCategory(String settingKey) {
        if (settingKey.startsWith("backup.")) {
            return "backup";
        }
        if (settingKey.startsWith("line.bot.")) {
            return "line.bot";
        }
        if (settingKey.startsWith("scheduler.")) {
            return "scheduler";
        }
        if (settingKey.startsWith("invest.analysis.")) {
            return "invest.analysis";
        }
        if (settingKey.startsWith("invest.strategy.")) {
            return "invest.strategy";
        }
        return "system";
    }
}
