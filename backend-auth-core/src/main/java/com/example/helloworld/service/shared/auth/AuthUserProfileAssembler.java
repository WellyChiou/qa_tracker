package com.example.helloworld.service.shared.auth;

import com.example.helloworld.dto.common.auth.AuthMenuItem;
import com.example.helloworld.dto.common.auth.AuthUserProfile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class AuthUserProfileAssembler<U, R, P> {
    private final Function<U, String> uidExtractor;
    private final Function<U, String> usernameExtractor;
    private final Function<U, String> emailExtractor;
    private final Function<U, String> displayNameExtractor;
    private final Function<U, String> photoUrlExtractor;
    private final Function<U, Collection<R>> rolesExtractor;
    private final Function<R, Collection<P>> rolePermissionsExtractor;
    private final Function<U, Collection<P>> permissionsExtractor;
    private final AuthRoleMapper<R> roleMapper;
    private final AuthPermissionMapper<P> permissionMapper;

    private AuthUserProfileAssembler(Builder<U, R, P> builder) {
        this.uidExtractor = builder.uidExtractor;
        this.usernameExtractor = builder.usernameExtractor;
        this.emailExtractor = builder.emailExtractor;
        this.displayNameExtractor = builder.displayNameExtractor;
        this.photoUrlExtractor = builder.photoUrlExtractor;
        this.rolesExtractor = builder.rolesExtractor;
        this.rolePermissionsExtractor = builder.rolePermissionsExtractor;
        this.permissionsExtractor = builder.permissionsExtractor;
        this.roleMapper = builder.roleMapper;
        this.permissionMapper = builder.permissionMapper;
    }

    public AuthUserProfile assemble(U user, String systemCode, List<AuthMenuItem> menus) {
        AuthUserProfile profile = new AuthUserProfile();
        profile.setSystemCode(systemCode);
        if (user == null) {
            profile.setAuthenticated(false);
            profile.setMenus(Collections.emptyList());
            return profile;
        }

        profile.setAuthenticated(true);
        profile.setUid(extract(uidExtractor, user));
        profile.setUsername(extract(usernameExtractor, user));
        profile.setEmail(extract(emailExtractor, user));
        profile.setDisplayName(extract(displayNameExtractor, user));
        profile.setPhotoUrl(extract(photoUrlExtractor, user));

        List<R> roles = toList(rolesExtractor.apply(user));
        profile.setRoles(roleMapper.map(roles));

        List<P> accumulatedPermissions = new ArrayList<>();
        for (R role : roles) {
            Collection<P> rolePerms = rolePermissionsExtractor.apply(role);
            if (rolePerms != null) {
                accumulatedPermissions.addAll(rolePerms);
            }
        }
        Collection<P> directPermissions = permissionsExtractor.apply(user);
        if (directPermissions != null) {
            accumulatedPermissions.addAll(directPermissions);
        }

        profile.setPermissions(permissionMapper.map(accumulatedPermissions));
        profile.setMenus(menus != null ? menus : Collections.emptyList());
        return profile;
    }

    private String extract(Function<U, String> extractor, U source) {
        return extractor == null || source == null ? null : extractor.apply(source);
    }

    private List<R> toList(Collection<R> roles) {
        if (roles == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(roles);
    }

    public static class Builder<U, R, P> {
        private Function<U, String> uidExtractor;
        private Function<U, String> usernameExtractor;
        private Function<U, String> emailExtractor;
        private Function<U, String> displayNameExtractor;
        private Function<U, String> photoUrlExtractor;
        private Function<U, Collection<R>> rolesExtractor;
        private Function<R, Collection<P>> rolePermissionsExtractor = role -> Collections.emptyList();
        private Function<U, Collection<P>> permissionsExtractor = user -> Collections.emptyList();
        private AuthRoleMapper<R> roleMapper;
        private AuthPermissionMapper<P> permissionMapper;

        public Builder<U, R, P> uid(Function<U, String> extractor) {
            this.uidExtractor = extractor;
            return this;
        }

        public Builder<U, R, P> username(Function<U, String> extractor) {
            this.usernameExtractor = extractor;
            return this;
        }

        public Builder<U, R, P> email(Function<U, String> extractor) {
            this.emailExtractor = extractor;
            return this;
        }

        public Builder<U, R, P> displayName(Function<U, String> extractor) {
            this.displayNameExtractor = extractor;
            return this;
        }

        public Builder<U, R, P> photoUrl(Function<U, String> extractor) {
            this.photoUrlExtractor = extractor;
            return this;
        }

        public Builder<U, R, P> roles(Function<U, Collection<R>> extractor) {
            this.rolesExtractor = extractor;
            return this;
        }

        public Builder<U, R, P> rolePermissions(Function<R, Collection<P>> extractor) {
            this.rolePermissionsExtractor = extractor;
            return this;
        }

        public Builder<U, R, P> permissions(Function<U, Collection<P>> extractor) {
            this.permissionsExtractor = extractor;
            return this;
        }

        public Builder<U, R, P> roleName(Function<R, String> extractor) {
            this.roleMapper = new AuthRoleMapper<>(extractor);
            return this;
        }

        public Builder<U, R, P> permissionCode(Function<P, String> extractor) {
            this.permissionMapper = new AuthPermissionMapper<>(extractor);
            return this;
        }

        public AuthUserProfileAssembler<U, R, P> build() {
            Objects.requireNonNull(uidExtractor, "uid extractor is required");
            Objects.requireNonNull(usernameExtractor, "username extractor is required");
            Objects.requireNonNull(emailExtractor, "email extractor is required");
            Objects.requireNonNull(displayNameExtractor, "display name extractor is required");
            Objects.requireNonNull(photoUrlExtractor, "photo url extractor is required");
            Objects.requireNonNull(rolesExtractor, "roles extractor is required");
            Objects.requireNonNull(rolePermissionsExtractor, "role permissions extractor is required");
            Objects.requireNonNull(permissionsExtractor, "permissions extractor is required");
            Objects.requireNonNull(roleMapper, "role name extractor is required");
            Objects.requireNonNull(permissionMapper, "permission code extractor is required");
            return new AuthUserProfileAssembler<>(this);
        }
    }
}
