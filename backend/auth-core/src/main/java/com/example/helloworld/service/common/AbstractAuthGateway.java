package com.example.helloworld.service.common;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractAuthGateway {
    private final Map<AuthDomain, JwtTokenGateway> jwtTokenGateways;
    private final List<AuthDomainPolicy> authDomainPolicies;

    protected AbstractAuthGateway(
            List<JwtTokenGateway> jwtTokenGateways,
            List<AuthDomainPolicy> authDomainPolicies) {
        this.authDomainPolicies = authDomainPolicies.stream()
                .sorted(Comparator.comparingInt(AuthDomainPolicy::order))
                .toList();

        Set<AuthDomain> supportedDomains = this.authDomainPolicies.stream()
                .map(AuthDomainPolicy::domain)
                .collect(Collectors.toSet());

        this.jwtTokenGateways = jwtTokenGateways.stream()
                .filter(gateway -> supportedDomains.contains(gateway.domain()))
                .collect(Collectors.toMap(JwtTokenGateway::domain, gateway -> gateway));
    }

    public AuthDomain resolveDomain(String path) {
        AuthDomainPolicy policy = findPolicy(path);
        if (policy == null) {
            throw new IllegalArgumentException("No AuthDomainPolicy matches path: " + path);
        }
        return policy.domain();
    }

    public boolean requiresAuthentication(String path) {
        AuthDomainPolicy policy = findPolicy(path);
        if (policy == null) {
            return false;
        }
        return policy.requiresAuthentication(path);
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

    public abstract UserDetailsService userDetailsService(AuthDomain domain);

    private JwtTokenGateway jwtGateway(AuthDomain domain) {
        JwtTokenGateway gateway = jwtTokenGateways.get(domain);
        if (gateway == null) {
            throw new IllegalStateException("No JwtTokenGateway registered for domain: " + domain);
        }
        return gateway;
    }

    private AuthDomainPolicy findPolicy(String path) {
        for (AuthDomainPolicy policy : authDomainPolicies) {
            if (policy.supportsPath(path)) {
                return policy;
            }
        }
        return null;
    }
}
