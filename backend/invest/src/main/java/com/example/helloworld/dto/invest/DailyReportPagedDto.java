package com.example.helloworld.dto.invest;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DailyReportPagedDto {
    private Long id;
    private LocalDate reportDate;
    private String reportType;
    private String status;
    private LocalDate dataAsOfTradeDate;
    private Long holdingCount;
    private Long highRiskCount;
    private Long criticalRiskCount;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getHoldingCount() {
        return holdingCount;
    }

    public void setHoldingCount(Long holdingCount) {
        this.holdingCount = holdingCount;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
