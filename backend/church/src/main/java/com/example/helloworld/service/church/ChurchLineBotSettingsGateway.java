package com.example.helloworld.service.church;

import com.example.helloworld.service.common.LineBotSettingsGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ChurchLineBotSettingsGateway implements LineBotSettingsGateway {
    private final SystemSettingService churchSystemSettingService;

    public ChurchLineBotSettingsGateway(
            @Qualifier("churchSystemSettingService") SystemSettingService churchSystemSettingService) {
        this.churchSystemSettingService = churchSystemSettingService;
    }

    @Override
    public String getChannelToken() {
        return churchSystemSettingService.getSettingValue("line.bot.channel-token", null);
    }

    @Override
    public String getChannelSecret() {
        return churchSystemSettingService.getSettingValue("line.bot.channel-secret", null);
    }

    @Override
    public String getWebhookUrl() {
        String url = churchSystemSettingService.getSettingValue("line.bot.webhook-url", null);
        return url != null ? url : "https://power-light-church.duckdns.org/api/church/line/webhook";
    }

    @Override
    public String getAdminUserId() {
        return churchSystemSettingService.getSettingValue("line.bot.admin-user-id", null);
    }
}
