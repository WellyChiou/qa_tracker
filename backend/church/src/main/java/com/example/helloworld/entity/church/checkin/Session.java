package com.example.helloworld.entity.church.checkin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.*;
import java.util.List;

@Getter
@Setter
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
}

