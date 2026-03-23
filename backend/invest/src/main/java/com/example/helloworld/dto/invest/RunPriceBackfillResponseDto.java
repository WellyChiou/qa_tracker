package com.example.helloworld.dto.invest;

import java.time.LocalDateTime;

public class RunPriceBackfillResponseDto {
    private Long jobLogId;
    private String jobName;
    private String batchId;
    private String runMode;
    private String status;
    private String scope;
    private Integer days;
    private Integer targetStockCount;
    private Integer successCount;
    private Integer failCount;
    private Integer upsertedRowCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
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

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getRunMode() {
        return runMode;
    }

    public void setRunMode(String runMode) {
        this.runMode = runMode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Integer getTargetStockCount() {
        return targetStockCount;
    }

    public void setTargetStockCount(Integer targetStockCount) {
        this.targetStockCount = targetStockCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public Integer getUpsertedRowCount() {
        return upsertedRowCount;
    }

    public void setUpsertedRowCount(Integer upsertedRowCount) {
        this.upsertedRowCount = upsertedRowCount;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
