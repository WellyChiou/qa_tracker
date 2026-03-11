package com.example.helloworld.entity.church;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "activities")
public class Activity {
    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "activity_date")
    private LocalDate activityDate;

    @Column(name = "start_time", length = 100)
    private String startTime;  // 活動開始時間

    @Column(name = "end_time", length = 100)
    private String endTime;  // 活動結束時間

    @Column(name = "activity_sessions", columnDefinition = "JSON")
    private String activitySessions;  // JSON 格式的多個時間段

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "tags", columnDefinition = "JSON")
    private String tags; // JSON 格式的標籤陣列

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}

