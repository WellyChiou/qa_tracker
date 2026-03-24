package com.example.helloworld.dto.invest;

import java.time.LocalDateTime;
import java.util.List;

public class SystemMenuDto {
    private Long id;
    private String menuCode;
    private String menuName;
    private String icon;
    private String url;
    private Long parentId;
    private Integer orderIndex;
    private Boolean isActive;
    private Boolean showInDashboard;
    private String requiredPermission;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SystemMenuDto> children;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMenuCode() {
        return menuCode;
    }

    public void setMenuCode(String menuCode) {
        this.menuCode = menuCode;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Boolean getShowInDashboard() {
        return showInDashboard;
    }

    public void setShowInDashboard(Boolean showInDashboard) {
        this.showInDashboard = showInDashboard;
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }

    public void setRequiredPermission(String requiredPermission) {
        this.requiredPermission = requiredPermission;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public List<SystemMenuDto> getChildren() {
        return children;
    }

    public void setChildren(List<SystemMenuDto> children) {
        this.children = children;
    }
}
