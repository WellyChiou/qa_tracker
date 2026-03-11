package com.example.helloworld.controller.personal;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.entity.personal.Permission;
import com.example.helloworld.service.personal.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/personal/permissions")
@CrossOrigin(origins = "*")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Permission>>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.ok(permissions));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Permission>> getPermissionById(@PathVariable Long id) {
        Optional<Permission> permission = permissionService.getPermissionById(id);
        return permission.map(p -> ResponseEntity.ok(ApiResponse.ok(p)))
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("權限不存在")));
    }

    @GetMapping("/resource/{resource}")
    public ResponseEntity<ApiResponse<List<Permission>>> getPermissionsByResource(@PathVariable String resource) {
        List<Permission> permissions = permissionService.getPermissionsByResource(resource);
        return ResponseEntity.ok(ApiResponse.ok(permissions));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Permission>> createPermission(@RequestBody Permission permission) {
        try {
            Permission created = permissionService.createPermission(permission);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("創建失敗: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Permission>> updatePermission(@PathVariable Long id, @RequestBody Permission permission) {
        try {
            Permission updated = permissionService.updatePermission(id, permission);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新失敗: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePermission(@PathVariable Long id) {
        try {
            permissionService.deletePermission(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除失敗: " + e.getMessage()));
        }
    }
}
