package com.example.helloworld.filter;

import com.example.helloworld.service.common.AuthDomain;
import com.example.helloworld.service.common.CommonUrlPermission;
import com.example.helloworld.service.common.UrlPermissionGateway;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class UrlPermissionFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(UrlPermissionFilter.class);

    private final Map<AuthDomain, UrlPermissionGateway> urlPermissionGateways;

    @Autowired
    public UrlPermissionFilter(List<UrlPermissionGateway> urlPermissionGateways) {
        this.urlPermissionGateways = new EnumMap<>(AuthDomain.class);
        for (UrlPermissionGateway gateway : urlPermissionGateways) {
            this.urlPermissionGateways.put(gateway.domain(), gateway);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        String httpMethod = request.getMethod();
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 如果是教會 API，優先檢查教會資料庫的配置
        if (requestPath.startsWith("/api/church/")) {
            // 如果教會認證 API，直接通過（必須公開，否則無法登入）
            if (requestPath.startsWith("/api/church/auth/")) {
                filterChain.doFilter(request, response);
                return;
            }
            
            try {
                if (handlePermissions(
                        getActivePermissions(AuthDomain.CHURCH),
                        request,
                        response,
                        filterChain,
                        authentication,
                        httpMethod,
                        true)) {
                    return;
                }
            } catch (Exception e) {
                log.error("❌ [UrlPermissionFilter] 查詢教會 URL 權限配置失敗", e);
            }
            
            // 如果資料庫中沒有找到匹配的規則，讓 SecurityConfig 處理（需要認證）
            // 注意：這意味著如果資料庫中沒有配置，預設需要認證
            filterChain.doFilter(request, response);
            return;
        }
        
        if (handlePermissions(
                getActivePermissions(AuthDomain.PERSONAL),
                request,
                response,
                filterChain,
                authentication,
                httpMethod,
                false)) {
            return;
        }

        // 如果數據庫中沒有匹配的規則，讓 SecurityConfig 的靜態配置處理
        // 這樣可以保持向後兼容性
        filterChain.doFilter(request, response);
    }

    private List<CommonUrlPermission> getActivePermissions(AuthDomain domain) {
        UrlPermissionGateway gateway = urlPermissionGateways.get(domain);
        if (gateway == null) {
            return List.of();
        }
        return gateway.getActivePermissions();
    }

    private boolean handlePermissions(
            List<CommonUrlPermission> permissions,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain,
            Authentication authentication,
            String httpMethod,
            boolean normalizePermissionCode) throws IOException, ServletException {
        String requestPath = request.getRequestURI();
        boolean foundUrlMatch = false;
        boolean foundExactMatch = false;

        for (CommonUrlPermission permission : permissions) {
            if (!matchesUrlPattern(requestPath, permission.urlPattern())) {
                continue;
            }

            foundUrlMatch = true;
            if (permission.httpMethod() != null
                    && !permission.httpMethod().isEmpty()
                    && !permission.httpMethod().equalsIgnoreCase(httpMethod)) {
                continue;
            }

            foundExactMatch = true;

            if (Boolean.TRUE.equals(permission.isPublic())) {
                ensurePublicAuthentication(authentication);
                filterChain.doFilter(request, response);
                return true;
            }

            if (authentication == null
                    || authentication instanceof AnonymousAuthenticationToken
                    || !authentication.isAuthenticated()) {
                sendUnauthorizedResponse(request, response);
                return true;
            }

            if (permission.requiredRole() != null && !permission.requiredRole().isEmpty()) {
                boolean hasRole = authentication.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals(permission.requiredRole()));
                if (!hasRole) {
                    sendForbiddenResponse(request, response);
                    return true;
                }
            }

            if (permission.requiredPermission() != null && !permission.requiredPermission().isEmpty()) {
                String permissionCode = permission.requiredPermission();
                if (normalizePermissionCode && permissionCode.startsWith("PERM_")) {
                    permissionCode = permissionCode.substring(5);
                }
                final String authorityToCheck = permissionCode.startsWith("PERM_")
                        ? permissionCode
                        : "PERM_" + permissionCode;

                boolean hasPermission = authentication.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals(authorityToCheck));
                if (!hasPermission) {
                    sendForbiddenResponse(request, response);
                    return true;
                }
            }

            filterChain.doFilter(request, response);
            return true;
        }

        if (foundUrlMatch && !foundExactMatch) {
            sendForbiddenResponse(request, response);
            return true;
        }

        return false;
    }

    private void ensurePublicAuthentication(Authentication authentication) {
        if (authentication != null
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.isAuthenticated()) {
            return;
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_PUBLIC"));
        Authentication publicAuth = new UsernamePasswordAuthenticationToken("public", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(publicAuth);
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
