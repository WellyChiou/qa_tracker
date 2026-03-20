package com.example.helloworld.dto.invest;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DailyReportDetailDto {
    private Long id;
    private String userId;
    private LocalDate reportDate;
    private String reportType;
    private String status;
    private DailyReportSummaryDto summary;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public DailyReportSummaryDto getSummary() {
        return summary;
    }

    public void setSummary(DailyReportSummaryDto summary) {
        this.summary = summary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
