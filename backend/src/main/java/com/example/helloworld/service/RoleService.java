package com.example.helloworld.service;

import com.example.helloworld.entity.Role;
import com.example.helloworld.entity.Permission;
import com.example.helloworld.repository.RoleRepository;
import com.example.helloworld.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    /**
     * 獲取所有角色
     */
    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    /**
     * 根據 ID 獲取角色
     */
    @Transactional(readOnly = true)
    public Optional<Role> getRoleById(Long id) {
        return roleRepository.findById(id);
    }

    /**
     * 根據角色名獲取角色
     */
    @Transactional(readOnly = true)
    public Optional<Role> getRoleByRoleName(String roleName) {
        return roleRepository.findByRoleName(roleName);
    }

    /**
     * 創建角色
     */
    @Transactional
    public Role createRole(Role role) {
        // 檢查角色名是否已存在
        if (roleRepository.findByRoleName(role.getRoleName()).isPresent()) {
            throw new RuntimeException("角色名已存在: " + role.getRoleName());
        }
        return roleRepository.save(role);
    }

    /**
     * 更新角色
     */
    @Transactional
    public Role updateRole(Long id, Role roleUpdate) {
        Role existing = roleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("角色不存在: " + id));

        // 更新基本信息
        if (roleUpdate.getRoleName() != null && !roleUpdate.getRoleName().equals(existing.getRoleName())) {
            // 檢查新角色名是否已存在
            if (roleRepository.findByRoleName(roleUpdate.getRoleName()).isPresent()) {
                throw new RuntimeException("角色名已存在: " + roleUpdate.getRoleName());
            }
            existing.setRoleName(roleUpdate.getRoleName());
        }
        if (roleUpdate.getDescription() != null) {
            existing.setDescription(roleUpdate.getDescription());
        }

        // 更新權限
        if (roleUpdate.getPermissions() != null) {
            Set<Permission> permissions = new HashSet<>();
            for (Permission permission : roleUpdate.getPermissions()) {
                if (permission != null && permission.getId() != null) {
                    permissionRepository.findById(permission.getId())
                        .ifPresent(permissions::add);
                }
            }
            existing.setPermissions(permissions);
        }

        return roleRepository.save(existing);
    }

    /**
     * 刪除角色
     */
    @Transactional
    public void deleteRole(Long id) {
        roleRepository.deleteById(id);
    }

    /**
     * 更新角色權限
     */
    @Transactional
    public Role updateRolePermissions(Long roleId, List<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("角色不存在: " + roleId));

        Set<Permission> permissions = permissionIds.stream()
            .map(permissionId -> permissionRepository.findById(permissionId))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());

        role.setPermissions(permissions);
        return roleRepository.save(role);
    }
}

