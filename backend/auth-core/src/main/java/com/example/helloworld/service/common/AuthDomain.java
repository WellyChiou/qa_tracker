package com.example.helloworld.service.common;

public enum AuthDomain {
    PERSONAL("personal"),
    CHURCH("church"),
    INVEST("invest");

    private final String value;

    AuthDomain(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static AuthDomain fromRequestPath(String requestPath) {
        if (requestPath != null && requestPath.startsWith("/api/church/")) {
            return CHURCH;
        }
        if (requestPath != null && requestPath.startsWith("/api/invest/")) {
            return INVEST;
        }
        return PERSONAL;
    }
}
