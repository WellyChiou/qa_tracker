package com.example.helloworld.service.invest.systemscheduler;

import com.example.helloworld.dto.invest.RunPriceBackfillResponseDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobLogPagedDto;
import com.example.helloworld.dto.invest.SystemSchedulerRunNowResponseDto;
import com.example.helloworld.entity.invest.PriceUpdateJobLog;
import com.example.helloworld.repository.invest.PriceUpdateJobLogRepository;
import com.example.helloworld.service.invest.InvestJobService;
import com.example.helloworld.service.invest.PriceBackfillService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Transactional(transactionManager = "investTransactionManager")
public class PriceBackfillSystemJobHandler implements SystemJobHandler {

    private final InvestJobService investJobService;
    private final PriceUpdateJobLogRepository priceUpdateJobLogRepository;
    private final PriceUpdateLogDetailEnricher priceUpdateLogDetailEnricher;

    public PriceBackfillSystemJobHandler(InvestJobService investJobService,
                                         PriceUpdateJobLogRepository priceUpdateJobLogRepository,
                                         PriceUpdateLogDetailEnricher priceUpdateLogDetailEnricher) {
        this.investJobService = investJobService;
        this.priceUpdateJobLogRepository = priceUpdateJobLogRepository;
        this.priceUpdateLogDetailEnricher = priceUpdateLogDetailEnricher;
    }

    @Override
    public SystemJobCode getJobCode() {
        return SystemJobCode.PRICE_BACKFILL;
    }

    @Override
    public int getDisplayOrder() {
        return 15;
    }

    @Override
    public SystemSchedulerJobDto buildJob() {
        SystemSchedulerJobDto dto = new SystemSchedulerJobDto();
        dto.setJobCode(getJobCode().name());
        dto.setJobName(PriceBackfillService.JOB_NAME);
        dto.setDescription("回補歷史行情（手動）。預設回補 30 日，範圍：持股 + default watchlist。");
        dto.setEnabled(true);
        dto.setScheduleType("MANUAL_ONLY");
        dto.setScheduleExpression("手動觸發（可透過 API 指定 30/60 或 5~120 日）");
        dto.setLogSource("price_update_job_log");
        return dto;
    }

    @Override
    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public SystemSchedulerJobLogPagedDto getLatestLog() {
        return priceUpdateJobLogRepository.findTopByJobNameOrderByStartedAtDesc(PriceBackfillService.JOB_NAME)
            .map(this::toLogDto)
            .orElse(null);
    }

    @Override
    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<SystemSchedulerJobLogPagedDto> getLogs(int page, int size) {
        return priceUpdateJobLogRepository
            .findByJobNameOrderByStartedAtDesc(PriceBackfillService.JOB_NAME, PageRequest.of(page, size))
            .map(this::toLogDto);
    }

    @Override
    public SystemSchedulerRunNowResponseDto runNow() {
        RunPriceBackfillResponseDto result = investJobService.runPriceBackfillForCurrentUser(30, "HOLDINGS_AND_WATCHLIST");
        SystemSchedulerRunNowResponseDto dto = new SystemSchedulerRunNowResponseDto();
        dto.setJobCode(getJobCode().name());
        dto.setStatus(result.getStatus());
        dto.setMessage(result.getMessage());
        dto.setTriggeredAt(LocalDateTime.now());
        dto.setReferenceLogId(result.getJobLogId());
        return dto;
    }

    private SystemSchedulerJobLogPagedDto toLogDto(PriceUpdateJobLog log) {
        SystemSchedulerJobLogPagedDto dto = new SystemSchedulerJobLogPagedDto();
        dto.setId(log.getId());
        dto.setJobCode(getJobCode().name());
        dto.setStatus(log.getStatus() == null ? null : log.getStatus().name());
        dto.setRunDate(log.getStartedAt() == null ? null : log.getStartedAt().toLocalDate());
        dto.setStartedAt(log.getStartedAt());
        dto.setFinishedAt(log.getFinishedAt());
        dto.setMessage(log.getMessage());
        dto.setSourceTable("price_update_job_log");
        dto.setRunMode(log.getRunMode() == null ? null : log.getRunMode().name());
        dto.setBatchId(log.getBatchId());
        dto.setTotalCount(log.getTotalCount());
        dto.setSuccessCount(log.getSuccessCount());
        dto.setFailCount(log.getFailCount());
        priceUpdateLogDetailEnricher.enrich(dto, log.getId());
        return dto;
    }
}
