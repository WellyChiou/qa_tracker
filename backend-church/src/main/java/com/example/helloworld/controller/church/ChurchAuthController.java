package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.auth.AuthCurrentUserResponse;
import com.example.helloworld.dto.common.auth.AuthLoginResponse;
import com.example.helloworld.dto.common.auth.AuthLogoutResponse;
import com.example.helloworld.dto.common.auth.AuthMenuItem;
import com.example.helloworld.dto.common.auth.AuthRefreshResponse;
import com.example.helloworld.dto.common.auth.AuthUserProfile;
import com.example.helloworld.entity.church.ChurchPermission;
import com.example.helloworld.entity.church.ChurchRole;
import com.example.helloworld.entity.church.ChurchUser;
import com.example.helloworld.repository.church.ChurchUserRepository;
import com.example.helloworld.service.common.TokenBlacklistService;
import com.example.helloworld.service.church.ChurchMenuService;
import com.example.helloworld.util.ChurchJwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/church/auth")
@CrossOrigin(origins = "*")
public class ChurchAuthController {

    @Autowired
    @Qualifier("churchAuthenticationManager")
    private AuthenticationManager churchAuthenticationManager;

    @Autowired
    private ChurchUserRepository churchUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ChurchMenuService churchMenuService;

    @Autowired
    private ChurchJwtUtil jwtUtil;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    /**
     * 獲取當前登入用戶資訊
     */
    @GetMapping("/current-user")
    public ResponseEntity<ApiResponse<AuthCurrentUserResponse>> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            AuthCurrentUserResponse response = new AuthCurrentUserResponse();

            if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getPrincipal().equals("anonymousUser")) {
                response.setAuthenticated(false);
                response.setSystemCode("church_admin");
                return ResponseEntity.ok(ApiResponse.ok(response));
            }

            String username = authentication.getName();
            ChurchUser user = churchUserRepository.findByUsernameWithRolesAndPermissions(username).orElse(null);

            if (user == null) {
                response.setAuthenticated(false);
                response.setSystemCode("church_admin");
                return ResponseEntity.ok(ApiResponse.ok(response));
            }

            populateUserProfile(response, user);
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取用戶資訊失敗: " + e.getMessage()));
        }
    }

    /**
     * 登入
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthLoginResponse>> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");

            Authentication authentication = churchAuthenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );

            // 更新最後登入時間
            ChurchUser user = churchUserRepository.findByUsernameWithRolesAndPermissions(username).orElse(null);
            if (user != null) {
                user.setLastLoginAt(LocalDateTime.now());
                churchUserRepository.save(user);
            }

            // 生成 Access Token
            String accessToken = jwtUtil.generateChurchAccessToken(username);
            
            // 生成 Refresh Token（如果啟用）
            String refreshToken = jwtUtil.generateChurchRefreshToken(username);

            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("登入失敗: 無法取得用戶資料"));
            }

            SecurityContextHolder.getContext().setAuthentication(authentication);
            AuthLoginResponse response = new AuthLoginResponse();
            populateUserProfile(response, user);
            response.setAccessToken(accessToken);
            response.setTokenType("Bearer");
            if (refreshToken != null) {
                response.setRefreshToken(refreshToken);
            }

            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("登入失敗: 用戶名或密碼錯誤"));
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("登入失敗: 用戶不存在"));
        } catch (Exception e) {
            e.printStackTrace(); // 輸出詳細錯誤到日誌
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("登入失敗: " + e.getMessage()));
        }
    }

    /**
     * 刷新 Access Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthRefreshResponse>> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("Refresh Token 不能為空"));
            }

            // 驗證 Refresh Token
            String username = jwtUtil.extractUsername(refreshToken);
            String system = jwtUtil.extractSystem(refreshToken);
            
            if (!"church".equals(system)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("無效的 Refresh Token"));
            }

            Boolean isValid = jwtUtil.validateChurchRefreshToken(refreshToken, username);
            if (!isValid) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("Refresh Token 無效或已過期"));
            }

            // 生成新的 Access Token
            String newAccessToken = jwtUtil.generateChurchAccessToken(username);

            AuthRefreshResponse response = new AuthRefreshResponse();
            response.setAuthenticated(true);
            response.setSystemCode("church_admin");
            response.setAccessToken(newAccessToken);
            response.setRefreshToken(refreshToken);
            response.setTokenType("Bearer");

            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("刷新 Token 失敗: " + e.getMessage()));
        }
    }

    /**
     * 登出（將 Token 加入黑名單）
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<AuthLogoutResponse>> logout(HttpServletRequest request) {
        try {
            // 從 Authorization header 提取 Token
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                // 將 Token 加入黑名單
                tokenBlacklistService.addToBlacklist(token);
            }
            
            SecurityContextHolder.clearContext();
            
            return ResponseEntity.ok(ApiResponse.ok(new AuthLogoutResponse(true, "登出成功")));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("登出失敗: " + e.getMessage()));
        }
    }

    private void populateUserProfile(AuthUserProfile profile, ChurchUser user) {
        profile.setSystemCode("church_admin");
        if (user == null) {
            profile.setAuthenticated(false);
            return;
        }

        profile.setAuthenticated(true);
        profile.setUid(user.getUid());
        profile.setUsername(user.getUsername());
        profile.setEmail(user.getEmail());
        profile.setDisplayName(user.getDisplayName());
        profile.setPhotoUrl(user.getPhotoUrl());
        profile.setRoles(extractRoleNames(user));
        profile.setPermissions(extractPermissionCodes(user));
        profile.setMenus(convertMenuItems(churchMenuService.getAdminMenus()));
    }

    private List<String> extractRoleNames(ChurchUser user) {
        if (user == null || user.getRoles() == null) {
            return new ArrayList<>();
        }
        return user.getRoles().stream()
            .map(ChurchRole::getRoleName)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    private List<String> extractPermissionCodes(ChurchUser user) {
        if (user == null) {
            return new ArrayList<>();
        }
        Set<String> permissionCodes = new LinkedHashSet<>();
        if (user.getRoles() != null) {
            for (ChurchRole role : user.getRoles()) {
                if (role == null || role.getPermissions() == null) {
                    continue;
                }
                for (ChurchPermission permission : role.getPermissions()) {
                    if (permission != null && permission.getPermissionCode() != null) {
                        permissionCodes.add(permission.getPermissionCode());
                    }
                }
            }
        }
        if (user.getPermissions() != null) {
            for (ChurchPermission permission : user.getPermissions()) {
                if (permission != null && permission.getPermissionCode() != null) {
                    permissionCodes.add(permission.getPermissionCode());
                }
            }
        }
        return new ArrayList<>(permissionCodes);
    }

    private List<AuthMenuItem> convertMenuItems(List<ChurchMenuService.MenuItemDTO> menuItems) {
        if (menuItems == null) {
            return new ArrayList<>();
        }
        return menuItems.stream()
            .filter(java.util.Objects::nonNull)
            .map(this::mapMenuItem)
            .collect(Collectors.toList());
    }

    private AuthMenuItem mapMenuItem(ChurchMenuService.MenuItemDTO menuItem) {
        AuthMenuItem node = new AuthMenuItem();
        node.setId(menuItem.getId());
        node.setMenuCode(menuItem.getMenuCode());
        node.setMenuName(menuItem.getMenuName());
        node.setIcon(menuItem.getIcon());
        node.setUrl(menuItem.getUrl());
        node.setOrderIndex(menuItem.getOrderIndex());
        node.setShowInDashboard(null);
        node.setChildren(convertMenuItems(menuItem.getChildren()));
        return node;
    }
}
