package com.example.helloworld.service.invest.auth;

import com.example.helloworld.entity.invest.auth.InvestPermission;
import com.example.helloworld.entity.invest.auth.InvestRole;
import com.example.helloworld.entity.invest.auth.InvestUser;
import com.example.helloworld.repository.invest.auth.InvestUserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class InvestUserDetailsService implements UserDetailsService {

    private final InvestUserRepository investUserRepository;

    public InvestUserDetailsService(InvestUserRepository investUserRepository) {
        this.investUserRepository = investUserRepository;
    }

    @Override
    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        InvestUser user = investUserRepository.findByUsernameWithRolesAndPermissions(username)
            .orElseThrow(() -> new UsernameNotFoundException("使用者不存在: " + username));

        String password = user.getPassword();
        if (password == null || password.trim().isEmpty()) {
            throw new UsernameNotFoundException("使用者密碼未設定: " + username);
        }

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(password)
            .authorities(getAuthorities(user))
            .accountExpired(false)
            .accountLocked(!user.getIsAccountNonLocked())
            .credentialsExpired(false)
            .disabled(!user.getIsEnabled())
            .build();
    }

    private Collection<? extends GrantedAuthority> getAuthorities(InvestUser user) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        for (InvestRole role : user.getRoles()) {
            addAuthority(authorities, role.getRoleName());
            for (InvestPermission permission : role.getPermissions()) {
                addPermissionAuthorities(authorities, permission.getPermissionCode());
            }
        }

        for (InvestPermission permission : user.getPermissions()) {
            addPermissionAuthorities(authorities, permission.getPermissionCode());
        }

        return authorities;
    }

    private void addPermissionAuthorities(List<GrantedAuthority> authorities, String permissionCode) {
        if (permissionCode == null) {
            return;
        }

        String rawCode = permissionCode.trim();
        if (rawCode.isEmpty()) {
            return;
        }

        String normalizedCode = rawCode.startsWith("PERM_") ? rawCode.substring(5) : rawCode;
        if (normalizedCode.isEmpty()) {
            return;
        }

        addAuthority(authorities, normalizedCode);
    }

    private void addAuthority(List<GrantedAuthority> authorities, String authority) {
        if (authority == null || authority.isBlank()) {
            return;
        }

        boolean exists = authorities.stream().anyMatch(existing -> existing.getAuthority().equals(authority));
        if (!exists) {
            authorities.add(new SimpleGrantedAuthority(authority));
        }
    }
}
