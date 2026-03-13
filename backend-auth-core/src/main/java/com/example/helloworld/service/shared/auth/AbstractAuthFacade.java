package com.example.helloworld.service.shared.auth;

import com.example.helloworld.dto.common.auth.AuthCurrentUserResponse;
import com.example.helloworld.dto.common.auth.AuthLoginResponse;
import com.example.helloworld.dto.common.auth.AuthMenuItem;
import com.example.helloworld.dto.common.auth.AuthRefreshResponse;
import com.example.helloworld.dto.common.auth.AuthUserProfile;

import java.util.Collections;
import java.util.List;

public abstract class AbstractAuthFacade<U, R, P, M> implements AuthFacade<U, R, P, M> {
    private final AuthUserProfileAssembler<U, R, P> assembler;

    protected AbstractAuthFacade(AuthUserProfileAssembler<U, R, P> assembler) {
        this.assembler = assembler;
    }

    @Override
    public AuthLoginResponse buildLoginResponse(U user, List<M> menus, AuthMenuMapper.MenuAdapter<M> menuAdapter, String systemCode, String accessToken, String refreshToken) {
        AuthLoginResponse response = new AuthLoginResponse();
        AuthUserProfile profile = assembler.assemble(user, systemCode, mapMenus(menus, menuAdapter));
        copyProfile(response, profile);
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        return response;
    }

    @Override
    public AuthCurrentUserResponse buildCurrentUserResponse(U user, List<M> menus, AuthMenuMapper.MenuAdapter<M> menuAdapter, String systemCode) {
        AuthCurrentUserResponse response = new AuthCurrentUserResponse();
        AuthUserProfile profile = assembler.assemble(user, systemCode, mapMenus(menus, menuAdapter));
        copyProfile(response, profile);
        return response;
    }

    @Override
    public AuthRefreshResponse buildRefreshResponse(String systemCode, String accessToken, String refreshToken) {
        AuthRefreshResponse response = new AuthRefreshResponse();
        response.setAuthenticated(true);
        response.setSystemCode(systemCode);
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        return response;
    }

    protected <T> List<AuthMenuItem> mapMenus(List<T> menus, AuthMenuMapper.MenuAdapter<T> adapter) {
        if (menus == null || adapter == null) {
            return Collections.emptyList();
        }
        return new AuthMenuMapper<T>().map(menus, adapter);
    }

    protected void copyProfile(AuthUserProfile target, AuthUserProfile source) {
        target.setAuthenticated(source.isAuthenticated());
        target.setUid(source.getUid());
        target.setUsername(source.getUsername());
        target.setEmail(source.getEmail());
        target.setDisplayName(source.getDisplayName());
        target.setPhotoUrl(source.getPhotoUrl());
        target.setSystemCode(source.getSystemCode());
        target.setRoles(source.getRoles());
        target.setPermissions(source.getPermissions());
        target.setMenus(source.getMenus());
    }
}
