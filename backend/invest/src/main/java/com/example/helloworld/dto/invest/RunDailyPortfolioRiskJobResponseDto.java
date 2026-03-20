package com.example.helloworld.dto.invest;

import java.time.LocalDate;

public class RunDailyPortfolioRiskJobResponseDto {
    private Long jobLogId;
    private String jobName;
    private LocalDate runDate;
    private String status;
    private Integer processedUserCount;
    private Integer snapshotCount;
    private Integer riskResultCount;
    private Integer reportCount;
    private Long reportId;
    private String message;

    public Long getJobLogId() {
        return jobLogId;
    }

    public void setJobLogId(Long jobLogId) {
        this.jobLogId = jobLogId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public LocalDate getRunDate() {
        return runDate;
    }

    public void setRunDate(LocalDate runDate) {
        this.runDate = runDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProcessedUserCount() {
        return processedUserCount;
    }

    public void setProcessedUserCount(Integer processedUserCount) {
        this.processedUserCount = processedUserCount;
    }

    public Integer getSnapshotCount() {
        return snapshotCount;
    }

    public void setSnapshotCount(Integer snapshotCount) {
        this.snapshotCount = snapshotCount;
    }

    public Integer getRiskResultCount() {
        return riskResultCount;
    }

    public void setRiskResultCount(Integer riskResultCount) {
        this.riskResultCount = riskResultCount;
    }

    public Integer getReportCount() {
        return reportCount;
    }

    public void setReportCount(Integer reportCount) {
        this.reportCount = reportCount;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
