package com.example.helloworld.service.church.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChurchAuthorizationSeedRunner {
    private static final Logger log = LoggerFactory.getLogger(ChurchAuthorizationSeedRunner.class);

    private final List<ChurchAuthorizationSeedModule> modules;

    public ChurchAuthorizationSeedRunner(List<ChurchAuthorizationSeedModule> modules) {
        this.modules = modules;
    }

    @Transactional(transactionManager = "churchTransactionManager")
    public void seedAll() {
        for (ChurchAuthorizationSeedModule module : modules) {
            log.info("🔐 初始化 Church 權限模組: {}", module.moduleName());
            module.seed();
        }
    }
}
