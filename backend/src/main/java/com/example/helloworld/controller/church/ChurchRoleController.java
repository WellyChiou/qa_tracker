package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.ChurchRole;
import com.example.helloworld.service.church.ChurchRoleService;
import org.springframework.beans.factory.annotation.Autowired;
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
     * 獲取所有角色
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllRoles() {
        try {
            List<ChurchRole> roles = churchRoleService.getAllRoles();
            Map<String, Object> response = new HashMap<>();
            response.put("roles", roles);
            response.put("message", "獲取角色列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取角色列表失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 根據 ID 獲取角色
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRoleById(@PathVariable Long id) {
        try {
            Optional<ChurchRole> role = churchRoleService.getRoleById(id);
            if (role.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("role", role.get());
                response.put("message", "獲取角色成功");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取角色失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 創建角色
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRole(@RequestBody ChurchRole role) {
        try {
            ChurchRole created = churchRoleService.createRole(role);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "角色創建成功");
            response.put("role", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "創建失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateRole(@PathVariable Long id, @RequestBody ChurchRole role) {
        try {
            ChurchRole updated = churchRoleService.updateRole(id, role);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "角色更新成功");
            response.put("role", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 刪除角色
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteRole(@PathVariable Long id) {
        try {
            churchRoleService.deleteRole(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "角色刪除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "刪除失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 為角色分配權限
     */
    @PostMapping("/{id}/permissions")
    public ResponseEntity<Map<String, Object>> assignPermissions(
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
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "權限分配成功");
            response.put("role", role);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "分配失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

