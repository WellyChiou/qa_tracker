package com.example.helloworld.dto.invest;

import java.math.BigDecimal;
import java.util.List;

public class PortfolioDashboardOverviewDto {
    private BigDecimal totalCost;
    private BigDecimal totalMarketValue;
    private BigDecimal totalPnL;
    private BigDecimal totalPnLPercent;
    private Long holdingCount;
    private List<DashboardDistributionItemDto> riskDistribution;
    private List<DashboardDistributionItemDto> recommendationDistribution;
    private List<DashboardTopRiskHoldingDto> topRiskHoldings;
    private DashboardAlertSummaryDto alertSummary;
    private DashboardLatestDailyReportSummaryDto latestDailyReportSummary;

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

    public Long getHoldingCount() {
        return holdingCount;
    }

    public void setHoldingCount(Long holdingCount) {
        this.holdingCount = holdingCount;
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

    public List<DashboardTopRiskHoldingDto> getTopRiskHoldings() {
        return topRiskHoldings;
    }

    public void setTopRiskHoldings(List<DashboardTopRiskHoldingDto> topRiskHoldings) {
        this.topRiskHoldings = topRiskHoldings;
    }

    public DashboardAlertSummaryDto getAlertSummary() {
        return alertSummary;
    }

    public void setAlertSummary(DashboardAlertSummaryDto alertSummary) {
        this.alertSummary = alertSummary;
    }

    public DashboardLatestDailyReportSummaryDto getLatestDailyReportSummary() {
        return latestDailyReportSummary;
    }

    public void setLatestDailyReportSummary(DashboardLatestDailyReportSummaryDto latestDailyReportSummary) {
        this.latestDailyReportSummary = latestDailyReportSummary;
    }
}
