package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.ChurchRole;
import com.example.helloworld.entity.church.ChurchPermission;
import com.example.helloworld.repository.church.ChurchRoleRepository;
import com.example.helloworld.repository.church.ChurchPermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ChurchRoleService {

    @Autowired
    private ChurchRoleRepository churchRoleRepository;

    @Autowired
    private ChurchPermissionRepository churchPermissionRepository;

    /**
     * 獲取所有角色
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<ChurchRole> getAllRoles() {
        // 使用 JOIN FETCH 主動加載關聯，避免懶加載問題
        return churchRoleRepository.findAllWithPermissions();
    }

    /**
     * 根據 ID 獲取角色
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<ChurchRole> getRoleById(Long id) {
        Optional<ChurchRole> roleOpt = churchRoleRepository.findByIdWithPermissions(id);
        if (roleOpt.isPresent()) {
            ChurchRole role = roleOpt.get();
            // 確保在事務內初始化懶加載的權限集合
            if (role.getPermissions() != null) {
                role.getPermissions().size();
            }
        }
        return roleOpt;
    }

    /**
     * 根據角色名獲取角色
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<ChurchRole> getRoleByRoleName(String roleName) {
        return churchRoleRepository.findByRoleName(roleName);
    }

    /**
     * 創建角色
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ChurchRole createRole(ChurchRole role) {
        // 檢查角色名是否已存在
        if (churchRoleRepository.findByRoleName(role.getRoleName()).isPresent()) {
            throw new RuntimeException("角色名已存在: " + role.getRoleName());
        }
        
        ChurchRole saved = churchRoleRepository.save(role);
        
        // 確保在事務內初始化懶加載的權限集合
        if (saved.getPermissions() != null) {
            saved.getPermissions().size();
        }
        
        return saved;
    }

    /**
     * 更新角色
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ChurchRole updateRole(Long id, ChurchRole roleUpdate) {
        ChurchRole existing = churchRoleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("角色不存在: " + id));

        // 更新基本信息
        if (roleUpdate.getRoleName() != null && !roleUpdate.getRoleName().equals(existing.getRoleName())) {
            // 檢查新角色名是否已存在
            if (churchRoleRepository.findByRoleName(roleUpdate.getRoleName()).isPresent()) {
                throw new RuntimeException("角色名已存在: " + roleUpdate.getRoleName());
            }
            existing.setRoleName(roleUpdate.getRoleName());
        }
        if (roleUpdate.getDescription() != null) {
            existing.setDescription(roleUpdate.getDescription());
        }

        // 更新權限
        if (roleUpdate.getPermissions() != null) {
            existing.setPermissions(roleUpdate.getPermissions());
        }

        ChurchRole saved = churchRoleRepository.save(existing);
        
        // 確保在事務內初始化懶加載的權限集合
        if (saved.getPermissions() != null) {
            saved.getPermissions().size();
        }
        
        return saved;
    }

    /**
     * 刪除角色
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public void deleteRole(Long id) {
        churchRoleRepository.deleteById(id);
    }

    /**
     * 為角色分配權限
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ChurchRole assignPermissions(Long roleId, List<Long> permissionIds) {
        ChurchRole role = churchRoleRepository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("角色不存在: " + roleId));

        Set<ChurchPermission> permissions = new HashSet<>();
        for (Long permissionId : permissionIds) {
            ChurchPermission permission = churchPermissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("權限不存在: " + permissionId));
            permissions.add(permission);
        }

        role.setPermissions(permissions);
        ChurchRole saved = churchRoleRepository.save(role);
        
        // 確保在事務內初始化懶加載的權限集合
        if (saved.getPermissions() != null) {
            saved.getPermissions().size();
        }
        
        return saved;
    }
}

