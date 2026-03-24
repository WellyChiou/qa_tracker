package com.example.helloworld.service.invest.auth;

import com.example.helloworld.entity.invest.auth.InvestMenuItem;
import com.example.helloworld.repository.invest.auth.InvestMenuItemRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InvestMenuService {

    private final InvestMenuItemRepository investMenuItemRepository;

    public InvestMenuService(InvestMenuItemRepository investMenuItemRepository) {
        this.investMenuItemRepository = investMenuItemRepository;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<MenuItemDto> getVisibleMenus() {
        Set<String> userPermissions = new HashSet<>();
        org.springframework.security.core.Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            if (authorities != null) {
                userPermissions.addAll(authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()));
            }
        }

        List<InvestMenuItem> rootMenus = investMenuItemRepository.findActiveRootMenus();
        List<MenuItemDto> visibleMenus = new ArrayList<>();
        for (InvestMenuItem rootMenu : rootMenus) {
            if (!hasPermission(rootMenu, userPermissions)) {
                continue;
            }

            MenuItemDto rootDto = toDto(rootMenu);
            List<InvestMenuItem> children = investMenuItemRepository.findActiveChildMenus(rootMenu.getId());
            List<MenuItemDto> visibleChildren = children.stream()
                .filter(child -> hasPermission(child, userPermissions))
                .map(this::toDto)
                .toList();
            rootDto.setChildren(visibleChildren);
            visibleMenus.add(rootDto);
        }

        return visibleMenus;
    }

    private boolean hasPermission(InvestMenuItem menuItem, Set<String> userPermissions) {
        if (menuItem.getRequiredPermission() == null || menuItem.getRequiredPermission().isBlank()) {
            return true;
        }

        String rawCode = menuItem.getRequiredPermission().trim();
        String normalizedCode = rawCode.startsWith("PERM_") ? rawCode.substring(5) : rawCode;
        return normalizedCode.isEmpty() || userPermissions.contains(normalizedCode);
    }

    private MenuItemDto toDto(InvestMenuItem menuItem) {
        MenuItemDto dto = new MenuItemDto();
        dto.setId(menuItem.getId());
        dto.setMenuCode(menuItem.getMenuCode());
        dto.setMenuName(menuItem.getMenuName());
        dto.setIcon(menuItem.getIcon());
        dto.setUrl(menuItem.getUrl());
        dto.setOrderIndex(menuItem.getOrderIndex());
        dto.setShowInDashboard(menuItem.getShowInDashboard());
        dto.setChildren(List.of());
        return dto;
    }

    public static class MenuItemDto {
        private Long id;
        private String menuCode;
        private String menuName;
        private String icon;
        private String url;
        private Integer orderIndex;
        private Boolean showInDashboard;
        private List<MenuItemDto> children;

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

        public List<MenuItemDto> getChildren() {
            return children;
        }

        public void setChildren(List<MenuItemDto> children) {
            this.children = children;
        }
    }
}
