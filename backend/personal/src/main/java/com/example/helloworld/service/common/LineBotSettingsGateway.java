package com.example.helloworld.service.common;

public interface LineBotSettingsGateway {
    String getChannelToken();

    String getChannelSecret();

    String getWebhookUrl();

    String getAdminUserId();
}
