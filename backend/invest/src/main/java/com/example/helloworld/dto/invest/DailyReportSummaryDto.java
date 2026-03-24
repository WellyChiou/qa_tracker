package com.example.helloworld.dto.invest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class DailyReportSummaryDto {
    private LocalDate reportDate;
    private String reportType;
    private String status;
    private LocalDate dataAsOfTradeDate;
    private Long holdingCount;
    private Long snapshotCount;
    private BigDecimal totalCost;
    private BigDecimal totalMarketValue;
    private BigDecimal totalPnL;
    private BigDecimal totalPnLPercent;
    private Long highRiskCount;
    private Long criticalRiskCount;
    private List<DashboardDistributionItemDto> riskDistribution;
    private List<DashboardDistributionItemDto> recommendationDistribution;
    private List<DailyReportTopRiskHoldingDto> topRiskHoldings;
    private List<String> noviceReminders;
    private String disclaimer;

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getDataAsOfTradeDate() {
        return dataAsOfTradeDate;
    }

    public void setDataAsOfTradeDate(LocalDate dataAsOfTradeDate) {
        this.dataAsOfTradeDate = dataAsOfTradeDate;
    }

    public Long getHoldingCount() {
        return holdingCount;
    }

    public void setHoldingCount(Long holdingCount) {
        this.holdingCount = holdingCount;
    }

    public Long getSnapshotCount() {
        return snapshotCount;
    }

    public void setSnapshotCount(Long snapshotCount) {
        this.snapshotCount = snapshotCount;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getTotalMarketValue() {
        return totalMarketValue;
    }

    public void setTotalMarketValue(BigDecimal totalMarketValue) {
        this.totalMarketValue = totalMarketValue;
    }

    public BigDecimal getTotalPnL() {
        return totalPnL;
    }

    public void setTotalPnL(BigDecimal totalPnL) {
        this.totalPnL = totalPnL;
    }

    public BigDecimal getTotalPnLPercent() {
        return totalPnLPercent;
    }

    public void setTotalPnLPercent(BigDecimal totalPnLPercent) {
        this.totalPnLPercent = totalPnLPercent;
    }

    public Long getHighRiskCount() {
        return highRiskCount;
    }

    public void setHighRiskCount(Long highRiskCount) {
        this.highRiskCount = highRiskCount;
    }

    public Long getCriticalRiskCount() {
        return criticalRiskCount;
    }

    public void setCriticalRiskCount(Long criticalRiskCount) {
        this.criticalRiskCount = criticalRiskCount;
    }

    public List<DashboardDistributionItemDto> getRiskDistribution() {
        return riskDistribution;
    }

    public void setRiskDistribution(List<DashboardDistributionItemDto> riskDistribution) {
        this.riskDistribution = riskDistribution;
    }

    public List<DashboardDistributionItemDto> getRecommendationDistribution() {
        return recommendationDistribution;
    }

    public void setRecommendationDistribution(List<DashboardDistributionItemDto> recommendationDistribution) {
        this.recommendationDistribution = recommendationDistribution;
    }

    public List<DailyReportTopRiskHoldingDto> getTopRiskHoldings() {
        return topRiskHoldings;
    }

    public void setTopRiskHoldings(List<DailyReportTopRiskHoldingDto> topRiskHoldings) {
        this.topRiskHoldings = topRiskHoldings;
    }

    public List<String> getNoviceReminders() {
        return noviceReminders;
    }

    public void setNoviceReminders(List<String> noviceReminders) {
        this.noviceReminders = noviceReminders;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
