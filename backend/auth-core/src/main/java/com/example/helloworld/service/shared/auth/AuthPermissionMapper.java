package com.example.helloworld.service.shared.auth;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class AuthPermissionMapper<P> {
    private final Function<P, String> permissionCodeExtractor;

    public AuthPermissionMapper(Function<P, String> permissionCodeExtractor) {
        this.permissionCodeExtractor = permissionCodeExtractor;
    }

    public List<String> map(Collection<P> permissions) {
        if (permissions == null) {
            return List.of();
        }

        Set<String> codes = new LinkedHashSet<>();
        for (P permission : permissions) {
            if (permission == null) {
                continue;
            }
            String code = permissionCodeExtractor.apply(permission);
            if (code != null) {
                codes.add(code);
            }
        }
        return List.copyOf(codes);
    }
}
