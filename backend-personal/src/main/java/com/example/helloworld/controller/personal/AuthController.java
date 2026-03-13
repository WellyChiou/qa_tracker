package com.example.helloworld.controller.personal;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.auth.AuthCurrentUserResponse;
import com.example.helloworld.dto.common.auth.AuthLoginResponse;
import com.example.helloworld.dto.common.auth.AuthLogoutResponse;
import com.example.helloworld.dto.common.auth.AuthMenuItem;
import com.example.helloworld.dto.common.auth.AuthRefreshResponse;
import com.example.helloworld.dto.common.auth.AuthUserProfile;
import com.example.helloworld.entity.personal.Permission;
import com.example.helloworld.entity.personal.Role;
import com.example.helloworld.entity.personal.User;
import com.example.helloworld.repository.personal.UserRepository;
import com.example.helloworld.service.personal.MenuService;
import com.example.helloworld.service.common.TokenBlacklistService;
import com.example.helloworld.util.PersonalJwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/personal/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("defaultAuthenticationManager")
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MenuService menuService;

    @Autowired
    private PersonalJwtUtil jwtUtil;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    /**
     * 獲取當前登入用戶資訊
     */
    @GetMapping("/current-user")
    public ResponseEntity<ApiResponse<AuthCurrentUserResponse>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AuthCurrentUserResponse response = new AuthCurrentUserResponse();

        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal().equals("anonymousUser")) {
            response.setAuthenticated(false);
            response.setSystemCode("personal");
            return ResponseEntity.ok(ApiResponse.ok(response));
        }

        String username = authentication.getName();
        User user = userRepository.findByUsernameWithRolesAndPermissions(username).orElse(null);
        populateUserProfile(response, user);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 登入
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthLoginResponse>> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");

            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );

            // 取得用戶資料（包含角色與權限），並更新最後登入時間
            User user = userRepository.findByUsernameWithRolesAndPermissions(username).orElse(null);
            if (user != null) {
                user.setLastLoginAt(LocalDateTime.now());
                userRepository.save(user);
            }

            // 生成 Access Token
            String accessToken = jwtUtil.generatePersonalAccessToken(username);
            
            // 生成 Refresh Token（如果啟用）
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("登入失敗: 無法取得用戶資料"));
            }
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String refreshToken = jwtUtil.generatePersonalRefreshToken(username);
            AuthLoginResponse response = new AuthLoginResponse();
            populateUserProfile(response, user);
            response.setAccessToken(accessToken);
            response.setTokenType("Bearer");
            if (refreshToken != null) {
                response.setRefreshToken(refreshToken);
            }

            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("登入失敗: " + e.getMessage()));
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
                return ResponseEntity.badRequest().body(ApiResponse.fail("Refresh Token 不能為空"));
            }

            // 驗證 Refresh Token
            String username = jwtUtil.extractUsername(refreshToken);
            String system = jwtUtil.extractSystem(refreshToken);
            
            if (!"personal".equals(system)) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("無效的 Refresh Token"));
            }

            Boolean isValid = jwtUtil.validatePersonalRefreshToken(refreshToken, username);
            if (!isValid) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Refresh Token 無效或已過期"));
            }

            // 生成新的 Access Token
            String newAccessToken = jwtUtil.generatePersonalAccessToken(username);
            AuthRefreshResponse response = new AuthRefreshResponse();
            response.setAuthenticated(true);
            response.setSystemCode("personal");
            response.setAccessToken(newAccessToken);
            response.setRefreshToken(refreshToken);
            response.setTokenType("Bearer");

            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刷新 Token 失敗: " + e.getMessage()));
        }
    }

    /**
     * 登出（將 Token 加入黑名單）
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<AuthLogoutResponse>> logout(HttpServletRequest request) {
        // 從 Authorization header 提取 Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // 將 Token 加入黑名單
            tokenBlacklistService.addToBlacklist(token);
        }
        
        // 清除 SecurityContext
        SecurityContextHolder.clearContext();
        
        return ResponseEntity.ok(ApiResponse.ok(new AuthLogoutResponse(true, "登出成功")));
    }

    private void populateUserProfile(AuthUserProfile profile, User user) {
        profile.setSystemCode("personal");
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
        profile.setMenus(convertMenuItems(menuService.getVisibleMenus()));
    }

    private List<String> extractRoleNames(User user) {
        if (user == null || user.getRoles() == null) {
            return new ArrayList<>();
        }
        return user.getRoles().stream()
            .map(Role::getRoleName)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    private List<String> extractPermissionCodes(User user) {
        if (user == null) {
            return new ArrayList<>();
        }
        Set<String> permissionCodes = new LinkedHashSet<>();
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                if (role == null || role.getPermissions() == null) {
                    continue;
                }
                for (Permission permission : role.getPermissions()) {
                    if (permission != null && permission.getPermissionCode() != null) {
                        permissionCodes.add(permission.getPermissionCode());
                    }
                }
            }
        }
        if (user.getPermissions() != null) {
            for (Permission permission : user.getPermissions()) {
                if (permission != null && permission.getPermissionCode() != null) {
                    permissionCodes.add(permission.getPermissionCode());
                }
            }
        }
        return new ArrayList<>(permissionCodes);
    }

    private List<AuthMenuItem> convertMenuItems(List<MenuService.MenuItemDTO> menuItems) {
        if (menuItems == null) {
            return new ArrayList<>();
        }
        return menuItems.stream()
            .filter(java.util.Objects::nonNull)
            .map(this::mapMenuItem)
            .collect(Collectors.toList());
    }

    private AuthMenuItem mapMenuItem(MenuService.MenuItemDTO menuItem) {
        AuthMenuItem node = new AuthMenuItem();
        node.setId(menuItem.getId());
        node.setMenuCode(menuItem.getMenuCode());
        node.setMenuName(menuItem.getMenuName());
        node.setIcon(menuItem.getIcon());
        node.setUrl(menuItem.getUrl());
        node.setOrderIndex(menuItem.getOrderIndex());
        node.setShowInDashboard(menuItem.getShowInDashboard());
        node.setChildren(convertMenuItems(menuItem.getChildren()));
        return node;
    }

    /**
     * 註冊新用戶（可選功能）
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody Map<String, String> registerRequest) {
        try {
            String username = registerRequest.get("username");
            String email = registerRequest.get("email");
            String password = registerRequest.get("password");
            String displayName = registerRequest.get("displayName");

            // 檢查用戶名是否已存在
            if (userRepository.findByUsername(username).isPresent()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("用戶名已存在"));
            }

            // 檢查郵箱是否已存在
            if (userRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("郵箱已被註冊"));
            }

            // 創建新用戶
            User user = new User();
            user.setUid(UUID.randomUUID().toString());
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setDisplayName(displayName != null ? displayName : username);
            user.setProviderId("local");
            user.setIsEnabled(true);
            user.setIsAccountNonLocked(true);

            userRepository.save(user);

            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("註冊失敗: " + e.getMessage()));
        }
    }
}
