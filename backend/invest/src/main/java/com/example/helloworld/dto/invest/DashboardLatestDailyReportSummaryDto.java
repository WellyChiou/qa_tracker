package com.example.helloworld.dto.invest;

import java.time.LocalDate;

public class DashboardLatestDailyReportSummaryDto {
    private Long reportId;
    private LocalDate reportDate;
    private String reportType;
    private String status;
    private LocalDate dataAsOfTradeDate;
    private Long highRiskCount;
    private Long criticalRiskCount;

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

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
}
