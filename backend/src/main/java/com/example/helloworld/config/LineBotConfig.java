package com.example.helloworld.config;

import com.example.helloworld.service.church.ConfigurationRefreshService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LineBotConfig {

    @Autowired
    private ConfigurationRefreshService configurationRefreshService;

    // Getters for configuration values（從資料庫動態讀取）
    public String getChannelToken() {
        return configurationRefreshService.getConfigValue("line.bot.channel-token", "");
    }

    public String getChannelSecret() {
        return configurationRefreshService.getConfigValue("line.bot.channel-secret", "");
    }

    public String getWebhookUrl() {
        return configurationRefreshService.getConfigValue("line.bot.webhook-url", "https://wc-project.duckdns.org/api/line/webhook");
    }

    public boolean isDailyReminderEnabled() {
        return configurationRefreshService.getConfigValueAsBoolean("line.bot.daily-reminder-enabled", true);
    }

    public String getDailyReminderTime() {
        return configurationRefreshService.getConfigValue("line.bot.daily-reminder-time", "20:00");
    }

    public String getAdminUserId() {
        return configurationRefreshService.getConfigValue("line.bot.admin-user-id", "");
    }

    public String getChurchGroupId() {
        return configurationRefreshService.getConfigValue("line.bot.church-group-id", "");
    }

    /**
     * 獲取每日提醒的 Cron 表達式
     * 如果設定了具體時間，則轉換為對應的 Cron；否則使用預設值
     */
    public String getDailyReminderCron() {
        String dailyReminderTime = getDailyReminderTime();
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
