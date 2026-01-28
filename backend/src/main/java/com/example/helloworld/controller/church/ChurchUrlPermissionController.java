package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.entity.church.ChurchUrlPermission;
import com.example.helloworld.service.church.ChurchUrlPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
     * 獲取所有 URL 權限配置（支持分頁和過濾）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ChurchUrlPermission>>> getAllPermissions(
            @RequestParam(required = false) String urlPattern,
            @RequestParam(required = false) String httpMethod,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(required = false) String requiredPermission,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<ChurchUrlPermission> permissionsPage = churchUrlPermissionService.getAllPermissions(
                urlPattern, httpMethod, isPublic, requiredPermission, isActive, page, size);
            PageResponse<ChurchUrlPermission> pageResponse = new PageResponse<>(
                permissionsPage.getContent(),
                permissionsPage.getTotalElements(),
                permissionsPage.getTotalPages(),
                permissionsPage.getNumber(),
                permissionsPage.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(pageResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取 URL 權限列表失敗：" + e.getMessage()));
        }
    }

    /**
     * 根據 ID 獲取 URL 權限配置
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChurchUrlPermission>> getPermissionById(@PathVariable Long id) {
        try {
            Optional<ChurchUrlPermission> permission = churchUrlPermissionService.getPermissionById(id);
            if (permission.isPresent()) {
                return ResponseEntity.ok(ApiResponse.ok(permission.get()));
            } else {
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(ApiResponse.fail("找不到指定的 URL 權限"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取 URL 權限失敗：" + e.getMessage()));
        }
    }

    /**
     * 創建 URL 權限配置
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChurchUrlPermission>> createPermission(@RequestBody ChurchUrlPermission permission) {
        try {
            ChurchUrlPermission created = churchUrlPermissionService.createPermission(permission);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("創建失敗: " + e.getMessage()));
        }
    }

    /**
     * 更新 URL 權限配置
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ChurchUrlPermission>> updatePermission(@PathVariable Long id, @RequestBody ChurchUrlPermission permission) {
        try {
            ChurchUrlPermission updated = churchUrlPermissionService.updatePermission(id, permission);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新失敗: " + e.getMessage()));
        }
    }

    /**
     * 刪除 URL 權限配置
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePermission(@PathVariable Long id) {
        try {
            churchUrlPermissionService.deletePermission(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除失敗: " + e.getMessage()));
        }
    }

    /**
     * 獲取所有啟用的 URL 權限配置（用於動態配置）
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ChurchUrlPermission>>> getActivePermissions() {
        try {
            List<ChurchUrlPermission> permissions = churchUrlPermissionService.getAllActivePermissions();
            return ResponseEntity.ok(ApiResponse.ok(permissions));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取 URL 權限列表失敗：" + e.getMessage()));
        }
    }
}

