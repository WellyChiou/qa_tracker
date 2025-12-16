package com.example.helloworld.entity.personal;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "line_groups")
public class LineGroup {
    @Id
    @Column(name = "group_id", length = 100)
    private String groupId; // LINE 群組 ID

    @Column(name = "group_name", length = 255)
    private String groupName; // 群組名稱（可選）

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // 是否啟用通知

    @Column(name = "member_count", nullable = false)
    private Integer memberCount = 0; // 群組人數

    @Column(name = "group_code", length = 50)
    private String groupCode; // 群組代碼：PERSONAL（個人）或 CHURCH_TECH_CONTROL（教會技術控制）

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
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
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
