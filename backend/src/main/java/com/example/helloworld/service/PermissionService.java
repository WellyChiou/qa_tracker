package com.example.helloworld.service;

import com.example.helloworld.entity.Permission;
import com.example.helloworld.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    /**
     * 獲取所有權限
     */
    @Transactional(readOnly = true)
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    /**
     * 根據 ID 獲取權限
     */
    @Transactional(readOnly = true)
    public Optional<Permission> getPermissionById(Long id) {
        return permissionRepository.findById(id);
    }

    /**
     * 根據權限代碼獲取權限
     */
    @Transactional(readOnly = true)
    public Optional<Permission> getPermissionByCode(String permissionCode) {
        return permissionRepository.findByPermissionCode(permissionCode);
    }

    /**
     * 根據資源獲取權限
     */
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByResource(String resource) {
        return permissionRepository.findByResource(resource);
    }

    /**
     * 創建權限
     */
    @Transactional
    public Permission createPermission(Permission permission) {
        // 檢查權限代碼是否已存在
        if (permissionRepository.findByPermissionCode(permission.getPermissionCode()).isPresent()) {
            throw new RuntimeException("權限代碼已存在: " + permission.getPermissionCode());
        }
        return permissionRepository.save(permission);
    }

    /**
     * 更新權限
     */
    @Transactional
    public Permission updatePermission(Long id, Permission permissionUpdate) {
        Permission existing = permissionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("權限不存在: " + id));

        // 更新基本信息
        if (permissionUpdate.getPermissionCode() != null && 
            !permissionUpdate.getPermissionCode().equals(existing.getPermissionCode())) {
            // 檢查新權限代碼是否已存在
            if (permissionRepository.findByPermissionCode(permissionUpdate.getPermissionCode()).isPresent()) {
                throw new RuntimeException("權限代碼已存在: " + permissionUpdate.getPermissionCode());
            }
            existing.setPermissionCode(permissionUpdate.getPermissionCode());
        }
        if (permissionUpdate.getPermissionName() != null) {
            existing.setPermissionName(permissionUpdate.getPermissionName());
        }
        if (permissionUpdate.getResource() != null) {
            existing.setResource(permissionUpdate.getResource());
        }
        if (permissionUpdate.getAction() != null) {
            existing.setAction(permissionUpdate.getAction());
        }
        if (permissionUpdate.getDescription() != null) {
            existing.setDescription(permissionUpdate.getDescription());
        }

        return permissionRepository.save(existing);
    }

    /**
     * 刪除權限
     */
    @Transactional
    public void deletePermission(Long id) {
        permissionRepository.deleteById(id);
    }
}

