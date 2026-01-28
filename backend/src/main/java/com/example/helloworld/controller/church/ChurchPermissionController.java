package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.entity.church.ChurchPermission;
import com.example.helloworld.service.church.ChurchPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
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
     * 獲取所有權限（支持分頁和過濾）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ChurchPermission>>> getAllPermissions(
            @RequestParam(required = false) String permissionCode,
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<ChurchPermission> permissionsPage = churchPermissionService.getAllPermissions(permissionCode, resource, action, page, size);
            PageResponse<ChurchPermission> pageResponse = new PageResponse<>(
                permissionsPage.getContent(),
                permissionsPage.getTotalElements(),
                permissionsPage.getTotalPages(),
                permissionsPage.getNumber(),
                permissionsPage.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(pageResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取權限列表失敗：" + e.getMessage()));
        }
    }

    /**
     * 根據 ID 獲取權限
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChurchPermission>> getPermissionById(@PathVariable Long id) {
        try {
            Optional<ChurchPermission> permission = churchPermissionService.getPermissionById(id);
            if (permission.isPresent()) {
                return ResponseEntity.ok(ApiResponse.ok(permission.get()));
            } else {
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(ApiResponse.fail("找不到指定的權限"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取權限失敗：" + e.getMessage()));
        }
    }

    /**
     * 根據資源獲取權限
     */
    @GetMapping("/resource/{resource}")
    public ResponseEntity<ApiResponse<List<ChurchPermission>>> getPermissionsByResource(@PathVariable String resource) {
        try {
            List<ChurchPermission> permissions = churchPermissionService.getPermissionsByResource(resource);
            return ResponseEntity.ok(ApiResponse.ok(permissions));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取權限列表失敗：" + e.getMessage()));
        }
    }

    /**
     * 創建權限
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChurchPermission>> createPermission(@RequestBody ChurchPermission permission) {
        try {
            ChurchPermission created = churchPermissionService.createPermission(permission);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("創建失敗: " + e.getMessage()));
        }
    }

    /**
     * 更新權限
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ChurchPermission>> updatePermission(@PathVariable Long id, @RequestBody ChurchPermission permission) {
        try {
            ChurchPermission updated = churchPermissionService.updatePermission(id, permission);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新失敗: " + e.getMessage()));
        }
    }

    /**
     * 刪除權限
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePermission(@PathVariable Long id) {
        try {
            churchPermissionService.deletePermission(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除失敗: " + e.getMessage()));
        }
    }
}

