package com.example.helloworld.controller.personal;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.entity.personal.Role;
import com.example.helloworld.service.personal.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/personal/roles")
@CrossOrigin(origins = "*")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Role>>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.ok(roles));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Role>> getRoleById(@PathVariable Long id) {
        Optional<Role> role = roleService.getRoleById(id);
        return role.map(r -> ResponseEntity.ok(ApiResponse.ok(r)))
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("角色不存在")));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Role>> createRole(@RequestBody Role role) {
        try {
            Role created = roleService.createRole(role);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("創建失敗: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Role>> updateRole(@PathVariable Long id, @RequestBody Role role) {
        try {
            Role updated = roleService.updateRole(id, role);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新失敗: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        try {
            roleService.deleteRole(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除失敗: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/permissions")
    public ResponseEntity<ApiResponse<Role>> updateRolePermissions(@PathVariable Long id, @RequestBody Map<String, Object> request) {
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
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新失敗: " + e.getMessage()));
        }
    }
}
