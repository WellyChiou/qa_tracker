package com.example.helloworld.service.invest.systemscheduler;

import com.example.helloworld.dto.invest.RunDailyPortfolioRiskJobResponseDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobLogPagedDto;
import com.example.helloworld.dto.invest.SystemSchedulerRunNowResponseDto;
import com.example.helloworld.entity.invest.SchedulerJobLog;
import com.example.helloworld.repository.invest.SchedulerJobLogRepository;
import com.example.helloworld.service.invest.DailyReportBatchService;
import com.example.helloworld.service.invest.InvestJobService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Transactional(transactionManager = "investTransactionManager")
public class DailyPortfolioRiskReportSystemJobHandler implements SystemJobHandler {

    private final InvestJobService investJobService;
    private final SchedulerJobLogRepository schedulerJobLogRepository;

    @Value("${invest.scheduler.daily-report.enabled:false}")
    private boolean schedulerEnabled;

    @Value("${invest.scheduler.daily-report.cron:0 10 18 * * *}")
    private String cron;

    @Value("${invest.scheduler.daily-report.zone:Asia/Taipei}")
    private String zone;

    public DailyPortfolioRiskReportSystemJobHandler(InvestJobService investJobService,
                                                    SchedulerJobLogRepository schedulerJobLogRepository) {
        this.investJobService = investJobService;
        this.schedulerJobLogRepository = schedulerJobLogRepository;
    }

    @Override
    public SystemJobCode getJobCode() {
        return SystemJobCode.DAILY_PORTFOLIO_RISK_REPORT;
    }

    @Override
    public int getDisplayOrder() {
        return 20;
    }

    @Override
    public SystemSchedulerJobDto buildJob() {
        SystemSchedulerJobDto dto = new SystemSchedulerJobDto();
        dto.setJobCode(getJobCode().name());
        dto.setJobName(DailyReportBatchService.JOB_NAME_DAILY_PORTFOLIO_RISK_REPORT);
        dto.setDescription("每日產生持股快照、風險結果與日報摘要。");
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
            .findTopByJobNameOrderByStartedAtDesc(DailyReportBatchService.JOB_NAME_DAILY_PORTFOLIO_RISK_REPORT)
            .map(this::toLogDto)
            .orElse(null);
    }

    @Override
    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<SystemSchedulerJobLogPagedDto> getLogs(int page, int size) {
        return schedulerJobLogRepository
            .findByJobNameOrderByStartedAtDesc(
                DailyReportBatchService.JOB_NAME_DAILY_PORTFOLIO_RISK_REPORT,
                PageRequest.of(page, size)
            )
            .map(this::toLogDto);
    }

    @Override
    public SystemSchedulerRunNowResponseDto runNow() {
        RunDailyPortfolioRiskJobResponseDto result = investJobService.runDailyPortfolioRiskReportForCurrentUser(null);
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
