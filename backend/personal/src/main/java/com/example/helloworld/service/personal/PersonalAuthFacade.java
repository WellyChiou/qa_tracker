package com.example.helloworld.service.personal;

import com.example.helloworld.dto.common.auth.AuthCurrentUserResponse;
import com.example.helloworld.dto.common.auth.AuthLoginResponse;
import com.example.helloworld.dto.common.auth.AuthRefreshResponse;
import com.example.helloworld.entity.personal.Permission;
import com.example.helloworld.entity.personal.Role;
import com.example.helloworld.entity.personal.User;
import com.example.helloworld.service.shared.auth.AbstractAuthFacade;
import com.example.helloworld.service.shared.auth.AuthMenuMapper;
import com.example.helloworld.service.shared.auth.AuthUserProfileAssembler;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonalAuthFacade extends AbstractAuthFacade<User, Role, Permission, MenuService.MenuItemDTO> {
    private final MenuService menuService;
    private static final AuthMenuMapper.MenuAdapter<MenuService.MenuItemDTO> MENU_ADAPTER = new AuthMenuMapper.MenuAdapter<>() {
        @Override
        public Long getId(MenuService.MenuItemDTO source) {
            return source.getId();
        }

        @Override
        public String getMenuCode(MenuService.MenuItemDTO source) {
            return source.getMenuCode();
        }

        @Override
        public String getMenuName(MenuService.MenuItemDTO source) {
            return source.getMenuName();
        }

        @Override
        public String getIcon(MenuService.MenuItemDTO source) {
            return source.getIcon();
        }

        @Override
        public String getUrl(MenuService.MenuItemDTO source) {
            return source.getUrl();
        }

        @Override
        public Integer getOrderIndex(MenuService.MenuItemDTO source) {
            return source.getOrderIndex();
        }

        @Override
        public Boolean getShowInDashboard(MenuService.MenuItemDTO source) {
            return source.getShowInDashboard();
        }

        @Override
        public List<MenuService.MenuItemDTO> getChildren(MenuService.MenuItemDTO source) {
            return source.getChildren();
        }
    };

    public PersonalAuthFacade(MenuService menuService) {
        super(createAssembler());
        this.menuService = menuService;
    }

    private static AuthUserProfileAssembler<User, Role, Permission> createAssembler() {
        return new AuthUserProfileAssembler.Builder<User, Role, Permission>()
            .uid(User::getUid)
            .username(User::getUsername)
            .email(User::getEmail)
            .displayName(User::getDisplayName)
            .photoUrl(User::getPhotoUrl)
            .roles(User::getRoles)
            .rolePermissions(role -> role == null ? List.of() : role.getPermissions())
            .permissions(User::getPermissions)
            .roleName(Role::getRoleName)
            .permissionCode(Permission::getPermissionCode)
            .build();
    }

    public AuthLoginResponse buildLoginResponse(User user, String accessToken, String refreshToken) {
        List<MenuService.MenuItemDTO> menus = menuService.getVisibleMenus();
        return super.buildLoginResponse(user, menus, MENU_ADAPTER, "personal", accessToken, refreshToken);
    }

    public AuthCurrentUserResponse buildCurrentUserResponse(User user) {
        List<MenuService.MenuItemDTO> menus = menuService.getVisibleMenus();
        return super.buildCurrentUserResponse(user, menus, MENU_ADAPTER, "personal");
    }

    public AuthRefreshResponse buildRefreshResponse(String accessToken, String refreshToken) {
        return super.buildRefreshResponse("personal", accessToken, refreshToken);
    }
}
