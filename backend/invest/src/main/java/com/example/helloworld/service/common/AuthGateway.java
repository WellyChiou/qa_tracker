package com.example.helloworld.service.common;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthGateway extends AbstractAuthGateway {

    private final UserDetailsService investUserDetailsService;

    public AuthGateway(
            List<JwtTokenGateway> jwtTokenGateways,
            List<AuthDomainPolicy> authDomainPolicies,
            UserDetailsService investUserDetailsService) {
        super(jwtTokenGateways, authDomainPolicies);
        this.investUserDetailsService = investUserDetailsService;
    }

    @Override
    public UserDetailsService userDetailsService(AuthDomain domain) {
        if (domain != AuthDomain.INVEST) {
            throw new IllegalArgumentException("Invest backend does not serve auth domain: " + domain);
        }
        return investUserDetailsService;
    }
}
