package com.example.helloworld.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(name = "uid", length = 128)
    private String uid;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "username", length = 100, unique = true)
    private String username; // 用戶名（用於登入）

    @Column(name = "password", length = 255)
    private String password; // 加密後的密碼

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;

    @Column(name = "provider_id", length = 50)
    private String providerId; // 登入提供者（如 'local', 'firebase', 'google' 等）

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true; // 帳號是否啟用

    @Column(name = "is_account_non_locked", nullable = false)
    private Boolean isAccountNonLocked = true; // 帳號是否未鎖定

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_uid"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_permissions",
        joinColumns = @JoinColumn(name = "user_uid"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>(); // 用戶直接分配的權限

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
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }

    public Boolean getIsAccountNonLocked() { return isAccountNonLocked; }
    public void setIsAccountNonLocked(Boolean isAccountNonLocked) { this.isAccountNonLocked = isAccountNonLocked; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public Set<Permission> getPermissions() { return permissions; }
    public void setPermissions(Set<Permission> permissions) { this.permissions = permissions; }
}

