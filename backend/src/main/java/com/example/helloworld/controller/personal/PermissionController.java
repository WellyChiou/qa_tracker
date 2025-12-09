package com.example.helloworld.controller.personal;

import com.example.helloworld.entity.personal.Permission;
import com.example.helloworld.service.personal.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/permissions")
@CrossOrigin(origins = "*")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    /**
     * 獲取所有權限
     */
    @GetMapping
    public ResponseEntity<List<Permission>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        return ResponseEntity.ok(permissions);
    }

    /**
     * 根據 ID 獲取權限
     */
    @GetMapping("/{id}")
    public ResponseEntity<Permission> getPermissionById(@PathVariable Long id) {
        Optional<Permission> permission = permissionService.getPermissionById(id);
        return permission.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根據資源獲取權限
     */
    @GetMapping("/resource/{resource}")
    public ResponseEntity<List<Permission>> getPermissionsByResource(@PathVariable String resource) {
        List<Permission> permissions = permissionService.getPermissionsByResource(resource);
        return ResponseEntity.ok(permissions);
    }

    /**
     * 創建權限
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createPermission(@RequestBody Permission permission) {
        try {
            Permission created = permissionService.createPermission(permission);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "權限創建成功");
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
     * 更新權限
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updatePermission(@PathVariable Long id, @RequestBody Permission permission) {
        try {
            Permission updated = permissionService.updatePermission(id, permission);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "權限更新成功");
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
     * 刪除權限
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePermission(@PathVariable Long id) {
        try {
            permissionService.deletePermission(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "權限刪除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "刪除失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
