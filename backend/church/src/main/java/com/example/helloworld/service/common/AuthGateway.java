package com.example.helloworld.service.common;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthGateway extends AbstractAuthGateway {
    private final UserDetailsService churchUserDetailsService;

    public AuthGateway(
            List<JwtTokenGateway> jwtTokenGateways,
            List<AuthDomainPolicy> authDomainPolicies,
            UserDetailsService churchUserDetailsService) {
        super(jwtTokenGateways, authDomainPolicies);
        this.churchUserDetailsService = churchUserDetailsService;
    }

    @Override
    public UserDetailsService userDetailsService(AuthDomain domain) {
        if (domain != AuthDomain.CHURCH) {
            throw new IllegalArgumentException("Church backend does not serve auth domain: " + domain);
        }
        return churchUserDetailsService;
    }
}
