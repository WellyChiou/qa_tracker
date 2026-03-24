package com.example.helloworld.service.invest.systemscheduler;

import com.example.helloworld.dto.invest.RunMarketAnalysisResponseDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobLogPagedDto;
import com.example.helloworld.dto.invest.SystemSchedulerRunNowResponseDto;
import com.example.helloworld.entity.invest.SchedulerJobLog;
import com.example.helloworld.repository.invest.SchedulerJobLogRepository;
import com.example.helloworld.service.invest.MarketAnalysisExecutionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Transactional(transactionManager = "investTransactionManager")
public class MarketAnalysisSystemJobHandler implements SystemJobHandler {

    private final MarketAnalysisExecutionService marketAnalysisExecutionService;
    private final SchedulerJobLogRepository schedulerJobLogRepository;

    public MarketAnalysisSystemJobHandler(MarketAnalysisExecutionService marketAnalysisExecutionService,
                                          SchedulerJobLogRepository schedulerJobLogRepository) {
        this.marketAnalysisExecutionService = marketAnalysisExecutionService;
        this.schedulerJobLogRepository = schedulerJobLogRepository;
    }

    @Override
    public SystemJobCode getJobCode() {
        return SystemJobCode.MARKET_ANALYSIS;
    }

    @Override
    public int getDisplayOrder() {
        return 40;
    }

    @Override
    public SystemSchedulerJobDto buildJob() {
        SystemSchedulerJobDto dto = new SystemSchedulerJobDto();
        dto.setJobCode(getJobCode().name());
        dto.setJobName(MarketAnalysisExecutionService.JOB_NAME_MARKET_ANALYSIS);
        dto.setDescription("手動執行強勢股與機會股分析（預設持股 + 觀察清單）。");
        dto.setEnabled(true);
        dto.setScheduleType("MANUAL");
        dto.setScheduleExpression("MANUAL_ONLY");
        dto.setLogSource("scheduler_job_log");
        return dto;
    }

    @Override
    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public SystemSchedulerJobLogPagedDto getLatestLog() {
        return schedulerJobLogRepository
            .findTopByJobNameOrderByStartedAtDesc(MarketAnalysisExecutionService.JOB_NAME_MARKET_ANALYSIS)
            .map(this::toLogDto)
            .orElse(null);
    }

    @Override
    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<SystemSchedulerJobLogPagedDto> getLogs(int page, int size) {
        return schedulerJobLogRepository
            .findByJobNameOrderByStartedAtDesc(
                MarketAnalysisExecutionService.JOB_NAME_MARKET_ANALYSIS,
                PageRequest.of(page, size)
            )
            .map(this::toLogDto);
    }

    @Override
    public SystemSchedulerRunNowResponseDto runNow() {
        RunMarketAnalysisResponseDto result = marketAnalysisExecutionService.runForCurrentUserWithLog(
            "HOLDINGS_AND_WATCHLIST",
            "手動觸發（系統排程管理 Run Now）"
        );
        SystemSchedulerRunNowResponseDto dto = new SystemSchedulerRunNowResponseDto();
        dto.setJobCode(getJobCode().name());
        dto.setStatus(result.getStatus());
        dto.setMessage(result.getMessage());
        dto.setTriggeredAt(LocalDateTime.now());
        dto.setReferenceLogId(null);
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
