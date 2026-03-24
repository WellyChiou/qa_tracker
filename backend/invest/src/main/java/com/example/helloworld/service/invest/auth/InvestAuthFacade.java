package com.example.helloworld.service.invest.auth;

import com.example.helloworld.dto.common.auth.AuthCurrentUserResponse;
import com.example.helloworld.dto.common.auth.AuthLoginResponse;
import com.example.helloworld.dto.common.auth.AuthRefreshResponse;
import com.example.helloworld.entity.invest.auth.InvestPermission;
import com.example.helloworld.entity.invest.auth.InvestRole;
import com.example.helloworld.entity.invest.auth.InvestUser;
import com.example.helloworld.service.shared.auth.AbstractAuthFacade;
import com.example.helloworld.service.shared.auth.AuthMenuMapper;
import com.example.helloworld.service.shared.auth.AuthUserProfileAssembler;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvestAuthFacade extends AbstractAuthFacade<InvestUser, InvestRole, InvestPermission, InvestMenuService.MenuItemDto> {

    private final InvestMenuService investMenuService;

    private static final AuthMenuMapper.MenuAdapter<InvestMenuService.MenuItemDto> MENU_ADAPTER =
        new AuthMenuMapper.MenuAdapter<>() {
            @Override
            public Long getId(InvestMenuService.MenuItemDto source) {
                return source.getId();
            }

            @Override
            public String getMenuCode(InvestMenuService.MenuItemDto source) {
                return source.getMenuCode();
            }

            @Override
            public String getMenuName(InvestMenuService.MenuItemDto source) {
                return source.getMenuName();
            }

            @Override
            public String getIcon(InvestMenuService.MenuItemDto source) {
                return source.getIcon();
            }

            @Override
            public String getUrl(InvestMenuService.MenuItemDto source) {
                return source.getUrl();
            }

            @Override
            public Integer getOrderIndex(InvestMenuService.MenuItemDto source) {
                return source.getOrderIndex();
            }

            @Override
            public Boolean getShowInDashboard(InvestMenuService.MenuItemDto source) {
                return source.getShowInDashboard();
            }

            @Override
            public List<InvestMenuService.MenuItemDto> getChildren(InvestMenuService.MenuItemDto source) {
                return source.getChildren();
            }
        };

    public InvestAuthFacade(InvestMenuService investMenuService) {
        super(createAssembler());
        this.investMenuService = investMenuService;
    }

    private static AuthUserProfileAssembler<InvestUser, InvestRole, InvestPermission> createAssembler() {
        return new AuthUserProfileAssembler.Builder<InvestUser, InvestRole, InvestPermission>()
            .uid(InvestUser::getUid)
            .username(InvestUser::getUsername)
            .email(InvestUser::getEmail)
            .displayName(InvestUser::getDisplayName)
            .photoUrl(InvestUser::getPhotoUrl)
            .roles(InvestUser::getRoles)
            .rolePermissions(role -> role == null ? List.of() : role.getPermissions())
            .permissions(InvestUser::getPermissions)
            .roleName(InvestRole::getRoleName)
            .permissionCode(InvestPermission::getPermissionCode)
            .build();
    }

    public AuthLoginResponse buildLoginResponse(InvestUser user, String accessToken, String refreshToken) {
        List<InvestMenuService.MenuItemDto> menus = investMenuService.getVisibleMenus();
        return super.buildLoginResponse(user, menus, MENU_ADAPTER, "invest", accessToken, refreshToken);
    }

    public AuthCurrentUserResponse buildCurrentUserResponse(InvestUser user) {
        List<InvestMenuService.MenuItemDto> menus = investMenuService.getVisibleMenus();
        return super.buildCurrentUserResponse(user, menus, MENU_ADAPTER, "invest");
    }

    public AuthRefreshResponse buildRefreshResponse(String accessToken, String refreshToken) {
        return super.buildRefreshResponse("invest", accessToken, refreshToken);
    }
}
