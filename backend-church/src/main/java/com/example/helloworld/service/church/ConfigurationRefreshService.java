package com.example.helloworld.service.church;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Church 系統配置刷新服務
 * 從 church 資料庫讀取配置並提供動態刷新功能
 */
@Service("churchConfigurationRefreshService")
public class ConfigurationRefreshService {

    @Autowired
    @Qualifier("churchSystemSettingService")
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
        // 注意：channel-token、channel-secret、webhook-url、admin-user-id 現在統一從個人網站資料庫讀取
        // 教會資料庫不再需要這些配置
        // getConfigValue("line.bot.daily-reminder-enabled", "true");
        // getConfigValue("line.bot.daily-reminder-time", "20:00");
        
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

