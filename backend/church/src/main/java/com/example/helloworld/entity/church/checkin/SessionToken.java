package com.example.helloworld.entity.church.checkin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.*;

@Getter
@Setter
@Entity
@Table(name = "session_tokens")
public class SessionToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="session_id", nullable = false)
    private Long sessionId;

    @Column(nullable = false, length = 16)
    private String token;

    @Column(name="expires_at", nullable = false)
    private LocalDateTime expiresAt;

}

