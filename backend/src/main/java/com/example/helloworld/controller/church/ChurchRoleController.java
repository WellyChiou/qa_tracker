package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.entity.church.ChurchRole;
import com.example.helloworld.service.church.ChurchRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/church/admin/roles")
@CrossOrigin(origins = "*")
public class ChurchRoleController {

    @Autowired
    private ChurchRoleService churchRoleService;

    /**
     * 獲取所有角色（支持分頁和過濾）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ChurchRole>>> getAllRoles(
            @RequestParam(required = false) String roleName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<ChurchRole> rolesPage = churchRoleService.getAllRoles(roleName, page, size);
            PageResponse<ChurchRole> pageResponse = new PageResponse<>(
                rolesPage.getContent(),
                rolesPage.getTotalElements(),
                rolesPage.getTotalPages(),
                rolesPage.getNumber(),
                rolesPage.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(pageResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取角色列表失敗：" + e.getMessage()));
        }
    }

    /**
     * 根據 ID 獲取角色
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChurchRole>> getRoleById(@PathVariable Long id) {
        try {
            Optional<ChurchRole> role = churchRoleService.getRoleById(id);
            if (role.isPresent()) {
                return ResponseEntity.ok(ApiResponse.ok(role.get()));
            } else {
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(ApiResponse.fail("找不到指定的角色"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取角色失敗：" + e.getMessage()));
        }
    }

    /**
     * 創建角色
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChurchRole>> createRole(@RequestBody ChurchRole role) {
        try {
            ChurchRole created = churchRoleService.createRole(role);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("創建失敗: " + e.getMessage()));
        }
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ChurchRole>> updateRole(@PathVariable Long id, @RequestBody ChurchRole role) {
        try {
            ChurchRole updated = churchRoleService.updateRole(id, role);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新失敗: " + e.getMessage()));
        }
    }

    /**
     * 刪除角色
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        try {
            churchRoleService.deleteRole(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除失敗: " + e.getMessage()));
        }
    }

    /**
     * 為角色分配權限
     */
    @PostMapping("/{id}/permissions")
    public ResponseEntity<ApiResponse<ChurchRole>> assignPermissions(
            @PathVariable Long id, 
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<?> permissionIdsRaw = (List<?>) request.get("permissionIds");
            // 將 Integer 或 Long 轉換為 Long，處理 JSON 解析後的類型問題
            List<Long> permissionIds = permissionIdsRaw.stream()
                .map(idValue -> {
                    if (idValue instanceof Long) {
                        return (Long) idValue;
                    } else if (idValue instanceof Integer) {
                        return ((Integer) idValue).longValue();
                    } else if (idValue instanceof Number) {
                        return ((Number) idValue).longValue();
                    } else {
                        throw new IllegalArgumentException("無效的權限 ID 類型: " + idValue.getClass().getName());
                    }
                })
                .collect(java.util.stream.Collectors.toList());
            ChurchRole role = churchRoleService.assignPermissions(id, permissionIds);
            return ResponseEntity.ok(ApiResponse.ok(role));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("分配失敗: " + e.getMessage()));
        }
    }
}

