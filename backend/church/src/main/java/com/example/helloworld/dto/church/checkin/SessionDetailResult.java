package com.example.helloworld.dto.church.checkin;

import java.time.LocalDate;

public class SessionDetailResult {
    private Long sessionId;
    private String sessionCode;
    private String sessionTitle;
    private LocalDate sessionDate;
    private Boolean checkedIn; // 是否已簽到
    private Boolean canceled; // 是否已取消簽到

    public SessionDetailResult() {}

    public SessionDetailResult(Long sessionId, String sessionCode, String sessionTitle, 
                           LocalDate sessionDate, Boolean checkedIn, Boolean canceled) {
        this.sessionId = sessionId;
        this.sessionCode = sessionCode;
        this.sessionTitle = sessionTitle;
        this.sessionDate = sessionDate;
        this.checkedIn = checkedIn;
        this.canceled = canceled;
    }

    // Getters and Setters
    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public String getSessionTitle() {
        return sessionTitle;
    }

    public void setSessionTitle(String sessionTitle) {
        this.sessionTitle = sessionTitle;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public Boolean getCheckedIn() {
        return checkedIn;
    }

    public void setCheckedIn(Boolean checkedIn) {
        this.checkedIn = checkedIn;
    }

    public Boolean getCanceled() {
        return canceled;
    }

    public void setCanceled(Boolean canceled) {
        this.canceled = canceled;
    }
}

