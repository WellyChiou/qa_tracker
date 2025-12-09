package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.ChurchUrlPermission;
import com.example.helloworld.service.church.ChurchUrlPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/church/admin/url-permissions")
@CrossOrigin(origins = "*")
public class ChurchUrlPermissionController {

    @Autowired
    private ChurchUrlPermissionService churchUrlPermissionService;

    /**
     * 獲取所有 URL 權限配置
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPermissions() {
        try {
            List<ChurchUrlPermission> permissions = churchUrlPermissionService.getAllPermissions();
            Map<String, Object> response = new HashMap<>();
            response.put("permissions", permissions);
            response.put("message", "獲取 URL 權限列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取 URL 權限列表失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 根據 ID 獲取 URL 權限配置
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPermissionById(@PathVariable Long id) {
        try {
            Optional<ChurchUrlPermission> permission = churchUrlPermissionService.getPermissionById(id);
            if (permission.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("permission", permission.get());
                response.put("message", "獲取 URL 權限成功");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取 URL 權限失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 創建 URL 權限配置
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createPermission(@RequestBody ChurchUrlPermission permission) {
        try {
            ChurchUrlPermission created = churchUrlPermissionService.createPermission(permission);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "URL 權限配置創建成功");
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
     * 更新 URL 權限配置
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updatePermission(@PathVariable Long id, @RequestBody ChurchUrlPermission permission) {
        try {
            ChurchUrlPermission updated = churchUrlPermissionService.updatePermission(id, permission);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "URL 權限配置更新成功");
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
     * 刪除 URL 權限配置
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePermission(@PathVariable Long id) {
        try {
            churchUrlPermissionService.deletePermission(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "URL 權限配置刪除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "刪除失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 獲取所有啟用的 URL 權限配置（用於動態配置）
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActivePermissions() {
        try {
            List<ChurchUrlPermission> permissions = churchUrlPermissionService.getAllActivePermissions();
            Map<String, Object> response = new HashMap<>();
            response.put("permissions", permissions);
            response.put("message", "獲取啟用的 URL 權限列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取 URL 權限列表失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}

