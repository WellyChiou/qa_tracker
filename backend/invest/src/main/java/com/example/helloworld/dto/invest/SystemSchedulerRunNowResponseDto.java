package com.example.helloworld.dto.invest;

import java.time.LocalDateTime;

public class SystemSchedulerRunNowResponseDto {
    private String jobCode;
    private String status;
    private String message;
    private LocalDateTime triggeredAt;
    private Long referenceLogId;

    public String getJobCode() {
        return jobCode;
    }

    public void setJobCode(String jobCode) {
        this.jobCode = jobCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(LocalDateTime triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public Long getReferenceLogId() {
        return referenceLogId;
    }

    public void setReferenceLogId(Long referenceLogId) {
        this.referenceLogId = referenceLogId;
    }
}
