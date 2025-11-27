package com.example.helloworld.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LineBotConfig {

    @Value("${line.bot.channel-token:}")
    private String channelToken;

    @Value("${line.bot.channel-secret:}")
    private String channelSecret;

    @Value("${line.bot.webhook-url:http://localhost:8080/api/line/webhook}")
    private String webhookUrl;

    @Value("${line.bot.daily-reminder-enabled:true}")
    private boolean dailyReminderEnabled;

    @Value("${line.bot.daily-reminder-time:20:00}")
    private String dailyReminderTime;

    @Value("${line.bot.admin-user-id:}")
    private String adminUserId;

    // Getters for configuration values
    public String getChannelToken() {
        return channelToken;
    }

    public String getChannelSecret() {
        return channelSecret;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public boolean isDailyReminderEnabled() {
        return dailyReminderEnabled;
    }

    public String getDailyReminderTime() {
        return dailyReminderTime;
    }

    public String getAdminUserId() {
        return adminUserId;
    }
}
