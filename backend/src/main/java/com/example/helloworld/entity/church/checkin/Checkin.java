package com.example.helloworld.entity.church.checkin;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "checkins",
    uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "member_id"})
)
public class Checkin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="session_id", nullable = false)
    private Long sessionId;

    @Column(name="member_id", nullable = false)
    private Long memberId;

    @Column(name="checked_in_at", nullable = false)
    private LocalDateTime checkedInAt;

    private String ip;

    @Column(name="user_agent")
    private String userAgent;

    private Boolean manual = false;

    @Column(name="manual_note")
    private String manualNote;

    @Column(name="manual_by")
    private String manualBy;

    private Boolean canceled = false;

    @Column(name="canceled_at")
    private LocalDateTime canceledAt;

    @Column(name="canceled_by")
    private String canceledBy;

    @Column(name="canceled_note")
    private String canceledNote;

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(LocalDateTime checkedInAt) { this.checkedInAt = checkedInAt; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public Boolean getManual() { return manual; }
    public void setManual(Boolean manual) { this.manual = manual; }
    public String getManualNote() { return manualNote; }
    public void setManualNote(String manualNote) { this.manualNote = manualNote; }
    public String getManualBy() { return manualBy; }
    public void setManualBy(String manualBy) { this.manualBy = manualBy; }
    public Boolean getCanceled() { return canceled; }
    public void setCanceled(Boolean canceled) { this.canceled = canceled; }
    public LocalDateTime getCanceledAt() { return canceledAt; }
    public void setCanceledAt(LocalDateTime canceledAt) { this.canceledAt = canceledAt; }
    public String getCanceledBy() { return canceledBy; }
    public void setCanceledBy(String canceledBy) { this.canceledBy = canceledBy; }
    public String getCanceledNote() { return canceledNote; }
    public void setCanceledNote(String canceledNote) { this.canceledNote = canceledNote; }
}

