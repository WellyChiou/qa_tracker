package com.example.helloworld.service.common;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthGateway extends AbstractAuthGateway {
    private final UserDetailsService personalUserDetailsService;

    public AuthGateway(
            List<JwtTokenGateway> jwtTokenGateways,
            List<AuthDomainPolicy> authDomainPolicies,
            UserDetailsService personalUserDetailsService) {
        super(jwtTokenGateways, authDomainPolicies);
        this.personalUserDetailsService = personalUserDetailsService;
    }

    @Override
    public UserDetailsService userDetailsService(AuthDomain domain) {
        if (domain != AuthDomain.PERSONAL) {
            throw new IllegalArgumentException("Personal backend does not serve auth domain: " + domain);
        }
        return personalUserDetailsService;
    }
}
