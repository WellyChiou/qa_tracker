package com.example.helloworld.service.shared.auth;

import com.example.helloworld.dto.common.auth.AuthMenuItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AuthMenuMapper<M> {
    public interface MenuAdapter<M> {
        Long getId(M source);
        String getMenuCode(M source);
        String getMenuName(M source);
        String getIcon(M source);
        String getUrl(M source);
        Integer getOrderIndex(M source);
        Boolean getShowInDashboard(M source);
        List<M> getChildren(M source);
    }

    public List<AuthMenuItem> map(List<M> sources, MenuAdapter<M> adapter) {
        if (sources == null || adapter == null) {
            return Collections.emptyList();
        }
        List<AuthMenuItem> results = new ArrayList<>();
        for (M source : sources) {
            if (source == null) {
                continue;
            }
            results.add(mapMenuItem(source, adapter));
        }
        return results;
    }

    private AuthMenuItem mapMenuItem(M source, MenuAdapter<M> adapter) {
        AuthMenuItem item = new AuthMenuItem();
        item.setId(adapter.getId(source));
        item.setMenuCode(adapter.getMenuCode(source));
        item.setMenuName(adapter.getMenuName(source));
        item.setIcon(adapter.getIcon(source));
        item.setUrl(adapter.getUrl(source));
        item.setOrderIndex(adapter.getOrderIndex(source));
        item.setShowInDashboard(adapter.getShowInDashboard(source));
        List<M> children = adapter.getChildren(source);
        if (children != null && !children.isEmpty()) {
            item.setChildren(map(children, adapter));
        }
        return item;
    }
}
