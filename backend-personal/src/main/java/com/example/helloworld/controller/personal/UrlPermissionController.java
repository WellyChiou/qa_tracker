package com.example.helloworld.controller.personal;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.entity.personal.UrlPermission;
import com.example.helloworld.service.personal.UrlPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/personal/url-permissions")
@CrossOrigin(origins = "*")
public class UrlPermissionController {

    @Autowired
    private UrlPermissionService urlPermissionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UrlPermission>>> getAllPermissions() {
        List<UrlPermission> permissions = urlPermissionService.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.ok(permissions));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UrlPermission>> getPermissionById(@PathVariable Long id) {
        Optional<UrlPermission> permission = urlPermissionService.getPermissionById(id);
        return permission.map(p -> ResponseEntity.ok(ApiResponse.ok(p)))
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("URL 權限不存在")));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UrlPermission>> createPermission(@RequestBody UrlPermission permission) {
        try {
            UrlPermission created = urlPermissionService.createPermission(permission);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("創建失敗: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UrlPermission>> updatePermission(@PathVariable Long id, @RequestBody UrlPermission permission) {
        try {
            UrlPermission updated = urlPermissionService.updatePermission(id, permission);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新失敗: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePermission(@PathVariable Long id) {
        try {
            urlPermissionService.deletePermission(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除失敗: " + e.getMessage()));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<UrlPermission>>> getActivePermissions() {
        List<UrlPermission> permissions = urlPermissionService.getAllActivePermissions();
        return ResponseEntity.ok(ApiResponse.ok(permissions));
    }
}
