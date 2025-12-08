package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.ChurchUser;
import com.example.helloworld.repository.church.ChurchUserRepository;
import com.example.helloworld.service.church.ChurchMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        ChurchUser user = churchUserRepository.findByUsername(username).orElse(null);
        
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
        
        // 獲取後台菜單
        userInfo.put("menus", churchMenuService.getAdminMenus());

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

            Authentication authentication = churchAuthenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );

            // 設置 SecurityContext 並保存到 session
            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(authentication);
            
            // 確保 session 創建並保存 SecurityContext
            HttpSession session = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest().getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

            // 更新最後登入時間
            ChurchUser user = churchUserRepository.findByUsername(username).orElse(null);
            if (user != null) {
                user.setLastLoginAt(LocalDateTime.now());
                churchUserRepository.save(user);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "登入成功");
            response.put("username", username);

            return ResponseEntity.ok(response);
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "登入失敗: 用戶名或密碼錯誤");
            return ResponseEntity.badRequest().body(response);
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "登入失敗: 用戶不存在");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "登入失敗: " + e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            e.printStackTrace(); // 輸出詳細錯誤到日誌
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "登出成功");
        return ResponseEntity.ok(response);
    }
}

