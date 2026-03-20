package com.example.helloworld.dto.invest;

import java.time.LocalDateTime;

public class WatchlistDefaultDto {
    private Long id;
    private String name;
    private Boolean isDefault;
    private Boolean isActive;
    private Integer activeItemCount;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean aDefault) {
        isDefault = aDefault;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Integer getActiveItemCount() {
        return activeItemCount;
    }

    public void setActiveItemCount(Integer activeItemCount) {
        this.activeItemCount = activeItemCount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
