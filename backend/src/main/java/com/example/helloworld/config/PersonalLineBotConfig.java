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

    public boolean isDailyReminderEnabled() {
        return systemSettingService.getSettingValueAsBoolean("line.bot.daily-reminder-enabled", false);
    }

    public String getDailyReminderTime() {
        return systemSettingService.getSettingValue("line.bot.daily-reminder-time", "20:00");
    }

    public String getAdminUserId() {
        return systemSettingService.getSettingValue("line.bot.admin-user-id", null);
    }

    /**
     * 獲取每日提醒的 Cron 表達式
     */
    public String getDailyReminderCron() {
        String dailyReminderTime = getDailyReminderTime();
        if (dailyReminderTime != null && !dailyReminderTime.trim().isEmpty()) {
            try {
                String[] parts = dailyReminderTime.split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);
                return String.format("0 %d %d * * ?", minute, hour);
            } catch (Exception e) {
                return "0 0 20 * * ?";
            }
        }
        return "0 0 20 * * ?";
    }
}

