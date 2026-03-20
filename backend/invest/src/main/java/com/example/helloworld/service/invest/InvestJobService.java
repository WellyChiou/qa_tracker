package com.example.helloworld.service.invest;

import com.example.helloworld.dto.invest.RunDailyPortfolioRiskJobResponseDto;
import com.example.helloworld.dto.invest.RunAlertPollingJobResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class InvestJobService {

    private final DailyReportBatchService dailyReportBatchService;
    private final PortfolioAlertPollingService portfolioAlertPollingService;

    public InvestJobService(DailyReportBatchService dailyReportBatchService,
                            PortfolioAlertPollingService portfolioAlertPollingService) {
        this.dailyReportBatchService = dailyReportBatchService;
        this.portfolioAlertPollingService = portfolioAlertPollingService;
    }

    public RunDailyPortfolioRiskJobResponseDto runDailyPortfolioRiskReportForCurrentUser(LocalDate reportDate) {
        DailyReportBatchService.BatchExecutionResult result = dailyReportBatchService.runForCurrentUser(reportDate);
        RunDailyPortfolioRiskJobResponseDto dto = new RunDailyPortfolioRiskJobResponseDto();
        dto.setJobLogId(result.jobLogId());
        dto.setJobName(result.jobName());
        dto.setRunDate(result.runDate());
        dto.setStatus(result.status());
        dto.setProcessedUserCount(result.processedUserCount());
        dto.setSnapshotCount(result.snapshotCount());
        dto.setRiskResultCount(result.riskResultCount());
        dto.setReportCount(result.reportCount());
        dto.setReportId(result.reportId());
        dto.setMessage(result.message());
        return dto;
    }

    public RunAlertPollingJobResponseDto runAlertPollingForCurrentUser() {
        PortfolioAlertPollingService.BatchExecutionResult result = portfolioAlertPollingService.runForCurrentUser();
        RunAlertPollingJobResponseDto dto = new RunAlertPollingJobResponseDto();
        dto.setJobLogId(result.jobLogId());
        dto.setJobName(result.jobName());
        dto.setRunDate(result.runDate());
        dto.setStatus(result.status());
        dto.setProcessedUserCount(result.processedUserCount());
        dto.setProcessedPortfolioCount(result.processedPortfolioCount());
        dto.setEventCount(result.eventCount());
        dto.setMessage(result.message());
        return dto;
    }
}
