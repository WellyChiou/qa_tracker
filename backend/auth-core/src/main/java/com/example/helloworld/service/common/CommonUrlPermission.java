package com.example.helloworld.service.common;

public record CommonUrlPermission(
        String urlPattern,
        String httpMethod,
        String requiredRole,
        String requiredPermission,
        Boolean isPublic
) {
}
