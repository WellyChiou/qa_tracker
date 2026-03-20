package com.example.helloworld.service.common;

import org.springframework.stereotype.Component;

@Component
public class InvestAuthDomainPolicy implements AuthDomainPolicy {
    @Override
    public AuthDomain domain() {
        return AuthDomain.INVEST;
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public boolean supportsPath(String path) {
        return path != null && path.startsWith("/api/invest/");
    }

    @Override
    public boolean requiresAuthentication(String path) {
        if (!supportsPath(path)) {
            return false;
        }

        return !path.equals("/api/invest/auth/login")
            && !path.equals("/api/invest/auth/refresh")
            && !path.equals("/api/invest/auth/register")
            && !path.startsWith("/api/invest/public/");
    }
}
