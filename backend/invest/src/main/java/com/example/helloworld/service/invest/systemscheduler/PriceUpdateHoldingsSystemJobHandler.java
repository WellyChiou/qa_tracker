package com.example.helloworld.service.invest.systemscheduler;

import com.example.helloworld.dto.invest.RunPriceUpdateResponseDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobLogPagedDto;
import com.example.helloworld.dto.invest.SystemSchedulerRunNowResponseDto;
import com.example.helloworld.entity.invest.PriceUpdateJobLog;
import com.example.helloworld.repository.invest.PriceUpdateJobLogRepository;
import com.example.helloworld.service.invest.InvestJobService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Transactional(transactionManager = "investTransactionManager")
public class PriceUpdateHoldingsSystemJobHandler implements SystemJobHandler {

    private static final String JOB_NAME = "PRICE_UPDATE_HOLDINGS";

    private final InvestJobService investJobService;
    private final PriceUpdateJobLogRepository priceUpdateJobLogRepository;

    @Value("${invest.scheduler.price-update.enabled:false}")
    private boolean schedulerEnabled;

    @Value("${invest.scheduler.price-update.tw.cron:0 30 15 * * MON-FRI}")
    private String twCron;

    @Value("${invest.scheduler.price-update.tw.zone:Asia/Taipei}")
    private String twZone;

    @Value("${invest.scheduler.price-update.us.cron:0 30 6 * * TUE-SAT}")
    private String usCron;

    @Value("${invest.scheduler.price-update.us.zone:Asia/Taipei}")
    private String usZone;

    public PriceUpdateHoldingsSystemJobHandler(InvestJobService investJobService,
                                               PriceUpdateJobLogRepository priceUpdateJobLogRepository) {
        this.investJobService = investJobService;
        this.priceUpdateJobLogRepository = priceUpdateJobLogRepository;
    }

    @Override
    public SystemJobCode getJobCode() {
        return SystemJobCode.PRICE_UPDATE_HOLDINGS;
    }

    @Override
    public int getDisplayOrder() {
        return 10;
    }

    @Override
    public SystemSchedulerJobDto buildJob() {
        SystemSchedulerJobDto dto = new SystemSchedulerJobDto();
        dto.setJobCode(getJobCode().name());
        dto.setJobName(JOB_NAME);
        dto.setDescription("更新目前登入者持股行情；排程會分 TW/US 市場執行。");
        dto.setEnabled(schedulerEnabled);
        dto.setScheduleType("CRON");
        dto.setScheduleExpression(String.format("TW: %s (%s) | US: %s (%s)", twCron, twZone, usCron, usZone));
        dto.setLogSource("price_update_job_log");
        return dto;
    }

    @Override
    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public SystemSchedulerJobLogPagedDto getLatestLog() {
        return priceUpdateJobLogRepository.findTopByJobNameOrderByStartedAtDesc(JOB_NAME)
            .map(this::toLogDto)
            .orElse(null);
    }

    @Override
    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<SystemSchedulerJobLogPagedDto> getLogs(int page, int size) {
        return priceUpdateJobLogRepository
            .findByJobNameOrderByStartedAtDesc(JOB_NAME, PageRequest.of(page, size))
            .map(this::toLogDto);
    }

    @Override
    public SystemSchedulerRunNowResponseDto runNow() {
        RunPriceUpdateResponseDto result = investJobService.runPriceUpdateForCurrentUser();
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
        return dto;
    }
}
