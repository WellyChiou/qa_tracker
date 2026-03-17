package com.example.helloworld.service.personal;

import com.example.helloworld.service.common.LineBotSettingsGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class PersonalLineBotSettingsGateway implements LineBotSettingsGateway {
    private final SystemSettingService personalSystemSettingService;

    public PersonalLineBotSettingsGateway(
            @Qualifier("personalSystemSettingService") SystemSettingService personalSystemSettingService) {
        this.personalSystemSettingService = personalSystemSettingService;
    }

    @Override
    public String getChannelToken() {
        return personalSystemSettingService.getSettingValue("line.bot.channel-token", null);
    }

    @Override
    public String getChannelSecret() {
        return personalSystemSettingService.getSettingValue("line.bot.channel-secret", null);
    }

    @Override
    public String getWebhookUrl() {
        String url = personalSystemSettingService.getSettingValue("line.bot.webhook-url", null);
        return url != null ? url : "https://power-light-church.duckdns.org/api/personal/line/webhook";
    }

    @Override
    public String getAdminUserId() {
        return personalSystemSettingService.getSettingValue("line.bot.admin-user-id", null);
    }
}
