package com.example.helloworld.dto.common.auth;

import java.util.ArrayList;
import java.util.List;

public class AuthMenuItem {
    private Long id;
    private String menuCode;
    private String menuName;
    private String icon;
    private String url;
    private Integer orderIndex;
    private Boolean showInDashboard;
    private List<AuthMenuItem> children = new ArrayList<>();

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

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Boolean getShowInDashboard() {
        return showInDashboard;
    }

    public void setShowInDashboard(Boolean showInDashboard) {
        this.showInDashboard = showInDashboard;
    }

    public List<AuthMenuItem> getChildren() {
        return children;
    }

    public void setChildren(List<AuthMenuItem> children) {
        this.children = children != null ? children : new ArrayList<>();
    }
}
