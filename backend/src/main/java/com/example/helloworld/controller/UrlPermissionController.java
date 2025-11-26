package com.example.helloworld.controller;

import com.example.helloworld.entity.UrlPermission;
import com.example.helloworld.service.UrlPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/url-permissions")
@CrossOrigin(origins = "*")
public class UrlPermissionController {

    @Autowired
    private UrlPermissionService urlPermissionService;

    /**
     * 獲取所有 URL 權限配置
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UrlPermission>> getAllPermissions() {
        List<UrlPermission> permissions = urlPermissionService.getAllPermissions();
        return ResponseEntity.ok(permissions);
    }

    /**
     * 根據 ID 獲取 URL 權限配置
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UrlPermission> getPermissionById(@PathVariable Long id) {
        Optional<UrlPermission> permission = urlPermissionService.getPermissionById(id);
        return permission.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 創建 URL 權限配置
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createPermission(@RequestBody UrlPermission permission) {
        try {
            UrlPermission created = urlPermissionService.createPermission(permission);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "URL 權限配置創建成功");
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
     * 更新 URL 權限配置
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updatePermission(@PathVariable Long id, @RequestBody UrlPermission permission) {
        try {
            UrlPermission updated = urlPermissionService.updatePermission(id, permission);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "URL 權限配置更新成功");
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
     * 刪除 URL 權限配置
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deletePermission(@PathVariable Long id) {
        try {
            urlPermissionService.deletePermission(id);
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
    public ResponseEntity<List<UrlPermission>> getActivePermissions() {
        List<UrlPermission> permissions = urlPermissionService.getAllActivePermissions();
        return ResponseEntity.ok(permissions);
    }
}

