package com.example.helloworld.service.shared.auth;

import com.example.helloworld.dto.common.auth.AuthCurrentUserResponse;
import com.example.helloworld.dto.common.auth.AuthLoginResponse;
import com.example.helloworld.dto.common.auth.AuthRefreshResponse;

import java.util.List;

public interface AuthFacade<U, R, P, M> {
    AuthLoginResponse buildLoginResponse(U user, List<M> menus, AuthMenuMapper.MenuAdapter<M> menuAdapter, String systemCode, String accessToken, String refreshToken);

    AuthCurrentUserResponse buildCurrentUserResponse(U user, List<M> menus, AuthMenuMapper.MenuAdapter<M> menuAdapter, String systemCode);

    AuthRefreshResponse buildRefreshResponse(String systemCode, String accessToken, String refreshToken);
}
