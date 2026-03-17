package com.example.helloworld.service.church.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChurchLineWebhookAuthorizationSeed implements ChurchAuthorizationSeedModule {
    private static final Logger log = LoggerFactory.getLogger(ChurchLineWebhookAuthorizationSeed.class);

    private final ChurchAuthorizationSeedSupport seedSupport;

    public ChurchLineWebhookAuthorizationSeed(ChurchAuthorizationSeedSupport seedSupport) {
        this.seedSupport = seedSupport;
    }

    @Override
    public String moduleName() {
        return "line-webhook";
    }

    @Override
    public void seed() {
        seedSupport.ensureUrlPermission(
                "/api/church/line/webhook",
                "POST",
                null,
                null,
                true,
                275,
                true,
                "Church LINE webhook 公開入口"
        );
        seedSupport.ensureUrlPermission(
                "/api/church/line/test",
                "GET",
                null,
                null,
                true,
                276,
                true,
                "Church LINE webhook 測試入口"
        );

        log.info("✅ 已確認 church LINE webhook 公開 URL 權限");
    }
}
