package com.example.helloworld.entity.church;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "church_info")
public class ChurchInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "info_key", length = 50, unique = true, nullable = false)
    private String infoKey;

    @Column(name = "info_value", columnDefinition = "TEXT")
    private String infoValue;

    @Column(name = "info_type", length = 20)
    private String infoType = "text";

    @Column(name = "display_order")
    private Integer displayOrder = 0;

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

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInfoKey() { return infoKey; }
    public void setInfoKey(String infoKey) { this.infoKey = infoKey; }

    public String getInfoValue() { return infoValue; }
    public void setInfoValue(String infoValue) { this.infoValue = infoValue; }

    public String getInfoType() { return infoType; }
    public void setInfoType(String infoType) { this.infoType = infoType; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

