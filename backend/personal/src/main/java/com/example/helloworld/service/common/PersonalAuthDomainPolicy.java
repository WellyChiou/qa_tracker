package com.example.helloworld.service.common;

import org.springframework.stereotype.Component;

@Component
public class PersonalAuthDomainPolicy implements AuthDomainPolicy {
    @Override
    public AuthDomain domain() {
        return AuthDomain.PERSONAL;
    }

    @Override
    public int order() {
        return 200;
    }

    @Override
    public boolean supportsPath(String path) {
        return path != null && path.startsWith("/api/personal/");
    }

    @Override
    public boolean requiresAuthentication(String path) {
        if (!supportsPath(path)) {
            return false;
        }

        return !path.startsWith("/api/personal/auth/login")
                && !path.startsWith("/api/personal/auth/refresh")
                && !path.startsWith("/api/personal/auth/register")
                && !path.startsWith("/api/personal/line/");
    }
}
