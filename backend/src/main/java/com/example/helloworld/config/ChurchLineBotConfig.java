package com.example.helloworld.config;

import com.example.helloworld.service.church.ConfigurationRefreshService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChurchLineBotConfig {

    @Autowired
    @Qualifier("personalConfigurationRefreshService")
    private com.example.helloworld.service.personal.ConfigurationRefreshService personalConfigurationRefreshService;

    @Autowired
    @Qualifier("churchConfigurationRefreshService")
    private com.example.helloworld.service.church.ConfigurationRefreshService churchConfigurationRefreshService;

    /**
     * 獲取 Personal 系統的配置值
     */
    private String getPersonalConfigValue(String key, String defaultValue) {
        return personalConfigurationRefreshService.getConfigValue(key, defaultValue);
    }

    /**
     * 獲取 Church 系統的配置值
     */
    private String getChurchConfigValue(String key, String defaultValue) {
        return churchConfigurationRefreshService.getConfigValue(key, defaultValue);
    }

    // ========== Personal 系統配置（從 qa_tracker.system_settings 讀取）==========
    
    /**
     * 獲取 Personal 系統的 Channel Token
     */
    public String getChannelToken() {
        return getPersonalConfigValue("line.bot.channel-token", "");
    }

    /**
     * 獲取 Personal 系統的 Channel Secret
     */
    public String getChannelSecret() {
        return getPersonalConfigValue("line.bot.channel-secret", "");
    }

    /**
     * 獲取 Personal 系統的 Webhook URL
     */
    public String getWebhookUrl() {
        return getPersonalConfigValue("line.bot.webhook-url", "https://power-light-church.duckdns.org/api/line/webhook");
    }


    /**
     * 獲取 Personal 系統的管理員用戶 ID
     */
    public String getAdminUserId() {
        return getPersonalConfigValue("line.bot.admin-user-id", "");
    }

    // ========== Church 系統配置（從 church.system_settings 讀取）==========
    
    /**
     * 獲取 Church 系統的群組 ID（用於發送服事人員通知）
     */
    public String getChurchGroupId() {
        return getChurchConfigValue("line.bot.church-group-id", "");
    }

    /**
     * 獲取 Church 系統的群組 ID 列表（未來可能有多個群組）
     * 返回逗號分隔的群組 ID 字串
     */
    public String getChurchGroupIds() {
        return getChurchConfigValue("line.bot.church-group-ids", "");
    }

}
