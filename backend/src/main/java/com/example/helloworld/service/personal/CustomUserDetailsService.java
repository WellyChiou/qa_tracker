package com.example.helloworld.service.personal;

import com.example.helloworld.entity.personal.Permission;
import com.example.helloworld.entity.personal.Role;
import com.example.helloworld.entity.personal.User;
import com.example.helloworld.repository.personal.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameWithRolesAndPermissions(username)
            .orElseThrow(() -> new UsernameNotFoundException("用戶不存在: " + username));

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword() != null ? user.getPassword() : "{noop}")
            .authorities(getAuthorities(user))
            .accountExpired(false)
            .accountLocked(!user.getIsAccountNonLocked())
            .credentialsExpired(false)
            .disabled(!user.getIsEnabled())
            .build();
    }

    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // 添加角色權限
        Set<Role> roles = user.getRoles();
        for (Role role : roles) {
            authorities.add(new SimpleGrantedAuthority(role.getRoleName()));
            
            // 添加角色擁有的權限
            Set<Permission> rolePermissions = role.getPermissions();
            for (Permission permission : rolePermissions) {
                authorities.add(new SimpleGrantedAuthority("PERM_" + permission.getPermissionCode()));
            }
        }

        // 添加用戶直接分配的權限
        Set<Permission> userPermissions = user.getPermissions();
        for (Permission permission : userPermissions) {
            authorities.add(new SimpleGrantedAuthority("PERM_" + permission.getPermissionCode()));
        }

        return authorities;
    }
}
