package com.example.helloworld.scheduler.invest;

import com.example.helloworld.entity.invest.PriceUpdateRunModeCode;
import com.example.helloworld.service.invest.PriceUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "invest.scheduler.price-update", name = "enabled", havingValue = "true")
public class InvestPriceUpdateScheduler {

    private static final Logger log = LoggerFactory.getLogger(InvestPriceUpdateScheduler.class);

    private final PriceUpdateService priceUpdateService;

    public InvestPriceUpdateScheduler(PriceUpdateService priceUpdateService) {
        this.priceUpdateService = priceUpdateService;
    }

    @Scheduled(
        cron = "${invest.scheduler.price-update.tw.cron:0 30 15 * * MON-FRI}",
        zone = "${invest.scheduler.price-update.tw.zone:Asia/Taipei}"
    )
    public void runTwMarketUpdate() {
        log.info("開始執行 Invest 行情排程（TW）");
        PriceUpdateService.SchedulerExecutionResult result = priceUpdateService.runForAllActiveUsers(
            Set.of("TW"),
            PriceUpdateRunModeCode.SCHEDULER_TW,
            "排程觸發（TW）"
        );
        log.info(
            "完成 Invest 行情排程（TW），users={}, jobs={}, total={}, success={}, fail={}",
            result.processedUserCount(),
            result.jobCount(),
            result.totalCount(),
            result.successCount(),
            result.failCount()
        );
    }

    @Scheduled(
        cron = "${invest.scheduler.price-update.us.cron:0 30 6 * * TUE-SAT}",
        zone = "${invest.scheduler.price-update.us.zone:Asia/Taipei}"
    )
    public void runUsMarketUpdate() {
        log.info("開始執行 Invest 行情排程（US）");
        PriceUpdateService.SchedulerExecutionResult result = priceUpdateService.runForAllActiveUsers(
            Set.of("US"),
            PriceUpdateRunModeCode.SCHEDULER_US,
            "排程觸發（US）"
        );
        log.info(
            "完成 Invest 行情排程（US），users={}, jobs={}, total={}, success={}, fail={}",
            result.processedUserCount(),
            result.jobCount(),
            result.totalCount(),
            result.successCount(),
            result.failCount()
        );
    }
}
