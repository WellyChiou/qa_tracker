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
    private final UserDetailsService churchUserDetailsService;

    public AuthGateway(
            List<JwtTokenGateway> jwtTokenGateways,
            UserDetailsService churchUserDetailsService) {
        this.jwtTokenGateways = jwtTokenGateways.stream()
                .filter(gateway -> gateway.domain() == AuthDomain.CHURCH)
                .collect(Collectors.toMap(JwtTokenGateway::domain, gateway -> gateway));
        this.churchUserDetailsService = churchUserDetailsService;
    }

    public boolean requiresAuthentication(String requestPath) {
        if (requestPath == null) {
            return false;
        }

        return requestPath.startsWith("/api/church/")
                && !requestPath.equals("/api/church/auth/login")
                && !requestPath.equals("/api/church/auth/refresh")
                && !requestPath.equals("/api/church/auth/register")
                && !requestPath.startsWith("/api/church/public/")
                && !requestPath.startsWith("/api/church/checkin/public/")
                && !requestPath.startsWith("/api/church/line/");
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
        if (domain != AuthDomain.CHURCH) {
            throw new IllegalArgumentException("Church backend does not serve auth domain: " + domain);
        }
        return churchUserDetailsService;
    }

    private JwtTokenGateway jwtGateway(AuthDomain domain) {
        JwtTokenGateway gateway = jwtTokenGateways.get(domain);
        if (gateway == null) {
            throw new IllegalStateException("No JwtTokenGateway registered for domain: " + domain);
        }
        return gateway;
    }
}
