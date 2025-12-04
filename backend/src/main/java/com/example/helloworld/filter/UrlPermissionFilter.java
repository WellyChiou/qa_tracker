package com.example.helloworld.filter;

import com.example.helloworld.entity.UrlPermission;
import com.example.helloworld.service.UrlPermissionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class UrlPermissionFilter extends OncePerRequestFilter {

    @Autowired
    private UrlPermissionService urlPermissionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        String httpMethod = request.getMethod();
        
        // 教會網站 API 是公開的，直接跳過權限檢查
        if (requestPath.startsWith("/api/church/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 獲取所有啟用的 URL 權限配置
        List<UrlPermission> permissions = urlPermissionService.getAllActivePermissions();

        // 按順序查找匹配的權限配置
        for (UrlPermission permission : permissions) {
            if (matchesUrlPattern(requestPath, permission.getUrlPattern())) {
                // 檢查 HTTP 方法
                if (permission.getHttpMethod() != null && 
                    !permission.getHttpMethod().isEmpty() && 
                    !permission.getHttpMethod().equalsIgnoreCase(httpMethod)) {
                    continue;
                }

                // 如果是公開的，直接通過
                if (Boolean.TRUE.equals(permission.getIsPublic())) {
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

