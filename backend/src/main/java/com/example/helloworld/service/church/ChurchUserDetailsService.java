package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.ChurchPermission;
import com.example.helloworld.entity.church.ChurchRole;
import com.example.helloworld.entity.church.ChurchUser;
import com.example.helloworld.repository.church.ChurchUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

@Service
@Qualifier("churchUserDetailsService")
public class ChurchUserDetailsService implements UserDetailsService {

    @Autowired
    private ChurchUserRepository churchUserRepository;

    @Override
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            ChurchUser user = churchUserRepository.findByUsernameWithRolesAndPermissions(username)
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
        } catch (Exception e) {
            throw new UsernameNotFoundException("載入用戶失敗: " + e.getMessage(), e);
        }
    }

    private Collection<? extends GrantedAuthority> getAuthorities(ChurchUser user) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // 添加角色權限
        Set<ChurchRole> roles = user.getRoles();
        for (ChurchRole role : roles) {
            authorities.add(new SimpleGrantedAuthority(role.getRoleName()));
            
            // 添加角色擁有的權限
            Set<ChurchPermission> rolePermissions = role.getPermissions();
            for (ChurchPermission permission : rolePermissions) {
                authorities.add(new SimpleGrantedAuthority("PERM_" + permission.getPermissionCode()));
            }
        }

        // 添加用戶直接分配的權限
        Set<ChurchPermission> userPermissions = user.getPermissions();
        for (ChurchPermission permission : userPermissions) {
            authorities.add(new SimpleGrantedAuthority("PERM_" + permission.getPermissionCode()));
        }

        return authorities;
    }
}

