package com.example.helloworld.controller.personal;

import com.example.helloworld.entity.personal.User;
import com.example.helloworld.repository.personal.UserRepository;
import com.example.helloworld.service.personal.MenuService;
import com.example.helloworld.service.personal.TokenBlacklistService;
import com.example.helloworld.util.JwtUtil;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
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
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    /**
     * 獲取當前登入用戶資訊
     */
    @GetMapping("/current-user")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        
        if (user == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("authenticated", true);
        userInfo.put("uid", user.getUid());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("displayName", user.getDisplayName());
        userInfo.put("photoUrl", user.getPhotoUrl());
        
        // 獲取用戶的菜單
        userInfo.put("menus", menuService.getVisibleMenus());

        return ResponseEntity.ok(userInfo);
    }

    /**
     * 登入
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");

            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );

            // 更新最後登入時間
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                user.setLastLoginAt(LocalDateTime.now());
                userRepository.save(user);
            }

            // 生成 Access Token
            String accessToken = jwtUtil.generatePersonalAccessToken(username);
            
            // 生成 Refresh Token（如果啟用）
            String refreshToken = jwtUtil.generatePersonalRefreshToken(username);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "登入成功");
            response.put("username", username);
            response.put("accessToken", accessToken);
            response.put("tokenType", "Bearer");
            
            // 只有當 Refresh Token 啟用時才返回
            if (refreshToken != null) {
                response.put("refreshToken", refreshToken);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "登入失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 刷新 Access Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Refresh Token 不能為空");
                return ResponseEntity.badRequest().body(error);
            }

            // 驗證 Refresh Token
            String username = jwtUtil.extractUsername(refreshToken);
            String system = jwtUtil.extractSystem(refreshToken);
            
            if (!"personal".equals(system)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "無效的 Refresh Token");
                return ResponseEntity.badRequest().body(error);
            }

            Boolean isValid = jwtUtil.validatePersonalRefreshToken(refreshToken, username);
            if (!isValid) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Refresh Token 無效或已過期");
                return ResponseEntity.badRequest().body(error);
            }

            // 生成新的 Access Token
            String newAccessToken = jwtUtil.generatePersonalAccessToken(username);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("accessToken", newAccessToken);
            response.put("tokenType", "Bearer");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "刷新 Token 失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 登出（將 Token 加入黑名單）
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        // 從 Authorization header 提取 Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // 將 Token 加入黑名單
            tokenBlacklistService.addToBlacklist(token);
        }
        
        // 清除 SecurityContext
        SecurityContextHolder.clearContext();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "登出成功");
        return ResponseEntity.ok(response);
    }

    /**
     * 註冊新用戶（可選功能）
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> registerRequest) {
        try {
            String username = registerRequest.get("username");
            String email = registerRequest.get("email");
            String password = registerRequest.get("password");
            String displayName = registerRequest.get("displayName");

            // 檢查用戶名是否已存在
            if (userRepository.findByUsername(username).isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "用戶名已存在");
                return ResponseEntity.badRequest().body(response);
            }

            // 檢查郵箱是否已存在
            if (userRepository.findByEmail(email).isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "郵箱已被註冊");
                return ResponseEntity.badRequest().body(response);
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

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "註冊成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "註冊失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
