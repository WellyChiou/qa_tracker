package com.example.helloworld.scheduler.invest;

import com.example.helloworld.service.invest.DatabaseBackupExecutionService;
import com.example.helloworld.service.invest.systemscheduler.SystemJobCode;
import com.example.helloworld.service.invest.systemscheduler.SystemSchedulerRuntimeGateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "invest.scheduler.database-backup",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class InvestDatabaseBackupScheduler {

    private static final Logger log = LoggerFactory.getLogger(InvestDatabaseBackupScheduler.class);

    private final DatabaseBackupExecutionService databaseBackupExecutionService;
    private final SystemSchedulerRuntimeGateService schedulerRuntimeGateService;

    public InvestDatabaseBackupScheduler(DatabaseBackupExecutionService databaseBackupExecutionService,
                                         SystemSchedulerRuntimeGateService schedulerRuntimeGateService) {
        this.databaseBackupExecutionService = databaseBackupExecutionService;
        this.schedulerRuntimeGateService = schedulerRuntimeGateService;
    }

    @Scheduled(
        cron = "${invest.scheduler.database-backup.cron:0 0 2 * * *}",
        zone = "${invest.scheduler.database-backup.zone:Asia/Taipei}"
    )
    public void runDatabaseBackup() {
        if (!schedulerRuntimeGateService.isEnabled(SystemJobCode.DATABASE_BACKUP)) {
            log.info("略過 Invest 資料庫備份排程：任務已停用");
            return;
        }

        log.info("開始執行 Invest 資料庫備份排程");
        DatabaseBackupExecutionService.ExecutionResult result =
            databaseBackupExecutionService.runWithLog("排程觸發（DATABASE_BACKUP）");
        log.info("完成 Invest 資料庫備份排程，status={}, logId={}, message={}",
            result.status(), result.logId(), result.message());
    }
}
