package com.example.helloworld.scheduler.invest;

import com.example.helloworld.service.invest.DailyReportBatchService;
import com.example.helloworld.service.invest.systemscheduler.SystemJobCode;
import com.example.helloworld.service.invest.systemscheduler.SystemSchedulerRuntimeGateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@ConditionalOnProperty(prefix = "invest.scheduler.daily-report", name = "enabled", havingValue = "true")
public class InvestDailyBatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(InvestDailyBatchScheduler.class);
    private final DailyReportBatchService dailyReportBatchService;
    private final SystemSchedulerRuntimeGateService schedulerRuntimeGateService;

    public InvestDailyBatchScheduler(DailyReportBatchService dailyReportBatchService,
                                     SystemSchedulerRuntimeGateService schedulerRuntimeGateService) {
        this.dailyReportBatchService = dailyReportBatchService;
        this.schedulerRuntimeGateService = schedulerRuntimeGateService;
    }

    @Scheduled(
        cron = "${invest.scheduler.daily-report.cron:0 10 18 * * *}",
        zone = "${invest.scheduler.daily-report.zone:Asia/Taipei}"
    )
    public void runDailyPortfolioRiskReport() {
        if (!schedulerRuntimeGateService.isEnabled(SystemJobCode.DAILY_PORTFOLIO_RISK_REPORT)) {
            log.info("略過 Invest 每日報告排程：任務已停用");
            return;
        }
        LocalDate reportDate = LocalDate.now(ZoneId.of("Asia/Taipei"));
        log.info("開始執行 Invest 每日報告排程，reportDate={}", reportDate);
        dailyReportBatchService.runForAllActiveUsers(reportDate);
        log.info("完成執行 Invest 每日報告排程，reportDate={}", reportDate);
    }
}
