package com.example.helloworld.service;

import com.example.helloworld.entity.User;
import com.example.helloworld.entity.Role;
import com.example.helloworld.repository.UserRepository;
import com.example.helloworld.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 獲取所有用戶
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * 根據 UID 獲取用戶
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByUid(String uid) {
        return userRepository.findById(uid);
    }

    /**
     * 根據用戶名獲取用戶
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 創建用戶
     */
    @Transactional
    public User createUser(User user) {
        // 如果提供了密碼，進行加密
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // 生成 UID（如果沒有提供）
        if (user.getUid() == null || user.getUid().isEmpty()) {
            user.setUid(UUID.randomUUID().toString());
        }

        // 設置默認值
        if (user.getIsEnabled() == null) {
            user.setIsEnabled(true);
        }
        if (user.getIsAccountNonLocked() == null) {
            user.setIsAccountNonLocked(true);
        }
        if (user.getProviderId() == null || user.getProviderId().isEmpty()) {
            user.setProviderId("local");
        }

        return userRepository.save(user);
    }

    /**
     * 更新用戶
     */
    @Transactional
    public User updateUser(String uid, User userUpdate) {
        User existing = userRepository.findById(uid)
            .orElseThrow(() -> new RuntimeException("用戶不存在: " + uid));

        // 更新基本信息
        if (userUpdate.getEmail() != null) {
            existing.setEmail(userUpdate.getEmail());
        }
        if (userUpdate.getUsername() != null) {
            existing.setUsername(userUpdate.getUsername());
        }
        if (userUpdate.getDisplayName() != null) {
            existing.setDisplayName(userUpdate.getDisplayName());
        }
        if (userUpdate.getPhotoUrl() != null) {
            existing.setPhotoUrl(userUpdate.getPhotoUrl());
        }
        if (userUpdate.getIsEnabled() != null) {
            existing.setIsEnabled(userUpdate.getIsEnabled());
        }
        if (userUpdate.getIsAccountNonLocked() != null) {
            existing.setIsAccountNonLocked(userUpdate.getIsAccountNonLocked());
        }

        // 更新密碼（如果提供）
        if (userUpdate.getPassword() != null && !userUpdate.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(userUpdate.getPassword()));
        }

        // 更新角色
        if (userUpdate.getRoles() != null) {
            Set<Role> roles = new HashSet<>();
            for (Role role : userUpdate.getRoles()) {
                if (role != null && role.getId() != null) {
                    roleRepository.findById(role.getId())
                        .ifPresent(roles::add);
                }
            }
            existing.setRoles(roles);
        }

        return userRepository.save(existing);
    }

    /**
     * 刪除用戶
     */
    @Transactional
    public void deleteUser(String uid) {
        userRepository.deleteById(uid);
    }

    /**
     * 更新用戶角色
     */
    @Transactional
    public User updateUserRoles(String uid, List<Long> roleIds) {
        User user = userRepository.findById(uid)
            .orElseThrow(() -> new RuntimeException("用戶不存在: " + uid));

        Set<Role> roles = roleIds.stream()
            .map(roleId -> roleRepository.findById(roleId))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());

        user.setRoles(roles);
        return userRepository.save(user);
    }

    /**
     * 檢查用戶名是否存在
     */
    @Transactional(readOnly = true)
    public boolean usernameExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    /**
     * 檢查郵箱是否存在
     */
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}

