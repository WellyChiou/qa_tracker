package com.example.helloworld.filter;

import com.example.helloworld.service.common.AuthDomain;
import com.example.helloworld.service.common.CommonUrlPermission;
import com.example.helloworld.service.common.UrlPermissionGateway;
import com.example.helloworld.service.common.UrlPermissionPolicy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class AbstractUrlPermissionFilter extends OncePerRequestFilter {
    private static final Log log = LogFactory.getLog(AbstractUrlPermissionFilter.class);

    private final Map<AuthDomain, UrlPermissionGateway> urlPermissionGateways;
    private final UrlPermissionPolicy urlPermissionPolicy;

    protected AbstractUrlPermissionFilter(
            List<UrlPermissionGateway> urlPermissionGateways,
            UrlPermissionPolicy urlPermissionPolicy) {
        this.urlPermissionGateways = new EnumMap<>(AuthDomain.class);
        for (UrlPermissionGateway gateway : urlPermissionGateways) {
            this.urlPermissionGateways.put(gateway.domain(), gateway);
        }
        this.urlPermissionPolicy = urlPermissionPolicy;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String httpMethod = request.getMethod();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AuthDomain domain = urlPermissionPolicy.resolveDomain(requestPath);
        if (domain == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (urlPermissionPolicy.bypassForAuthPath(requestPath, domain)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (domain == AuthDomain.CHURCH) {
            try {
                if (handlePermissions(
                        getActivePermissions(domain),
                        request,
                        response,
                        filterChain,
                        authentication,
                        httpMethod,
                        urlPermissionPolicy.normalizePermissionCode(domain))) {
                    return;
                }
            } catch (Exception e) {
                log.error("❌ [UrlPermissionFilter] 查詢教會 URL 權限配置失敗", e);
            }

            filterChain.doFilter(request, response);
            return;
        }

        if (handlePermissions(
                getActivePermissions(domain),
                request,
                response,
                filterChain,
                authentication,
                httpMethod,
                urlPermissionPolicy.normalizePermissionCode(domain))) {
            return;
        }

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
                final String normalizedCode = permissionCode.startsWith("PERM_")
                        ? permissionCode.substring(5)
                        : permissionCode;

                boolean hasPermission = authentication.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals(normalizedCode));
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

    private boolean matchesUrlPattern(String url, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return false;
        }

        String regex = pattern
                .replace(".", "\\.")
                .replace("**", ".*")
                .replace("*", "[^/]*");

        return Pattern.matches(regex, url);
    }

    private void sendUnauthorizedResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getRequestURI().startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"未授權\",\"authenticated\":false}");
        } else {
            response.sendRedirect("/");
        }
    }

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
