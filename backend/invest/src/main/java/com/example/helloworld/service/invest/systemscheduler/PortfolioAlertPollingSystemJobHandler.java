package com.example.helloworld.service.invest.systemscheduler;

import com.example.helloworld.dto.invest.RunAlertPollingJobResponseDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobLogPagedDto;
import com.example.helloworld.dto.invest.SystemSchedulerRunNowResponseDto;
import com.example.helloworld.entity.invest.SchedulerJobLog;
import com.example.helloworld.repository.invest.SchedulerJobLogRepository;
import com.example.helloworld.service.invest.InvestJobService;
import com.example.helloworld.service.invest.PortfolioAlertPollingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Transactional(transactionManager = "investTransactionManager")
public class PortfolioAlertPollingSystemJobHandler implements SystemJobHandler {

    private final InvestJobService investJobService;
    private final SchedulerJobLogRepository schedulerJobLogRepository;

    @Value("${invest.scheduler.alert-polling.enabled:false}")
    private boolean schedulerEnabled;

    @Value("${invest.scheduler.alert-polling.fixed-delay-ms:120000}")
    private long fixedDelayMs;

    @Value("${invest.scheduler.alert-polling.initial-delay-ms:30000}")
    private long initialDelayMs;

    public PortfolioAlertPollingSystemJobHandler(InvestJobService investJobService,
                                                 SchedulerJobLogRepository schedulerJobLogRepository) {
        this.investJobService = investJobService;
        this.schedulerJobLogRepository = schedulerJobLogRepository;
    }

    @Override
    public SystemJobCode getJobCode() {
        return SystemJobCode.PORTFOLIO_ALERT_POLLING;
    }

    @Override
    public int getDisplayOrder() {
        return 30;
    }

    @Override
    public SystemSchedulerJobDto buildJob() {
        SystemSchedulerJobDto dto = new SystemSchedulerJobDto();
        dto.setJobCode(getJobCode().name());
        dto.setJobName(PortfolioAlertPollingService.JOB_NAME_ALERT_POLLING);
        dto.setDescription("輪詢目前持股警示條件，建立 STOP_LOSS / DROP_PERCENT / ABNORMAL_DROP 事件。");
        dto.setEnabled(schedulerEnabled);
        dto.setScheduleType("FIXED_DELAY");
        dto.setScheduleExpression(String.format("fixedDelay=%dms, initialDelay=%dms", fixedDelayMs, initialDelayMs));
        dto.setLogSource("scheduler_job_log");
        return dto;
    }

    @Override
    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public SystemSchedulerJobLogPagedDto getLatestLog() {
        return schedulerJobLogRepository
            .findTopByJobNameOrderByStartedAtDesc(PortfolioAlertPollingService.JOB_NAME_ALERT_POLLING)
            .map(this::toLogDto)
            .orElse(null);
    }

    @Override
    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<SystemSchedulerJobLogPagedDto> getLogs(int page, int size) {
        return schedulerJobLogRepository
            .findByJobNameOrderByStartedAtDesc(
                PortfolioAlertPollingService.JOB_NAME_ALERT_POLLING,
                PageRequest.of(page, size)
            )
            .map(this::toLogDto);
    }

    @Override
    public SystemSchedulerRunNowResponseDto runNow() {
        RunAlertPollingJobResponseDto result = investJobService.runAlertPollingForCurrentUser();
        SystemSchedulerRunNowResponseDto dto = new SystemSchedulerRunNowResponseDto();
        dto.setJobCode(getJobCode().name());
        dto.setStatus(result.getStatus());
        dto.setMessage(result.getMessage());
        dto.setTriggeredAt(LocalDateTime.now());
        dto.setReferenceLogId(result.getJobLogId());
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
