package com.example.helloworld.service.common;

public interface UrlPermissionPolicy {
    AuthDomain resolveDomain(String requestPath);

    boolean bypassForAuthPath(String requestPath, AuthDomain domain);

    boolean normalizePermissionCode(AuthDomain domain);
}
