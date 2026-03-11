package com.example.helloworld.config;

import com.example.helloworld.service.common.LineBotSettingsGateway;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChurchLineBotConfig {
    private final LineBotSettingsGateway lineBotSettingsGateway;

    public ChurchLineBotConfig(LineBotSettingsGateway lineBotSettingsGateway) {
        this.lineBotSettingsGateway = lineBotSettingsGateway;
    }

    // ========== Church 系統配置（從 church.system_settings 讀取）==========
    
    /**
     * 獲取 Church 系統的 Channel Token
     */
    public String getChannelToken() {
        return lineBotSettingsGateway.getChannelToken();
    }

    /**
     * 獲取 Church 系統的 Channel Secret
     */
    public String getChannelSecret() {
        return lineBotSettingsGateway.getChannelSecret();
    }

    /**
     * 獲取 Church 系統的 Webhook URL
     */
    public String getWebhookUrl() {
        return lineBotSettingsGateway.getWebhookUrl();
    }


    /**
     * 獲取 Church 系統的管理員用戶 ID
     */
    public String getAdminUserId() {
        return lineBotSettingsGateway.getAdminUserId();
    }

}
