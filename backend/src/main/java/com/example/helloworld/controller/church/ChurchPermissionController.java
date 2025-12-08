package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.ChurchPermission;
import com.example.helloworld.service.church.ChurchPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/church/admin/permissions")
@CrossOrigin(origins = "*")
public class ChurchPermissionController {

    @Autowired
    private ChurchPermissionService churchPermissionService;

    /**
     * 獲取所有權限
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_CHURCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllPermissions() {
        try {
            List<ChurchPermission> permissions = churchPermissionService.getAllPermissions();
            Map<String, Object> response = new HashMap<>();
            response.put("permissions", permissions);
            response.put("message", "獲取權限列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取權限列表失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 根據 ID 獲取權限
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_CHURCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> getPermissionById(@PathVariable Long id) {
        try {
            Optional<ChurchPermission> permission = churchPermissionService.getPermissionById(id);
            if (permission.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("permission", permission.get());
                response.put("message", "獲取權限成功");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取權限失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 根據資源獲取權限
     */
    @GetMapping("/resource/{resource}")
    @PreAuthorize("hasAuthority('PERM_CHURCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> getPermissionsByResource(@PathVariable String resource) {
        try {
            List<ChurchPermission> permissions = churchPermissionService.getPermissionsByResource(resource);
            Map<String, Object> response = new HashMap<>();
            response.put("permissions", permissions);
            response.put("message", "獲取權限列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取權限列表失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 創建權限
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CHURCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> createPermission(@RequestBody ChurchPermission permission) {
        try {
            ChurchPermission created = churchPermissionService.createPermission(permission);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "權限創建成功");
            response.put("permission", created);
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
    @PreAuthorize("hasAuthority('PERM_CHURCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> updatePermission(@PathVariable Long id, @RequestBody ChurchPermission permission) {
        try {
            ChurchPermission updated = churchPermissionService.updatePermission(id, permission);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "權限更新成功");
            response.put("permission", updated);
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
    @PreAuthorize("hasAuthority('PERM_CHURCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> deletePermission(@PathVariable Long id) {
        try {
            churchPermissionService.deletePermission(id);
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

