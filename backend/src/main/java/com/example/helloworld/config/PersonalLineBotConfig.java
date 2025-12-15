package com.example.helloworld.config;

import com.example.helloworld.service.personal.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class PersonalLineBotConfig {

    @Autowired
    @Qualifier("personalSystemSettingService")
    private SystemSettingService systemSettingService;

    // Getters for configuration values（從個人網站資料庫讀取）
    public String getChannelToken() {
        return systemSettingService.getSettingValue("line.bot.channel-token", null);
    }

    public String getChannelSecret() {
        return systemSettingService.getSettingValue("line.bot.channel-secret", null);
    }

    public String getWebhookUrl() {
        String url = systemSettingService.getSettingValue("line.bot.webhook-url", null);
        return url != null ? url : "https://power-light-church.duckdns.org/api/personal/line/webhook";
    }


    public String getAdminUserId() {
        return systemSettingService.getSettingValue("line.bot.admin-user-id", null);
    }

}

