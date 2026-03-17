package com.example.helloworld.filter;

import com.example.helloworld.service.common.AuthDomain;
import com.example.helloworld.service.common.AuthGateway;
import com.example.helloworld.service.common.TokenBlacklistService;
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
    private AuthGateway authGateway;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String requestPath = request.getRequestURI();
        if (!authGateway.requiresAuthentication(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthDomain domain = AuthDomain.fromRequestPath(requestPath);

        String jwt = null;
        String username = null;
        String system = null;

        // 從 Authorization header 提取 Token
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                username = authGateway.extractUsername(domain, jwt);
                system = authGateway.extractSystem(domain, jwt);
            } catch (Exception e) {
                // Token 無效，繼續處理（讓後續的 Security Filter 處理）
            }
        }

        // 驗證 Token 和系統類型，並載入用戶權限
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String expectedSystem = domain.value();
            
            // 檢查 Token 是否在黑名單中
            if (tokenBlacklistService.isBlacklisted(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }
            
            if (system != null && system.equals(expectedSystem)) {
                // 只接受 Access Token（不接受 Refresh Token）
                String tokenType = authGateway.extractTokenType(domain, jwt);
                if (!"access".equals(tokenType)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                
                Boolean isValid = authGateway.validateAccessToken(domain, jwt, username);
                
                if (isValid) {
                    try {
                        UserDetails userDetails = authGateway.loadUserDetails(domain, username);
                        
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
