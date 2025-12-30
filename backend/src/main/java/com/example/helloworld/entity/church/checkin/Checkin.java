package com.example.helloworld.entity.church.checkin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.*;

@Getter
@Setter
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

}

