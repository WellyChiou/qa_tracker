package com.example.helloworld.controller;

import com.example.helloworld.entity.Role;
import com.example.helloworld.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*")
public class RoleController {

    @Autowired
    private RoleService roleService;

    /**
     * 獲取所有角色
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Role>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    /**
     * 根據 ID 獲取角色
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Role> getRoleById(@PathVariable Long id) {
        Optional<Role> role = roleService.getRoleById(id);
        return role.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 創建角色
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createRole(@RequestBody Role role) {
        try {
            Role created = roleService.createRole(role);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "角色創建成功");
            response.put("data", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "創建失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateRole(@PathVariable Long id, @RequestBody Role role) {
        try {
            Role updated = roleService.updateRole(id, role);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "角色更新成功");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 刪除角色
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteRole(@PathVariable Long id) {
        try {
            roleService.deleteRole(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "角色刪除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "刪除失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新角色權限
     */
    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateRolePermissions(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> permissionIdsObj = (List<Object>) request.get("permissionIds");
            List<Long> permissionIds = new ArrayList<>();
            if (permissionIdsObj != null) {
                for (Object permId : permissionIdsObj) {
                    if (permId instanceof Number) {
                        permissionIds.add(((Number) permId).longValue());
                    } else if (permId instanceof String) {
                        permissionIds.add(Long.parseLong((String) permId));
                    }
                }
            }
            Role updated = roleService.updateRolePermissions(id, permissionIds);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "角色權限更新成功");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

