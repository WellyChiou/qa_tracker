package com.example.helloworld.service.common;

public interface AuthDomainPolicy {
    AuthDomain domain();

    int order();

    boolean supportsPath(String path);

    boolean requiresAuthentication(String path);
}
