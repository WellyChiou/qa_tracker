package com.example.helloworld.entity.church;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "service_schedule_dates")
public class ServiceScheduleDate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_schedule_id", nullable = false)
    private ServiceSchedule serviceSchedule;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "day_of_week", insertable = false, updatable = false)
    private Integer dayOfWeek; // 1=週日, 2=週一, ..., 7=週六（GENERATED COLUMN，自動計算）

    @Column(name = "day_of_week_label", insertable = false, updatable = false, length = 10)
    private String dayOfWeekLabel; // 六（週六）或 日（週日）（GENERATED COLUMN，自動計算）

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "serviceScheduleDate", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50) // 批量載入，減少 N+1 查詢問題
    private List<ServiceSchedulePositionConfig> positionConfigs;

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

    public ServiceSchedule getServiceSchedule() {
        return serviceSchedule;
    }

    public void setServiceSchedule(ServiceSchedule serviceSchedule) {
        this.serviceSchedule = serviceSchedule;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getDayOfWeekLabel() {
        return dayOfWeekLabel;
    }

    public void setDayOfWeekLabel(String dayOfWeekLabel) {
        this.dayOfWeekLabel = dayOfWeekLabel;
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

    public List<ServiceSchedulePositionConfig> getPositionConfigs() {
        return positionConfigs;
    }

    public void setPositionConfigs(List<ServiceSchedulePositionConfig> positionConfigs) {
        this.positionConfigs = positionConfigs;
    }
}

