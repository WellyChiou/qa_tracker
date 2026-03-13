package com.example.helloworld.service.shared.auth;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AuthRoleMapper<R> {
    private final Function<R, String> roleNameExtractor;

    public AuthRoleMapper(Function<R, String> roleNameExtractor) {
        this.roleNameExtractor = roleNameExtractor;
    }

    public List<String> map(Collection<R> roles) {
        if (roles == null) {
            return List.of();
        }

        Set<String> roleNames = new LinkedHashSet<>();
        for (R role : roles) {
            if (role == null) {
                continue;
            }
            String name = roleNameExtractor.apply(role);
            if (name != null) {
                roleNames.add(name);
            }
        }
        return List.copyOf(roleNames);
    }
}
