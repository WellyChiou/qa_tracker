package com.example.helloworld.service.invest;

import com.example.helloworld.dto.invest.RunDailyPortfolioRiskJobResponseDto;
import com.example.helloworld.dto.invest.RunAlertPollingJobResponseDto;
import com.example.helloworld.dto.invest.RunMarketAnalysisResponseDto;
import com.example.helloworld.dto.invest.RunPriceUpdateResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class InvestJobService {

    private final DailyReportBatchService dailyReportBatchService;
    private final PortfolioAlertPollingService portfolioAlertPollingService;
    private final PriceUpdateService priceUpdateService;
    private final MarketAnalysisService marketAnalysisService;

    public InvestJobService(DailyReportBatchService dailyReportBatchService,
                            PortfolioAlertPollingService portfolioAlertPollingService,
                            PriceUpdateService priceUpdateService,
                            MarketAnalysisService marketAnalysisService) {
        this.dailyReportBatchService = dailyReportBatchService;
        this.portfolioAlertPollingService = portfolioAlertPollingService;
        this.priceUpdateService = priceUpdateService;
        this.marketAnalysisService = marketAnalysisService;
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

    public RunPriceUpdateResponseDto runPriceUpdateForCurrentUser() {
        return priceUpdateService.runForCurrentUser();
    }

    public RunMarketAnalysisResponseDto runMarketAnalysisForCurrentUser(String scope) {
        return marketAnalysisService.runForCurrentUser(scope);
    }
}
