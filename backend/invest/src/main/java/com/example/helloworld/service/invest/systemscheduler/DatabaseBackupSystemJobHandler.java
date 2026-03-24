package com.example.helloworld.service.invest.systemscheduler;

import com.example.helloworld.dto.invest.SystemSchedulerJobDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobLogPagedDto;
import com.example.helloworld.dto.invest.SystemSchedulerRunNowResponseDto;
import com.example.helloworld.entity.invest.SchedulerJobLog;
import com.example.helloworld.repository.invest.SchedulerJobLogRepository;
import com.example.helloworld.service.invest.DatabaseBackupExecutionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Transactional(transactionManager = "investTransactionManager")
public class DatabaseBackupSystemJobHandler implements SystemJobHandler {

    private final DatabaseBackupExecutionService databaseBackupExecutionService;
    private final SchedulerJobLogRepository schedulerJobLogRepository;

    @Value("${invest.scheduler.database-backup.enabled:true}")
    private boolean schedulerEnabled;

    @Value("${invest.scheduler.database-backup.cron:0 0 2 * * *}")
    private String cron;

    @Value("${invest.scheduler.database-backup.zone:Asia/Taipei}")
    private String zone;

    public DatabaseBackupSystemJobHandler(DatabaseBackupExecutionService databaseBackupExecutionService,
                                          SchedulerJobLogRepository schedulerJobLogRepository) {
        this.databaseBackupExecutionService = databaseBackupExecutionService;
        this.schedulerJobLogRepository = schedulerJobLogRepository;
    }

    @Override
    public SystemJobCode getJobCode() {
        return SystemJobCode.DATABASE_BACKUP;
    }

    @Override
    public int getDisplayOrder() {
        return 18;
    }

    @Override
    public SystemSchedulerJobDto buildJob() {
        SystemSchedulerJobDto dto = new SystemSchedulerJobDto();
        dto.setJobCode(getJobCode().name());
        dto.setJobName(DatabaseBackupExecutionService.JOB_NAME_DATABASE_BACKUP);
        dto.setDescription("資料庫備份排程。執行既有 InvestBackupService/createBackup 流程。");
        dto.setEnabled(schedulerEnabled);
        dto.setScheduleType("CRON");
        dto.setScheduleExpression(String.format("%s (%s)", cron, zone));
        dto.setLogSource("scheduler_job_log");
        return dto;
    }

    @Override
    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public SystemSchedulerJobLogPagedDto getLatestLog() {
        return schedulerJobLogRepository
            .findTopByJobNameOrderByStartedAtDesc(DatabaseBackupExecutionService.JOB_NAME_DATABASE_BACKUP)
            .map(this::toLogDto)
            .orElse(null);
    }

    @Override
    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<SystemSchedulerJobLogPagedDto> getLogs(int page, int size) {
        return schedulerJobLogRepository
            .findByJobNameOrderByStartedAtDesc(
                DatabaseBackupExecutionService.JOB_NAME_DATABASE_BACKUP,
                PageRequest.of(page, size)
            )
            .map(this::toLogDto);
    }

    @Override
    public SystemSchedulerRunNowResponseDto runNow() {
        DatabaseBackupExecutionService.ExecutionResult result = databaseBackupExecutionService
            .runWithLog("手動觸發（系統排程管理 Run Now）");

        SystemSchedulerRunNowResponseDto dto = new SystemSchedulerRunNowResponseDto();
        dto.setJobCode(getJobCode().name());
        dto.setStatus(result.status());
        dto.setMessage(result.message());
        dto.setTriggeredAt(LocalDateTime.now());
        dto.setReferenceLogId(result.logId());
        return dto;
    }

    private SystemSchedulerJobLogPagedDto toLogDto(SchedulerJobLog log) {
        SystemSchedulerJobLogPagedDto dto = new SystemSchedulerJobLogPagedDto();
        dto.setId(log.getId());
        dto.setJobCode(getJobCode().name());
        dto.setStatus(log.getStatus() == null ? null : log.getStatus().name());
        dto.setRunDate(log.getRunDate());
        dto.setStartedAt(log.getStartedAt());
        dto.setFinishedAt(log.getFinishedAt());
        dto.setMessage(log.getMessage());
        dto.setSourceTable("scheduler_job_log");
        return dto;
    }
}
