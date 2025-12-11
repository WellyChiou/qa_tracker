package com.example.helloworld.filter;

import com.example.helloworld.entity.personal.UrlPermission;
import com.example.helloworld.entity.church.ChurchUrlPermission;
import com.example.helloworld.service.personal.UrlPermissionService;
import com.example.helloworld.service.church.ChurchUrlPermissionService;
import com.example.helloworld.service.church.ChurchPermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class UrlPermissionFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(UrlPermissionFilter.class);

    @Autowired
    private UrlPermissionService urlPermissionService;

    @Autowired(required = false)
    private ChurchUrlPermissionService churchUrlPermissionService;

    @Autowired(required = false)
    private ChurchPermissionService churchPermissionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        String httpMethod = request.getMethod();
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 獲取所有啟用的 URL 權限配置
        List<UrlPermission> permissions = new ArrayList<>();
        
        // 如果是教會 API，優先檢查教會資料庫的配置
        if (requestPath.startsWith("/api/church/")) {
            // 如果教會認證 API，直接通過（必須公開，否則無法登入）
            if (requestPath.startsWith("/api/church/auth/")) {
                filterChain.doFilter(request, response);
                return;
            }
            
            if (churchUrlPermissionService != null) {
                try {
                    List<ChurchUrlPermission> churchPermissions = churchUrlPermissionService.getAllActivePermissions();
                    boolean foundMatch = false;
                    
                    // 檢查教會資料庫的 URL 權限配置
                    boolean foundExactMatch = false; // 是否找到完全匹配（URL + HTTP 方法）
                    
                    for (ChurchUrlPermission cp : churchPermissions) {
                        boolean urlMatches = matchesUrlPattern(requestPath, cp.getUrlPattern());
                        
                        if (urlMatches) {
                            // 檢查 HTTP 方法
                            boolean httpMethodMatches = true;
                            if (cp.getHttpMethod() != null && !cp.getHttpMethod().isEmpty()) {
                                httpMethodMatches = cp.getHttpMethod().equalsIgnoreCase(httpMethod);
                                if (!httpMethodMatches) {
                                    continue; // 跳過這個配置，繼續檢查下一個
                                }
                            }
                            
                            // 如果 URL 和 HTTP 方法都匹配，標記為找到完全匹配
                            foundExactMatch = true;
                            foundMatch = true;

                            // 如果是公開的，設置匿名認證以通過 Spring Security 的 authenticated() 檢查
                            if (Boolean.TRUE.equals(cp.getIsPublic())) {
                                // 設置匿名認證，讓 Spring Security 的 authenticated() 檢查通過
                                // 這樣可以避免 SecurityConfig 中的 .authenticated() 規則攔截
                                if (authentication == null || 
                                    authentication instanceof AnonymousAuthenticationToken || 
                                    !authentication.isAuthenticated()) {
                                    List<GrantedAuthority> authorities = new ArrayList<>();
                                    authorities.add(new SimpleGrantedAuthority("ROLE_PUBLIC"));
                                    Authentication publicAuth = new UsernamePasswordAuthenticationToken(
                                        "public", null, authorities);
                                    SecurityContextHolder.getContext().setAuthentication(publicAuth);
                                }
                                filterChain.doFilter(request, response);
                                return;
                            }

                            // 檢查認證狀態
                            if (authentication == null || 
                                authentication instanceof AnonymousAuthenticationToken || 
                                !authentication.isAuthenticated()) {
                                sendUnauthorizedResponse(request, response);
                                return;
                            }

                            // 檢查角色
                            if (cp.getRequiredRole() != null && !cp.getRequiredRole().isEmpty()) {
                                Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
                                boolean hasRole = authorities.stream()
                                    .anyMatch(auth -> auth.getAuthority().equals(cp.getRequiredRole()));
                                
                                if (!hasRole) {
                                    sendForbiddenResponse(request, response);
                                    return;
                                }
                            }

                            // 檢查權限（如果需要）
                            if (cp.getRequiredPermission() != null && !cp.getRequiredPermission().isEmpty()) {
                                Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
                                
                                // required_permission 存儲的是權限代碼（CODE），需要加上 PERM_ 前綴
                                String permissionCode = cp.getRequiredPermission();
                                // 如果已經有 PERM_ 前綴，移除它（避免重複）
                                if (permissionCode.startsWith("PERM_")) {
                                    permissionCode = permissionCode.substring(5);
                                }
                                final String permissionCodeToCheck = "PERM_" + permissionCode;
                                
                                boolean hasPermission = authorities.stream()
                                    .anyMatch(auth -> auth.getAuthority().equals(permissionCodeToCheck));
                                
                                if (!hasPermission) {
                                    sendForbiddenResponse(request, response);
                                    return;
                                }
                            }

                            // 通過所有檢查，繼續過濾鏈
                            filterChain.doFilter(request, response);
                            return;
                        }
                    }
                    
                    // 如果找到了匹配的 URL 模式但 HTTP 方法不匹配，拒絕請求
                    // 因為這表示資料庫中有配置但 HTTP 方法不對，應該拒絕而不是讓 SecurityConfig 處理
                    if (foundMatch && !foundExactMatch) {
                        sendForbiddenResponse(request, response);
                        return;
                    }
                } catch (Exception e) {
                    // 如果查詢資料庫失敗，記錄錯誤但繼續處理
                    log.error("❌ [UrlPermissionFilter] 查詢教會 URL 權限配置失敗", e);
                }
            }
            
            // 如果資料庫中沒有找到匹配的規則，讓 SecurityConfig 處理（需要認證）
            // 注意：這意味著如果資料庫中沒有配置，預設需要認證
            filterChain.doFilter(request, response);
            return;
        }
        
        // 獲取主資料庫的 URL 權限配置
        permissions = urlPermissionService.getAllActivePermissions();

        // 按順序查找匹配的權限配置
        for (UrlPermission permission : permissions) {
            if (matchesUrlPattern(requestPath, permission.getUrlPattern())) {
                // 檢查 HTTP 方法
                if (permission.getHttpMethod() != null && 
                    !permission.getHttpMethod().isEmpty() && 
                    !permission.getHttpMethod().equalsIgnoreCase(httpMethod)) {
                    continue;
                }

                // 如果是公開的，設置匿名認證以通過 Spring Security 的 authenticated() 檢查
                if (Boolean.TRUE.equals(permission.getIsPublic())) {
                    // 設置匿名認證，讓 Spring Security 的 authenticated() 檢查通過
                    if (authentication == null || 
                        authentication instanceof AnonymousAuthenticationToken || 
                        !authentication.isAuthenticated()) {
                        List<GrantedAuthority> authorities = new ArrayList<>();
                        authorities.add(new SimpleGrantedAuthority("ROLE_PUBLIC"));
                        Authentication publicAuth = new UsernamePasswordAuthenticationToken(
                            "public", null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(publicAuth);
                    }
                    filterChain.doFilter(request, response);
                    return;
                }

                // 檢查認證狀態
                if (authentication == null || 
                    authentication instanceof AnonymousAuthenticationToken || 
                    !authentication.isAuthenticated()) {
                    sendUnauthorizedResponse(request, response);
                    return;
                }

                // 檢查角色
                if (permission.getRequiredRole() != null && !permission.getRequiredRole().isEmpty()) {
                    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
                    boolean hasRole = authorities.stream()
                        .anyMatch(auth -> auth.getAuthority().equals(permission.getRequiredRole()));
                    
                    if (!hasRole) {
                        sendForbiddenResponse(request, response);
                        return;
                    }
                }

                // 檢查權限（如果需要）
                if (permission.getRequiredPermission() != null && !permission.getRequiredPermission().isEmpty()) {
                    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
                    String permissionCode = "PERM_" + permission.getRequiredPermission();
                    boolean hasPermission = authorities.stream()
                        .anyMatch(auth -> auth.getAuthority().equals(permissionCode));
                    
                    if (!hasPermission) {
                        sendForbiddenResponse(request, response);
                        return;
                    }
                }

                // 通過檢查，繼續過濾鏈
                filterChain.doFilter(request, response);
                return;
            }
        }

        // 如果數據庫中沒有匹配的規則，讓 SecurityConfig 的靜態配置處理
        // 這樣可以保持向後兼容性
        filterChain.doFilter(request, response);
    }

    /**
     * 檢查 URL 是否匹配模式（支持通配符 *）
     */
    private boolean matchesUrlPattern(String url, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return false;
        }
        
        // 將 Spring Security 的 URL 模式轉換為正則表達式
        String regex = pattern
            .replace(".", "\\.")
            .replace("**", ".*")
            .replace("*", "[^/]*");
        
        return Pattern.matches(regex, url);
    }

    /**
     * 發送未授權響應
     */
    private void sendUnauthorizedResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getRequestURI().startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"未授權\",\"authenticated\":false}");
        } else {
            // 前端已改為 Vue SPA，重定向到根路徑讓 Vue Router 處理
            response.sendRedirect("/");
        }
    }

    /**
     * 發送禁止訪問響應
     */
    private void sendForbiddenResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getRequestURI().startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"權限不足\",\"forbidden\":true}");
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "權限不足");
        }
    }
}

