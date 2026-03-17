package com.example.helloworld.service.common;

import org.springframework.stereotype.Component;

@Component
public class PersonalUrlPermissionPolicy implements UrlPermissionPolicy {
    @Override
    public AuthDomain resolveDomain(String requestPath) {
        return AuthDomain.fromRequestPath(requestPath);
    }

    @Override
    public boolean bypassForAuthPath(String requestPath, AuthDomain domain) {
        return false;
    }

    @Override
    public boolean normalizePermissionCode(AuthDomain domain) {
        return false;
    }
}
