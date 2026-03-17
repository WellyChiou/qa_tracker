package com.example.helloworld.service.church.seed;

import com.example.helloworld.entity.church.ChurchMenuItem;
import com.example.helloworld.entity.church.ChurchPermission;
import com.example.helloworld.entity.church.ChurchRole;
import com.example.helloworld.entity.church.ChurchUrlPermission;
import com.example.helloworld.repository.church.ChurchMenuItemRepository;
import com.example.helloworld.repository.church.ChurchPermissionRepository;
import com.example.helloworld.repository.church.ChurchRoleRepository;
import com.example.helloworld.repository.church.ChurchUrlPermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class ChurchAuthorizationSeedSupport {
    private final ChurchPermissionRepository permissionRepository;
    private final ChurchMenuItemRepository menuItemRepository;
    private final ChurchUrlPermissionRepository urlPermissionRepository;
    private final ChurchRoleRepository roleRepository;

    public ChurchAuthorizationSeedSupport(
            ChurchPermissionRepository permissionRepository,
            ChurchMenuItemRepository menuItemRepository,
            ChurchUrlPermissionRepository urlPermissionRepository,
            ChurchRoleRepository roleRepository) {
        this.permissionRepository = permissionRepository;
        this.menuItemRepository = menuItemRepository;
        this.urlPermissionRepository = urlPermissionRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(transactionManager = "churchTransactionManager")
    public ChurchPermission ensurePermission(String code, String name, String resource, String action, String description) {
        ChurchPermission permission = permissionRepository.findByPermissionCode(code)
                .orElseGet(ChurchPermission::new);
        permission.setPermissionCode(code);
        permission.setPermissionName(name);
        permission.setResource(resource);
        permission.setAction(action);
        permission.setDescription(description);
        return permissionRepository.save(permission);
    }

    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Long findMenuId(String menuCode) {
        return menuItemRepository.findByMenuCode(menuCode)
                .map(ChurchMenuItem::getId)
                .orElse(null);
    }

    @Transactional(transactionManager = "churchTransactionManager")
    public void ensureMenu(
            String menuCode,
            String menuName,
            String icon,
            String url,
            Long parentId,
            int orderIndex,
            String requiredPermission,
            String description) {
        ChurchMenuItem menuItem = menuItemRepository.findByMenuCode(menuCode)
                .orElseGet(ChurchMenuItem::new);
        menuItem.setMenuCode(menuCode);
        menuItem.setMenuName(menuName);
        menuItem.setIcon(icon);
        menuItem.setUrl(url);
        menuItem.setParentId(parentId);
        menuItem.setOrderIndex(orderIndex);
        menuItem.setIsActive(true);
        menuItem.setMenuType("admin");
        menuItem.setRequiredPermission(requiredPermission);
        menuItem.setDescription(description);
        menuItem.setShowInDashboard(false);
        menuItemRepository.save(menuItem);
    }

    @Transactional(transactionManager = "churchTransactionManager")
    public void ensureUrlPermission(
            String urlPattern,
            String httpMethod,
            String requiredPermission,
            String requiredRole,
            boolean isPublic,
            int orderIndex,
            boolean isActive,
            String description) {
        ChurchUrlPermission permission = urlPermissionRepository
                .findByUrlPatternAndHttpMethod(urlPattern, httpMethod)
                .orElseGet(ChurchUrlPermission::new);
        permission.setUrlPattern(urlPattern);
        permission.setHttpMethod(httpMethod);
        permission.setRequiredPermission(requiredPermission);
        permission.setRequiredRole(requiredRole);
        permission.setIsPublic(isPublic);
        permission.setOrderIndex(orderIndex);
        permission.setIsActive(isActive);
        permission.setDescription(description);
        urlPermissionRepository.save(permission);
    }

    @Transactional(transactionManager = "churchTransactionManager")
    public void assignPermissionsToRole(String roleName, ChurchPermission... permissionsToAssign) {
        Optional<ChurchRole> roleOpt = roleRepository.findByRoleName(roleName);
        if (roleOpt.isEmpty()) {
            return;
        }

        ChurchRole role = roleOpt.get();
        Set<ChurchPermission> permissions = role.getPermissions() != null
                ? role.getPermissions()
                : new HashSet<>();

        for (ChurchPermission permission : permissionsToAssign) {
            permissions.add(permission);
        }

        role.setPermissions(permissions);
        roleRepository.save(role);
    }
}
