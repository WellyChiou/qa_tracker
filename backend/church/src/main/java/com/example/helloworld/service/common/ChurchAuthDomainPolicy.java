package com.example.helloworld.service.common;

import org.springframework.stereotype.Component;

@Component
public class ChurchAuthDomainPolicy implements AuthDomainPolicy {
    @Override
    public AuthDomain domain() {
        return AuthDomain.CHURCH;
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public boolean supportsPath(String path) {
        return path != null && path.startsWith("/api/church/");
    }

    @Override
    public boolean requiresAuthentication(String path) {
        if (!supportsPath(path)) {
            return false;
        }

        return !path.equals("/api/church/auth/login")
                && !path.equals("/api/church/auth/refresh")
                && !path.equals("/api/church/auth/register")
                && !path.startsWith("/api/church/public/")
                && !path.startsWith("/api/church/checkin/public/")
                && !path.startsWith("/api/church/line/");
    }
}
