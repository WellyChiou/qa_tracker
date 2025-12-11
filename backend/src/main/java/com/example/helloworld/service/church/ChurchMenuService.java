package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.ChurchMenuItem;
import com.example.helloworld.repository.church.ChurchMenuItemRepository;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ChurchMenuService {
    private static final Logger log = LoggerFactory.getLogger(ChurchMenuService.class);

    @Autowired
    private ChurchMenuItemRepository churchMenuItemRepository;

    /**
     * 獲取前台菜單（根據權限過濾）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<MenuItemDTO> getFrontendMenus() {
        return getMenusByType("frontend");
    }

    /**
     * 獲取後台菜單（根據權限過濾）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<MenuItemDTO> getAdminMenus() {
        return getMenusByType("admin");
    }

    /**
     * 獲取儀表板快速操作菜單（根據權限過濾，只返回 show_in_dashboard = true 的菜單）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<MenuItemDTO> getDashboardQuickActions() {
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
                log.error("❌ 獲取用戶權限失敗: {}", e.getMessage(), e);
            }

            // 獲取所有啟用的、顯示在儀表板的、後台類型的根菜單（不包含子菜單）
            List<ChurchMenuItem> dashboardMenus = churchMenuItemRepository
                .findByMenuTypeAndIsActiveAndShowInDashboardAndParentIdIsNullOrderByOrderIndexAsc(
                    "admin", true, true);

            if (dashboardMenus == null) {
                return new ArrayList<>();
            }
            
            List<MenuItemDTO> visibleMenus = new ArrayList<>();
            
            for (ChurchMenuItem menu : dashboardMenus) {
                try {
                    if (hasPermission(menu, userPermissions)) {
                        MenuItemDTO menuDTO = convertToDTO(menu);
                        visibleMenus.add(menuDTO);
                    }
                } catch (Exception e) {
                    log.error("❌ 處理儀表板菜單時發生錯誤: {}", e.getMessage(), e);
                }
            }
            
            return visibleMenus;
        } catch (Exception e) {
            log.error("❌ 獲取儀表板快速操作時發生錯誤", e);
            return new ArrayList<>();
        }
    }

    /**
     * 根據菜單類型獲取菜單
     */
    private List<MenuItemDTO> getMenusByType(String menuType) {
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
                log.error("❌ 獲取用戶權限失敗: {}", e.getMessage(), e);
            }

            // 獲取所有啟用的根菜單
            List<ChurchMenuItem> rootMenus = churchMenuItemRepository.findActiveRootMenusByType(menuType);
            
            if (rootMenus == null) {
                return new ArrayList<>();
            }
            
            List<MenuItemDTO> visibleMenus = new ArrayList<>();
            
            for (ChurchMenuItem rootMenu : rootMenus) {
                try {
                    if (hasPermission(rootMenu, userPermissions)) {
                        MenuItemDTO menuDTO = convertToDTO(rootMenu);
                        
                        // 獲取子菜單
                        List<ChurchMenuItem> childMenus = churchMenuItemRepository.findActiveChildMenus(rootMenu.getId());
                        List<MenuItemDTO> visibleChildren = new ArrayList<>();
                        
                        if (childMenus != null) {
                            for (ChurchMenuItem childMenu : childMenus) {
                                try {
                                    if (hasPermission(childMenu, userPermissions)) {
                                        visibleChildren.add(convertToDTO(childMenu));
                                    }
                                } catch (Exception e) {
                                    log.error("❌ 處理子菜單時發生錯誤: {}", e.getMessage(), e);
                                }
                            }
                        }
                        
                        if (!visibleChildren.isEmpty()) {
                            menuDTO.setChildren(visibleChildren);
                        }
                        
                        visibleMenus.add(menuDTO);
                    }
                } catch (Exception e) {
                    log.error("❌ 處理根菜單時發生錯誤: {}", e.getMessage(), e);
                }
            }
            
            return visibleMenus;
        } catch (Exception e) {
            log.error("❌ 獲取可見菜單時發生錯誤", e);
            return new ArrayList<>();
        }
    }

    /**
     * 檢查用戶是否有權限查看菜單
     */
    private boolean hasPermission(ChurchMenuItem menuItem, Set<String> userPermissions) {
        // 如果菜單不需要權限，任何人都可以查看
        if (menuItem.getRequiredPermission() == null || menuItem.getRequiredPermission().isEmpty()) {
            return true;
        }
        
        // 檢查用戶是否有對應的權限
        String requiredPermission = "PERM_" + menuItem.getRequiredPermission();
        return userPermissions.contains(requiredPermission);
    }

    /**
     * 轉換 ChurchMenuItem 為 DTO
     */
    private MenuItemDTO convertToDTO(ChurchMenuItem menuItem) {
        MenuItemDTO dto = new MenuItemDTO();
        dto.setId(menuItem.getId());
        dto.setMenuCode(menuItem.getMenuCode());
        dto.setMenuName(menuItem.getMenuName());
        dto.setIcon(menuItem.getIcon());
        dto.setUrl(menuItem.getUrl());
        dto.setOrderIndex(menuItem.getOrderIndex());
        dto.setMenuType(menuItem.getMenuType());
        dto.setDescription(menuItem.getDescription());
        return dto;
    }

    /**
     * 獲取所有菜單項（用於管理）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<ChurchMenuItem> getAllMenuItems() {
        return churchMenuItemRepository.findAll();
    }

    /**
     * 根據 ID 獲取菜單項
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<ChurchMenuItem> getMenuItemById(Long id) {
        return churchMenuItemRepository.findById(id);
    }

    /**
     * 根據菜單代碼獲取菜單項
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<ChurchMenuItem> getMenuItemByCode(String menuCode) {
        return churchMenuItemRepository.findByMenuCode(menuCode);
    }

    /**
     * 創建菜單項
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ChurchMenuItem createMenuItem(ChurchMenuItem menuItem) {
        if (menuItem.getMenuCode() != null && 
            churchMenuItemRepository.findByMenuCode(menuItem.getMenuCode()).isPresent()) {
            throw new RuntimeException("菜單代碼已存在: " + menuItem.getMenuCode());
        }

        if (menuItem.getIsActive() == null) {
            menuItem.setIsActive(true);
        }
        if (menuItem.getOrderIndex() == null) {
            menuItem.setOrderIndex(0);
        }
        if (menuItem.getMenuType() == null) {
            menuItem.setMenuType("frontend");
        }

        return churchMenuItemRepository.save(menuItem);
    }

    /**
     * 更新菜單項
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ChurchMenuItem updateMenuItem(Long id, ChurchMenuItem menuItemUpdate) {
        ChurchMenuItem existing = churchMenuItemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("菜單項不存在: " + id));

        if (menuItemUpdate.getMenuCode() != null && 
            !menuItemUpdate.getMenuCode().equals(existing.getMenuCode())) {
            if (churchMenuItemRepository.findByMenuCode(menuItemUpdate.getMenuCode()).isPresent()) {
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
            existing.setParentId(menuItemUpdate.getParentId());
        }
        if (menuItemUpdate.getOrderIndex() != null) {
            existing.setOrderIndex(menuItemUpdate.getOrderIndex());
        }
        if (menuItemUpdate.getIsActive() != null) {
            existing.setIsActive(menuItemUpdate.getIsActive());
        }
        if (menuItemUpdate.getMenuType() != null) {
            existing.setMenuType(menuItemUpdate.getMenuType());
        }
        if (menuItemUpdate.getRequiredPermission() != null) {
            existing.setRequiredPermission(menuItemUpdate.getRequiredPermission());
        }
        if (menuItemUpdate.getDescription() != null) {
            existing.setDescription(menuItemUpdate.getDescription());
        }
        if (menuItemUpdate.getShowInDashboard() != null) {
            existing.setShowInDashboard(menuItemUpdate.getShowInDashboard());
        }

        return churchMenuItemRepository.save(existing);
    }

    /**
     * 刪除菜單項
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public void deleteMenuItem(Long id) {
        List<ChurchMenuItem> children = churchMenuItemRepository.findByParentIdOrderByOrderIndexAsc(id);
        if (!children.isEmpty()) {
            throw new RuntimeException("無法刪除，該菜單項下還有子菜單");
        }
        churchMenuItemRepository.deleteById(id);
    }

    // DTO 類
    public static class MenuItemDTO {
        private Long id;
        private String menuCode;
        private String menuName;
        private String icon;
        private String url;
        private Integer orderIndex;
        private String menuType;
        private String description;
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

        public String getMenuType() { return menuType; }
        public void setMenuType(String menuType) { this.menuType = menuType; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public List<MenuItemDTO> getChildren() { return children; }
        public void setChildren(List<MenuItemDTO> children) { this.children = children; }
    }
}

