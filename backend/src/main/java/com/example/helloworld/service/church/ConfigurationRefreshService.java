package com.example.helloworld.service.church;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置刷新服務
 * 從資料庫讀取配置並提供動態刷新功能
 */
@Service
public class ConfigurationRefreshService {

    @Autowired
    private SystemSettingService systemSettingService;

    // 配置緩存
    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    /**
     * 獲取配置值（從緩存或資料庫）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public String getConfigValue(String key, String defaultValue) {
        // 先從緩存讀取
        if (configCache.containsKey(key)) {
            return configCache.get(key);
        }
        
        // 緩存未命中，從資料庫讀取
        String value = systemSettingService.getSettingValue(key, defaultValue);
        configCache.put(key, value);
        return value;
    }

    /**
     * 獲取整數配置值
     */
    public int getConfigValueAsInt(String key, int defaultValue) {
        String value = getConfigValue(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 獲取布林配置值
     */
    public boolean getConfigValueAsBoolean(String key, boolean defaultValue) {
        String value = getConfigValue(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    /**
     * 獲取長整數配置值
     */
    public long getConfigValueAsLong(String key, long defaultValue) {
        String value = getConfigValue(key, String.valueOf(defaultValue));
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 刷新配置緩存（從資料庫重新讀取）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public void refreshConfig() {
        configCache.clear();
        // 預先載入常用配置
        loadCommonConfigs();
    }

    /**
     * 刷新特定配置
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public void refreshConfig(String key) {
        String value = systemSettingService.getSettingValue(key, null);
        if (value != null) {
            configCache.put(key, value);
        } else {
            configCache.remove(key);
        }
    }

    /**
     * 預先載入常用配置
     */
    private void loadCommonConfigs() {
        // LINE Bot 配置
        getConfigValue("line.bot.channel-token", "");
        getConfigValue("line.bot.channel-secret", "");
        getConfigValue("line.bot.webhook-url", "https://power-light-church.duckdns.org/api/line/webhook");
        getConfigValue("line.bot.daily-reminder-enabled", "true");
        getConfigValue("line.bot.daily-reminder-time", "20:00");
        getConfigValue("line.bot.admin-user-id", "");
        getConfigValue("line.bot.church-group-id", "");
        
        // JWT 配置
        getConfigValue("jwt.secret", "F/cPluFKK3/44X5iX9GdY6P7Ye+BIDdBTw6uljBTl9o=");
        getConfigValue("jwt.access-token-expiration", "3600000");
        getConfigValue("jwt.refresh-token-enabled", "true");
        getConfigValue("jwt.refresh-token-expiration", "604800000");
    }

    /**
     * 初始化配置（應用啟動時調用）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public void initializeConfig() {
        refreshConfig();
    }
}

