package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.entity.church.ChurchRole;
import com.example.helloworld.service.church.ChurchRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

}
