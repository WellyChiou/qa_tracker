package com.example.helloworld.dto.invest;

import java.time.LocalDate;

public class RunAlertPollingJobResponseDto {
    private Long jobLogId;
    private String jobName;
    private LocalDate runDate;
    private String status;
    private Integer processedUserCount;
    private Integer processedPortfolioCount;
    private Integer eventCount;
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

    public Integer getProcessedPortfolioCount() {
        return processedPortfolioCount;
    }

    public void setProcessedPortfolioCount(Integer processedPortfolioCount) {
        this.processedPortfolioCount = processedPortfolioCount;
    }

    public Integer getEventCount() {
        return eventCount;
    }

    public void setEventCount(Integer eventCount) {
        this.eventCount = eventCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
