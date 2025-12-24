package com.example.helloworld.util.church;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class CurrentUser {
    private CurrentUser() {}

    public static String usernameOrSystem() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) return "system";
            if (!auth.isAuthenticated()) return "system";
            String name = auth.getName();
            if (name == null || name.isBlank() || "anonymousUser".equals(name)) return "system";
            return name;
        } catch (Exception e) {
            return "system";
        }
    }
}

