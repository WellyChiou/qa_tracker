package com.example.helloworld.service.common;

import org.springframework.stereotype.Component;

@Component
public class InvestUrlPermissionPolicy implements UrlPermissionPolicy {
    @Override
    public AuthDomain resolveDomain(String requestPath) {
        if (requestPath != null && requestPath.startsWith("/api/invest/")) {
            return AuthDomain.INVEST;
        }
        return null;
    }

    @Override
    public boolean bypassForAuthPath(String requestPath, AuthDomain domain) {
        return domain == AuthDomain.INVEST
            && requestPath != null
            && requestPath.startsWith("/api/invest/auth/");
    }

    @Override
    public boolean normalizePermissionCode(AuthDomain domain) {
        return domain == AuthDomain.INVEST;
    }
}
