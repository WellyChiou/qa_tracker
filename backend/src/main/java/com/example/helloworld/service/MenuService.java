package com.example.helloworld.service;

import com.example.helloworld.entity.MenuItem;
import com.example.helloworld.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MenuService {

    @Autowired
    private MenuItemRepository menuItemRepository;

    /**
     * 獲取用戶可見的菜單（根據權限過濾）
     */
    @Transactional(readOnly = true)
    public List<MenuItemDTO> getVisibleMenus() {
        try {
            // 獲取當前用戶的權限
            Set<String> userPermissions = new java.util.HashSet<>();
            
            try {
                org.springframework.security.core.Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated() && 
                    !authentication.getPrincipal().equals("anonymousUser")) {
                    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
                    if (authorities != null) {
                        userPermissions = authorities.stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet());
                    }
                }
            } catch (Exception e) {
                // 如果獲取權限失敗，使用空權限集合（只顯示不需要權限的菜單）
                System.err.println("獲取用戶權限失敗: " + e.getMessage());
            }

            // 獲取所有啟用的根菜單
            List<MenuItem> rootMenus = menuItemRepository.findActiveRootMenus();
            
            if (rootMenus == null) {
                return new ArrayList<>();
            }
            
            List<MenuItemDTO> visibleMenus = new ArrayList<>();
            
            for (MenuItem rootMenu : rootMenus) {
                try {
                    if (hasPermission(rootMenu, userPermissions)) {
                        MenuItemDTO menuDTO = convertToDTO(rootMenu);
                        
                        // 獲取子菜單
                        List<MenuItem> childMenus = menuItemRepository.findActiveChildMenus(rootMenu.getId());
                        List<MenuItemDTO> visibleChildren = new ArrayList<>();
                        
                        if (childMenus != null) {
                            for (MenuItem childMenu : childMenus) {
                                try {
                                    if (hasPermission(childMenu, userPermissions)) {
                                        visibleChildren.add(convertToDTO(childMenu));
                                    }
                                } catch (Exception e) {
                                    System.err.println("處理子菜單時發生錯誤: " + e.getMessage());
                                }
                            }
                        }
                        
                        if (!visibleChildren.isEmpty()) {
                            menuDTO.setChildren(visibleChildren);
                        }
                        
                        visibleMenus.add(menuDTO);
                    }
                } catch (Exception e) {
                    System.err.println("處理根菜單時發生錯誤: " + e.getMessage());
                }
            }
            
            return visibleMenus;
        } catch (Exception e) {
            System.err.println("獲取可見菜單時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            // 返回空列表而不是拋出異常
            return new ArrayList<>();
        }
    }

    /**
     * 檢查用戶是否有權限查看菜單
     */
    private boolean hasPermission(MenuItem menuItem, Set<String> userPermissions) {
        // 如果菜單不需要權限，任何人都可以查看
        if (menuItem.getRequiredPermission() == null || menuItem.getRequiredPermission().isEmpty()) {
            return true;
        }
        
        // 檢查用戶是否有對應的權限
        String requiredPermission = "PERM_" + menuItem.getRequiredPermission();
        return userPermissions.contains(requiredPermission);
    }

    /**
     * 轉換 MenuItem 為 DTO
     */
    private MenuItemDTO convertToDTO(MenuItem menuItem) {
        MenuItemDTO dto = new MenuItemDTO();
        dto.setId(menuItem.getId());
        dto.setMenuCode(menuItem.getMenuCode());
        dto.setMenuName(menuItem.getMenuName());
        dto.setIcon(menuItem.getIcon());
        dto.setUrl(menuItem.getUrl());
        dto.setOrderIndex(menuItem.getOrderIndex());
        dto.setShowInDashboard(menuItem.getShowInDashboard());
        return dto;
    }

    // DTO 類
    public static class MenuItemDTO {
        private Long id;
        private String menuCode;
        private String menuName;
        private String icon;
        private String url;
        private Integer orderIndex;
        private Boolean showInDashboard;
        private List<MenuItemDTO> children;

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

        public Integer getOrderIndex() { return orderIndex; }
        public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

        public Boolean getShowInDashboard() { return showInDashboard; }
        public void setShowInDashboard(Boolean showInDashboard) { this.showInDashboard = showInDashboard; }

        public List<MenuItemDTO> getChildren() { return children; }
        public void setChildren(List<MenuItemDTO> children) { this.children = children; }
    }

    /**
     * 獲取所有菜單項（用於管理）
     */
    @Transactional(readOnly = true)
    public List<MenuItem> getAllMenuItems() {
        return menuItemRepository.findAll();
    }

    /**
     * 根據 ID 獲取菜單項
     */
    @Transactional(readOnly = true)
    public Optional<MenuItem> getMenuItemById(Long id) {
        return menuItemRepository.findById(id);
    }

    /**
     * 根據菜單代碼獲取菜單項
     */
    @Transactional(readOnly = true)
    public Optional<MenuItem> getMenuItemByCode(String menuCode) {
        return menuItemRepository.findByMenuCode(menuCode);
    }

    /**
     * 創建菜單項
     */
    @Transactional
    public MenuItem createMenuItem(MenuItem menuItem) {
        // 檢查菜單代碼是否已存在
        if (menuItem.getMenuCode() != null && 
            menuItemRepository.findByMenuCode(menuItem.getMenuCode()).isPresent()) {
            throw new RuntimeException("菜單代碼已存在: " + menuItem.getMenuCode());
        }

        // 設置默認值
        if (menuItem.getIsActive() == null) {
            menuItem.setIsActive(true);
        }
        if (menuItem.getShowInDashboard() == null) {
            menuItem.setShowInDashboard(true);
        }
        if (menuItem.getOrderIndex() == null) {
            menuItem.setOrderIndex(0);
        }

        return menuItemRepository.save(menuItem);
    }

    /**
     * 更新菜單項
     */
    @Transactional
    public MenuItem updateMenuItem(Long id, MenuItem menuItemUpdate) {
        MenuItem existing = menuItemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("菜單項不存在: " + id));

        // 更新基本信息
        if (menuItemUpdate.getMenuCode() != null && 
            !menuItemUpdate.getMenuCode().equals(existing.getMenuCode())) {
            // 檢查新菜單代碼是否已存在
            if (menuItemRepository.findByMenuCode(menuItemUpdate.getMenuCode()).isPresent()) {
                throw new RuntimeException("菜單代碼已存在: " + menuItemUpdate.getMenuCode());
            }
            existing.setMenuCode(menuItemUpdate.getMenuCode());
        }
        if (menuItemUpdate.getMenuName() != null) {
            existing.setMenuName(menuItemUpdate.getMenuName());
        }
        if (menuItemUpdate.getIcon() != null) {
            existing.setIcon(menuItemUpdate.getIcon());
        }
        if (menuItemUpdate.getUrl() != null) {
            existing.setUrl(menuItemUpdate.getUrl());
        }
        if (menuItemUpdate.getParentId() != null) {
            // parentId 類型轉換已在 Controller 層處理
            existing.setParentId(menuItemUpdate.getParentId());
        }
        if (menuItemUpdate.getOrderIndex() != null) {
            existing.setOrderIndex(menuItemUpdate.getOrderIndex());
        }
        if (menuItemUpdate.getIsActive() != null) {
            existing.setIsActive(menuItemUpdate.getIsActive());
        }
        // 處理 showInDashboard：需要明確處理 Boolean 類型（false 也是有效值）
        if (menuItemUpdate.getShowInDashboard() != null) {
            existing.setShowInDashboard(menuItemUpdate.getShowInDashboard());
        }
        if (menuItemUpdate.getRequiredPermission() != null) {
            existing.setRequiredPermission(menuItemUpdate.getRequiredPermission());
        }
        if (menuItemUpdate.getDescription() != null) {
            existing.setDescription(menuItemUpdate.getDescription());
        }

        return menuItemRepository.save(existing);
    }

    /**
     * 刪除菜單項
     */
    @Transactional
    public void deleteMenuItem(Long id) {
        // 檢查是否有子菜單
        List<MenuItem> children = menuItemRepository.findByParentIdOrderByOrderIndexAsc(id);
        if (!children.isEmpty()) {
            throw new RuntimeException("無法刪除，該菜單項下還有子菜單");
        }
        menuItemRepository.deleteById(id);
    }
}

