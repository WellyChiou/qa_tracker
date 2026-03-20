package com.example.helloworld.scheduler.invest;

import com.example.helloworld.service.invest.PortfolioAlertPollingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "invest.scheduler.alert-polling", name = "enabled", havingValue = "true")
public class InvestAlertPollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(InvestAlertPollingScheduler.class);
    private final PortfolioAlertPollingService portfolioAlertPollingService;

    public InvestAlertPollingScheduler(PortfolioAlertPollingService portfolioAlertPollingService) {
        this.portfolioAlertPollingService = portfolioAlertPollingService;
    }

    @Scheduled(
        fixedDelayString = "${invest.scheduler.alert-polling.fixed-delay-ms:120000}",
        initialDelayString = "${invest.scheduler.alert-polling.initial-delay-ms:30000}"
    )
    public void runAlertPolling() {
        log.info("開始執行 Invest 警示輪詢排程");
        portfolioAlertPollingService.runForAllActiveUsers();
        log.info("完成執行 Invest 警示輪詢排程");
    }
}
