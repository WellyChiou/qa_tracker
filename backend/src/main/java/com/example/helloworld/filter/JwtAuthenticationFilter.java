package com.example.helloworld.filter;

import com.example.helloworld.service.personal.CustomUserDetailsService;
import com.example.helloworld.service.personal.TokenBlacklistService;
import com.example.helloworld.service.church.ChurchUserDetailsService;
import com.example.helloworld.util.ChurchJwtUtil;
import com.example.helloworld.util.PersonalJwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private ChurchJwtUtil churchJwtUtil;

    @Autowired
    private PersonalJwtUtil personalJwtUtil;

    @Autowired
    private CustomUserDetailsService personalUserDetailsService;

    @Autowired
    private ChurchUserDetailsService churchUserDetailsService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String requestPath = request.getRequestURI();
        
        // 只處理需要 JWT 認證的路徑
        boolean isChurchPath = requestPath.startsWith("/api/church/");
        boolean isPersonalPath = requestPath.startsWith("/api/personal/") && 
                                 !isChurchPath &&
                                 !requestPath.startsWith("/api/personal/auth/login") &&
                                 !requestPath.startsWith("/api/personal/auth/register") &&
                                 !requestPath.startsWith("/api/personal/line/");
        
        // 如果路徑不需要認證，直接放行
        if (!isChurchPath && !isPersonalPath) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = null;
        String username = null;
        String system = null;

        // 從 Authorization header 提取 Token
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                if (isChurchPath) {
                    username = churchJwtUtil.extractUsername(jwt);
                    system = churchJwtUtil.extractSystem(jwt);
                } else {
                    username = personalJwtUtil.extractUsername(jwt);
                    system = personalJwtUtil.extractSystem(jwt);
                }
            } catch (Exception e) {
                // Token 無效，繼續處理（讓後續的 Security Filter 處理）
            }
        }

        // 驗證 Token 和系統類型，並載入用戶權限
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String expectedSystem = isChurchPath ? "church" : "personal";
            
            // 檢查 Token 是否在黑名單中
            if (tokenBlacklistService.isBlacklisted(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }
            
            if (system != null && system.equals(expectedSystem)) {
                // 只接受 Access Token（不接受 Refresh Token）
                String tokenType = isChurchPath ? churchJwtUtil.extractTokenType(jwt) : personalJwtUtil.extractTokenType(jwt);
                if (!"access".equals(tokenType)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                
                Boolean isValid = isChurchPath 
                    ? churchJwtUtil.validateChurchAccessToken(jwt, username)
                    : personalJwtUtil.validatePersonalAccessToken(jwt, username);
                
                if (isValid) {
                    try {
                        // 從對應的 UserDetailsService 載入用戶信息和權限
                        UserDetails userDetails = isChurchPath
                            ? churchUserDetailsService.loadUserByUsername(username)
                            : personalUserDetailsService.loadUserByUsername(username);
                        
                        UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } catch (Exception e) {
                        // 載入用戶失敗，繼續處理（讓後續的 Security Filter 處理）
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}

