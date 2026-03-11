package com.example.helloworld.config;

import com.example.helloworld.service.common.LineBotSettingsGateway;
import org.springframework.stereotype.Component;

@Component
public class PersonalLineBotConfig {
    private final LineBotSettingsGateway lineBotSettingsGateway;

    public PersonalLineBotConfig(LineBotSettingsGateway lineBotSettingsGateway) {
        this.lineBotSettingsGateway = lineBotSettingsGateway;
    }

    // Getters for configuration values（從個人網站資料庫讀取）
    public String getChannelToken() {
        return lineBotSettingsGateway.getChannelToken();
    }

    public String getChannelSecret() {
        return lineBotSettingsGateway.getChannelSecret();
    }

    public String getWebhookUrl() {
        return lineBotSettingsGateway.getWebhookUrl();
    }


    public String getAdminUserId() {
        return lineBotSettingsGateway.getAdminUserId();
    }

}
