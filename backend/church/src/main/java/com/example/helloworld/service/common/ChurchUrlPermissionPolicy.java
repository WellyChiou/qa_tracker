package com.example.helloworld.service.common;

import org.springframework.stereotype.Component;

@Component
public class ChurchUrlPermissionPolicy implements UrlPermissionPolicy {
    @Override
    public AuthDomain resolveDomain(String requestPath) {
        return AuthDomain.fromRequestPath(requestPath);
    }

    @Override
    public boolean bypassForAuthPath(String requestPath, AuthDomain domain) {
        return domain == AuthDomain.CHURCH
                && requestPath != null
                && requestPath.startsWith("/api/church/auth/");
    }

    @Override
    public boolean normalizePermissionCode(AuthDomain domain) {
        return domain == AuthDomain.CHURCH;
    }
}
