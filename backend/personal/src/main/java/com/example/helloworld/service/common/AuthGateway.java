package com.example.helloworld.service.common;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthGateway {
    private final Map<AuthDomain, JwtTokenGateway> jwtTokenGateways;
    private final UserDetailsService personalUserDetailsService;

    public AuthGateway(
            List<JwtTokenGateway> jwtTokenGateways,
            UserDetailsService personalUserDetailsService) {
        this.jwtTokenGateways = jwtTokenGateways.stream()
                .filter(gateway -> gateway.domain() == AuthDomain.PERSONAL)
                .collect(Collectors.toMap(JwtTokenGateway::domain, gateway -> gateway));
        this.personalUserDetailsService = personalUserDetailsService;
    }

    public boolean requiresAuthentication(String requestPath) {
        if (requestPath == null) {
            return false;
        }

        boolean isPersonalPath = requestPath.startsWith("/api/personal/")
                && !requestPath.startsWith("/api/personal/auth/login")
                && !requestPath.startsWith("/api/personal/auth/refresh")
                && !requestPath.startsWith("/api/personal/auth/register")
                && !requestPath.startsWith("/api/personal/line/");

        return isPersonalPath;
    }

    public String extractUsername(AuthDomain domain, String jwt) {
        return jwtGateway(domain).extractUsername(jwt);
    }

    public String extractSystem(AuthDomain domain, String jwt) {
        return jwtGateway(domain).extractSystem(jwt);
    }

    public String extractTokenType(AuthDomain domain, String jwt) {
        return jwtGateway(domain).extractTokenType(jwt);
    }

    public boolean validateAccessToken(AuthDomain domain, String jwt, String username) {
        return jwtGateway(domain).validateAccessToken(jwt, username);
    }

    public UserDetails loadUserDetails(AuthDomain domain, String username) {
        return userDetailsService(domain).loadUserByUsername(username);
    }

    public UserDetailsService userDetailsService(AuthDomain domain) {
        if (domain != AuthDomain.PERSONAL) {
            throw new IllegalArgumentException("Personal backend does not serve auth domain: " + domain);
        }
        return personalUserDetailsService;
    }

    private JwtTokenGateway jwtGateway(AuthDomain domain) {
        JwtTokenGateway gateway = jwtTokenGateways.get(domain);
        if (gateway == null) {
            throw new IllegalStateException("No JwtTokenGateway registered for domain: " + domain);
        }
        return gateway;
    }
}
