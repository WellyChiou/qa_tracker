package com.example.helloworld.entity.church.checkin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.*;
import java.util.List;

@Entity
@Table(name = "sessions")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="session_type")
    private String sessionType;

    private String title;

    @Column(name="session_date")
    private LocalDate sessionDate;

    @Column(name="open_at")
    private LocalDateTime openAt;

    @Column(name="close_at")
    private LocalDateTime closeAt;

    private String status;

    @Column(name="session_code", unique = true)
    private String sessionCode;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<SessionGroup> sessionGroups;

    public Long getId() { return id; }
    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }
    public LocalDateTime getOpenAt() { return openAt; }
    public void setOpenAt(LocalDateTime openAt) { this.openAt = openAt; }
    public LocalDateTime getCloseAt() { return closeAt; }
    public void setCloseAt(LocalDateTime closeAt) { this.closeAt = closeAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSessionCode() { return sessionCode; }
    public void setSessionCode(String sessionCode) { this.sessionCode = sessionCode; }
    public List<SessionGroup> getSessionGroups() { return sessionGroups; }
    public void setSessionGroups(List<SessionGroup> sessionGroups) { this.sessionGroups = sessionGroups; }
}

