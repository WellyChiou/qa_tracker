package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.ChurchUser;
import com.example.helloworld.entity.church.ChurchRole;
import com.example.helloworld.repository.church.ChurchUserRepository;
import com.example.helloworld.repository.church.ChurchRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ChurchUserService {

    @Autowired
    private ChurchUserRepository churchUserRepository;

    @Autowired
    private ChurchRoleRepository churchRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 獲取所有用戶
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<ChurchUser> getAllUsers() {
        // 使用 JOIN FETCH 主動加載關聯，避免懶加載問題
        return churchUserRepository.findAllWithRolesAndPermissions();
    }

    /**
     * 根據 UID 獲取用戶
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<ChurchUser> getUserByUid(String uid) {
        return churchUserRepository.findByUid(uid);
    }

    /**
     * 根據用戶名獲取用戶
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<ChurchUser> getUserByUsername(String username) {
        return churchUserRepository.findByUsername(username);
    }

    /**
     * 檢查用戶名是否存在
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public boolean usernameExists(String username) {
        return churchUserRepository.findByUsername(username).isPresent();
    }

    /**
     * 檢查郵箱是否存在
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public boolean emailExists(String email) {
        return churchUserRepository.findByEmail(email).isPresent();
    }

    /**
     * 創建用戶
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ChurchUser createUser(ChurchUser user) {
        // 檢查用戶名是否已存在
        if (user.getUsername() != null && usernameExists(user.getUsername())) {
            throw new RuntimeException("用戶名已存在: " + user.getUsername());
        }

        // 檢查郵箱是否已存在
        if (user.getEmail() != null && emailExists(user.getEmail())) {
            throw new RuntimeException("郵箱已存在: " + user.getEmail());
        }

        // 如果提供了密碼，進行加密
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // 生成 UID（如果沒有提供）
        if (user.getUid() == null || user.getUid().isEmpty()) {
            user.setUid(UUID.randomUUID().toString());
        }

        return churchUserRepository.save(user);
    }

    /**
     * 更新用戶
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ChurchUser updateUser(String uid, ChurchUser userUpdate) {
        ChurchUser existing = churchUserRepository.findByUid(uid)
            .orElseThrow(() -> new RuntimeException("用戶不存在: " + uid));

        // 更新基本信息
        if (userUpdate.getEmail() != null && !userUpdate.getEmail().equals(existing.getEmail())) {
            if (emailExists(userUpdate.getEmail())) {
                throw new RuntimeException("郵箱已存在: " + userUpdate.getEmail());
            }
            existing.setEmail(userUpdate.getEmail());
        }
        if (userUpdate.getUsername() != null && !userUpdate.getUsername().equals(existing.getUsername())) {
            if (usernameExists(userUpdate.getUsername())) {
                throw new RuntimeException("用戶名已存在: " + userUpdate.getUsername());
            }
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
            existing.setRoles(userUpdate.getRoles());
        }

        // 更新權限
        if (userUpdate.getPermissions() != null) {
            existing.setPermissions(userUpdate.getPermissions());
        }

        return churchUserRepository.save(existing);
    }

    /**
     * 刪除用戶
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public void deleteUser(String uid) {
        churchUserRepository.deleteById(uid);
    }

    /**
     * 為用戶分配角色
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ChurchUser assignRoles(String uid, List<Long> roleIds) {
        ChurchUser user = churchUserRepository.findByUid(uid)
            .orElseThrow(() -> new RuntimeException("用戶不存在: " + uid));

        Set<ChurchRole> roles = new HashSet<>();
        for (Long roleId : roleIds) {
            ChurchRole role = churchRoleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在: " + roleId));
            roles.add(role);
        }

        user.setRoles(roles);
        return churchUserRepository.save(user);
    }
}

