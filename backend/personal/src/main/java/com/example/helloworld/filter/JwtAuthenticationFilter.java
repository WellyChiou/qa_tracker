package com.example.helloworld.filter;

import com.example.helloworld.service.common.AuthGateway;
import com.example.helloworld.service.common.TokenBlacklistService;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationFilter extends AbstractJwtAuthenticationFilter {
    public JwtAuthenticationFilter(AuthGateway authGateway, TokenBlacklistService tokenBlacklistService) {
        super(authGateway, tokenBlacklistService);
    }
}
