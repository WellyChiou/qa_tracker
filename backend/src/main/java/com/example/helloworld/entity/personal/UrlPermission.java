package com.example.helloworld.entity.personal;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "url_permissions")
public class UrlPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url_pattern", nullable = false, length = 255)
    private String urlPattern;

    @Column(name = "http_method", length = 10)
    private String httpMethod; // GET, POST, PUT, DELETE, null = all

    @Column(name = "required_role", length = 50)
    private String requiredRole; // ROLE_ADMIN, ROLE_USER, null = any authenticated

    @Column(name = "required_permission", length = 100)
    private String requiredPermission; // Permission code, null = no specific permission

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false; // true = no auth required

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0; // Lower number = higher priority

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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

    public String getUrlPattern() { return urlPattern; }
    public void setUrlPattern(String urlPattern) { this.urlPattern = urlPattern; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public String getRequiredRole() { return requiredRole; }
    public void setRequiredRole(String requiredRole) { this.requiredRole = requiredRole; }

    public String getRequiredPermission() { return requiredPermission; }
    public void setRequiredPermission(String requiredPermission) { this.requiredPermission = requiredPermission; }

    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
