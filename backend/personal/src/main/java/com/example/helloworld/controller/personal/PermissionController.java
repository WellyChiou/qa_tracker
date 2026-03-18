package com.example.helloworld.controller.personal;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.entity.personal.Permission;
import com.example.helloworld.service.personal.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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

    @Deprecated
    @GetMapping
    public ResponseEntity<ApiResponse<List<Permission>>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.ok(permissions));
    }

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<List<PermissionOptionDTO>>> getPermissionOptions() {
        List<PermissionOptionDTO> options = permissionService.getAllPermissions().stream()
            .map(permission -> new PermissionOptionDTO(
                permission.getId(),
                permission.getPermissionCode(),
                permission.getPermissionName()
            ))
            .toList();
        return ResponseEntity.ok(ApiResponse.ok(options));
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PageResponse<Permission>>> getAllPermissionsPaged(
            @RequestParam(required = false) String permissionCode,
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<Permission> permissionsPage = permissionService.getAllPermissions(permissionCode, resource, action, page, size);
            PageResponse<Permission> pageResponse = new PageResponse<>(
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

    public record PermissionOptionDTO(Long id, String permissionCode, String permissionName) {}
}
