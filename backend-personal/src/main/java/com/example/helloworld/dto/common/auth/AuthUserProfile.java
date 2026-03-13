package com.example.helloworld.dto.common.auth;

import java.util.ArrayList;
import java.util.List;

public class AuthUserProfile {
    private boolean authenticated;
    private String uid;
    private String username;
    private String email;
    private String displayName;
    private String photoUrl;
    private String systemCode;
    private List<AuthMenuItem> menus = new ArrayList<>();
    private List<String> roles = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public void setSystemCode(String systemCode) {
        this.systemCode = systemCode;
    }

    public List<AuthMenuItem> getMenus() {
        return menus;
    }

    public void setMenus(List<AuthMenuItem> menus) {
        this.menus = menus != null ? menus : new ArrayList<>();
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles != null ? roles : new ArrayList<>();
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions != null ? permissions : new ArrayList<>();
    }
}
