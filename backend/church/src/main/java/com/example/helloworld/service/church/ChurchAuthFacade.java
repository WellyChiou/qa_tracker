package com.example.helloworld.service.church;

import com.example.helloworld.dto.common.auth.AuthCurrentUserResponse;
import com.example.helloworld.dto.common.auth.AuthLoginResponse;
import com.example.helloworld.dto.common.auth.AuthRefreshResponse;
import com.example.helloworld.entity.church.ChurchPermission;
import com.example.helloworld.entity.church.ChurchRole;
import com.example.helloworld.entity.church.ChurchUser;
import com.example.helloworld.service.shared.auth.AbstractAuthFacade;
import com.example.helloworld.service.shared.auth.AuthMenuMapper;
import com.example.helloworld.service.shared.auth.AuthUserProfileAssembler;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChurchAuthFacade extends AbstractAuthFacade<ChurchUser, ChurchRole, ChurchPermission, ChurchMenuService.MenuItemDTO> {
    private final ChurchMenuService menuService;
    private static final AuthMenuMapper.MenuAdapter<ChurchMenuService.MenuItemDTO> MENU_ADAPTER = new AuthMenuMapper.MenuAdapter<>() {
        @Override
        public Long getId(ChurchMenuService.MenuItemDTO source) {
            return source.getId();
        }

        @Override
        public String getMenuCode(ChurchMenuService.MenuItemDTO source) {
            return source.getMenuCode();
        }

        @Override
        public String getMenuName(ChurchMenuService.MenuItemDTO source) {
            return source.getMenuName();
        }

        @Override
        public String getIcon(ChurchMenuService.MenuItemDTO source) {
            return source.getIcon();
        }

        @Override
        public String getUrl(ChurchMenuService.MenuItemDTO source) {
            return source.getUrl();
        }

        @Override
        public Integer getOrderIndex(ChurchMenuService.MenuItemDTO source) {
            return source.getOrderIndex();
        }

        @Override
        public Boolean getShowInDashboard(ChurchMenuService.MenuItemDTO source) {
            return null;
        }

        @Override
        public List<ChurchMenuService.MenuItemDTO> getChildren(ChurchMenuService.MenuItemDTO source) {
            return source.getChildren();
        }
    };

    public ChurchAuthFacade(ChurchMenuService menuService) {
        super(createAssembler());
        this.menuService = menuService;
    }

    private static AuthUserProfileAssembler<ChurchUser, ChurchRole, ChurchPermission> createAssembler() {
        return new AuthUserProfileAssembler.Builder<ChurchUser, ChurchRole, ChurchPermission>()
            .uid(ChurchUser::getUid)
            .username(ChurchUser::getUsername)
            .email(ChurchUser::getEmail)
            .displayName(ChurchUser::getDisplayName)
            .photoUrl(ChurchUser::getPhotoUrl)
            .roles(ChurchUser::getRoles)
            .rolePermissions(role -> role == null ? List.of() : role.getPermissions())
            .permissions(ChurchUser::getPermissions)
            .roleName(ChurchRole::getRoleName)
            .permissionCode(ChurchPermission::getPermissionCode)
            .build();
    }

    public AuthLoginResponse buildLoginResponse(ChurchUser user, String accessToken, String refreshToken) {
        List<ChurchMenuService.MenuItemDTO> menus = menuService.getAdminMenus();
        return super.buildLoginResponse(user, menus, MENU_ADAPTER, "church", accessToken, refreshToken);
    }

    public AuthCurrentUserResponse buildCurrentUserResponse(ChurchUser user) {
        List<ChurchMenuService.MenuItemDTO> menus = menuService.getAdminMenus();
        return super.buildCurrentUserResponse(user, menus, MENU_ADAPTER, "church");
    }

    public AuthRefreshResponse buildRefreshResponse(String accessToken, String refreshToken) {
        return super.buildRefreshResponse("church", accessToken, refreshToken);
    }
}
