package com.example.helloworld.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "menu_items")
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_code", length = 50, unique = true, nullable = false)
    private String menuCode;

    @Column(name = "menu_name", length = 100, nullable = false)
    private String menuName;

    @Column(name = "icon", length = 50)
    private String icon; // 圖標類名或 emoji

    @Column(name = "url", length = 255)
    private String url; // 菜單連結

    @Column(name = "parent_id")
    private Long parentId; // 父菜單 ID，用於建立層級結構

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0; // 排序順序

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // 是否啟用

    @Column(name = "show_in_dashboard")
    private Boolean showInDashboard = true; // 是否在儀表板快速訪問中顯示

    @Column(name = "required_permission", length = 100)
    private String requiredPermission; // 需要的權限代碼

    @Column(name = "description", length = 255)
    private String description;

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

    public String getMenuCode() { return menuCode; }
    public void setMenuCode(String menuCode) { this.menuCode = menuCode; }

    public String getMenuName() { return menuName; }
    public void setMenuName(String menuName) { this.menuName = menuName; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getShowInDashboard() { return showInDashboard; }
    public void setShowInDashboard(Boolean showInDashboard) { this.showInDashboard = showInDashboard; }

    public String getRequiredPermission() { return requiredPermission; }
    public void setRequiredPermission(String requiredPermission) { this.requiredPermission = requiredPermission; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

