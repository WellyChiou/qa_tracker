package com.example.helloworld.controller.invest.auth;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.auth.AuthCurrentUserResponse;
import com.example.helloworld.dto.common.auth.AuthLoginResponse;
import com.example.helloworld.dto.common.auth.AuthLogoutResponse;
import com.example.helloworld.dto.common.auth.AuthRefreshResponse;
import com.example.helloworld.entity.invest.auth.InvestUser;
import com.example.helloworld.repository.invest.auth.InvestUserRepository;
import com.example.helloworld.service.common.TokenBlacklistService;
import com.example.helloworld.service.invest.auth.InvestAuthFacade;
import com.example.helloworld.util.InvestJwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/invest/auth")
@CrossOrigin(origins = "*")
public class InvestAuthController {

    private final AuthenticationManager authenticationManager;
    private final InvestUserRepository investUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final InvestJwtUtil investJwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final InvestAuthFacade investAuthFacade;

    public InvestAuthController(
            @Qualifier("investAuthenticationManager") AuthenticationManager authenticationManager,
            InvestUserRepository investUserRepository,
            PasswordEncoder passwordEncoder,
            InvestJwtUtil investJwtUtil,
            TokenBlacklistService tokenBlacklistService,
            InvestAuthFacade investAuthFacade) {
        this.authenticationManager = authenticationManager;
        this.investUserRepository = investUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.investJwtUtil = investJwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
        this.investAuthFacade = investAuthFacade;
    }

    @GetMapping("/current-user")
    public ResponseEntity<ApiResponse<AuthCurrentUserResponse>> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                AuthCurrentUserResponse fallback = new AuthCurrentUserResponse();
                fallback.setAuthenticated(false);
                fallback.setSystemCode("invest");
                fallback.setMenus(List.of());
                return ResponseEntity.ok(ApiResponse.ok(fallback));
            }

            String username = authentication.getName();
            InvestUser user = investUserRepository.findByUsernameWithRolesAndPermissions(username).orElse(null);
            AuthCurrentUserResponse response = investAuthFacade.buildCurrentUserResponse(user);
            if (response.getMenus() == null) {
                response.setMenus(List.of());
            }
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取使用者資訊失敗: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthLoginResponse>> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");

            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );

            InvestUser user = investUserRepository.findByUsernameWithRolesAndPermissions(username).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("登入失敗: 無法取得使用者資料"));
            }

            user.setLastLoginAt(LocalDateTime.now());
            investUserRepository.save(user);

            String accessToken = investJwtUtil.generateInvestAccessToken(username);
            String refreshToken = investJwtUtil.generateInvestRefreshToken(username);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            AuthLoginResponse response = investAuthFacade.buildLoginResponse(user, accessToken, refreshToken);
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("登入失敗: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthRefreshResponse>> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Refresh Token 不能為空"));
            }

            String username = investJwtUtil.extractUsername(refreshToken);
            String system = investJwtUtil.extractSystem(refreshToken);

            if (!"invest".equals(system)) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("無效的 Refresh Token"));
            }

            Boolean isValid = investJwtUtil.validateInvestRefreshToken(refreshToken, username);
            if (!isValid) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Refresh Token 無效或已過期"));
            }

            String newAccessToken = investJwtUtil.generateInvestAccessToken(username);
            AuthRefreshResponse response = investAuthFacade.buildRefreshResponse(newAccessToken, refreshToken);
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刷新 Token 失敗: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<AuthLogoutResponse>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            tokenBlacklistService.addToBlacklist(token);
        }

        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.ok(new AuthLogoutResponse(true, "登出成功")));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody Map<String, String> registerRequest) {
        try {
            String username = registerRequest.get("username");
            String email = registerRequest.get("email");
            String password = registerRequest.get("password");
            String displayName = registerRequest.get("displayName");

            if (investUserRepository.findByUsername(username).isPresent()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("使用者名稱已存在"));
            }

            if (investUserRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Email 已被註冊"));
            }

            InvestUser user = new InvestUser();
            user.setUid(UUID.randomUUID().toString());
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setDisplayName(displayName != null ? displayName : username);
            user.setProviderId("local");
            user.setIsEnabled(true);
            user.setIsAccountNonLocked(true);

            investUserRepository.save(user);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("註冊失敗: " + e.getMessage()));
        }
    }
}
