package com.example.helloworld.entity.church;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "position_persons", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"position_id", "person_id", "day_type"}))
public class PositionPerson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = false)
    private Position position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "day_type", nullable = false, length = 20)
    private String dayType; // "saturday" 或 "sunday"

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "include_in_auto_schedule", nullable = false)
    private Boolean includeInAutoSchedule = true; // 是否參與自動分配，默認為 true

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

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public String getDayType() {
        return dayType;
    }

    public void setDayType(String dayType) {
        this.dayType = dayType;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getIncludeInAutoSchedule() {
        return includeInAutoSchedule;
    }

    public void setIncludeInAutoSchedule(Boolean includeInAutoSchedule) {
        this.includeInAutoSchedule = includeInAutoSchedule;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

