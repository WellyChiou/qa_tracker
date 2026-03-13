package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.auth.AuthCurrentUserResponse;
import com.example.helloworld.dto.common.auth.AuthLoginResponse;
import com.example.helloworld.dto.common.auth.AuthLogoutResponse;
import com.example.helloworld.dto.common.auth.AuthRefreshResponse;
import com.example.helloworld.entity.church.ChurchUser;
import com.example.helloworld.repository.church.ChurchUserRepository;
import com.example.helloworld.service.church.ChurchAuthFacade;
import com.example.helloworld.service.common.TokenBlacklistService;
import com.example.helloworld.util.ChurchJwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;

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
    private ChurchAuthFacade churchAuthFacade;

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

            if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getPrincipal().equals("anonymousUser")) {
                AuthCurrentUserResponse fallback = new AuthCurrentUserResponse();
                fallback.setAuthenticated(false);
                fallback.setSystemCode("church_admin");
                return ResponseEntity.ok(ApiResponse.ok(fallback));
            }

            String username = authentication.getName();
            ChurchUser user = churchUserRepository.findByUsernameWithRolesAndPermissions(username).orElse(null);
            AuthCurrentUserResponse response = churchAuthFacade.buildCurrentUserResponse(user);
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
            AuthLoginResponse response = churchAuthFacade.buildLoginResponse(user, accessToken, refreshToken);
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
            AuthRefreshResponse response = churchAuthFacade.buildRefreshResponse(newAccessToken, refreshToken);
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

}
