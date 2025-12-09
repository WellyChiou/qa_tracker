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

    @Value("${line.bot.church-group-id:}")
    private String churchGroupId;

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

    public String getChurchGroupId() {
        return churchGroupId;
    }

    /**
     * 獲取每日提醒的 Cron 表達式
     * 如果設定了具體時間，則轉換為對應的 Cron；否則使用預設值
     */
    public String getDailyReminderCron() {
        if (dailyReminderTime != null && !dailyReminderTime.trim().isEmpty()) {
            // 將 HH:MM 格式轉換為 Cron 表達式
            try {
                String[] parts = dailyReminderTime.split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);
                return String.format("0 %d %d * * ?", minute, hour);
            } catch (Exception e) {
                // 如果解析失敗，使用預設值
                return "0 0 20 * * ?";
            }
        }
        return "0 0 20 * * ?";
    }
}
