package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.entity.church.ChurchUser;
import com.example.helloworld.service.church.ChurchUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/church/admin/users")
@CrossOrigin(origins = "*")
public class ChurchUserController {

    @Autowired
    private ChurchUserService churchUserService;

    /**
     * 獲取所有用戶（支援分頁）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ChurchUser>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ChurchUser> usersPage = churchUserService.getAllUsers(pageable);
            // Service 層已經使用 JOIN FETCH 主動加載所有關聯，無需在這裡手動初始化
            PageResponse<ChurchUser> pageResponse = new PageResponse<>(
                    usersPage.getContent(),
                    usersPage.getTotalElements(),
                    usersPage.getTotalPages(),
                    usersPage.getNumber(),
                    usersPage.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(pageResponse));
        } catch (Exception e) {
            e.printStackTrace(); // 輸出錯誤堆棧以便調試
            return ResponseEntity.status(500)
                    .body(ApiResponse.fail("獲取用戶列表失敗：" + e.getMessage()));
        }
    }

    /**
     * 根據 UID 獲取用戶
     */
    @GetMapping("/{uid}")
    public ResponseEntity<ApiResponse<ChurchUser>> getUserByUid(@PathVariable String uid) {
        try {
            Optional<ChurchUser> user = churchUserService.getUserByUid(uid);
            if (user.isPresent()) {
                return ResponseEntity.ok(ApiResponse.ok(user.get()));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("用戶不存在"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取用戶失敗：" + e.getMessage()));
        }
    }

    /**
     * 創建用戶
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChurchUser>> createUser(@RequestBody ChurchUser user) {
        try {
            ChurchUser created = churchUserService.createUser(user);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("創建失敗: " + e.getMessage()));
        }
    }

    /**
     * 更新用戶
     */
    @PutMapping("/{uid}")
    public ResponseEntity<ApiResponse<ChurchUser>> updateUser(@PathVariable String uid, @RequestBody ChurchUser user) {
        try {
            ChurchUser updated = churchUserService.updateUser(uid, user);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("更新失敗: " + e.getMessage()));
        }
    }

    /**
     * 刪除用戶
     */
    @DeleteMapping("/{uid}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable String uid) {
        try {
            churchUserService.deleteUser(uid);
            return ResponseEntity.ok(ApiResponse.ok("用戶刪除成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("刪除失敗: " + e.getMessage()));
        }
    }

    /**
     * 為用戶分配角色
     */
    @PostMapping("/{uid}/roles")
    public ResponseEntity<ApiResponse<ChurchUser>> assignRoles(
            @PathVariable String uid, 
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<?> roleIdsRaw = (List<?>) request.get("roleIds");
            // 將 Integer 或 Long 轉換為 Long，處理 JSON 解析後的類型問題
            List<Long> roleIds = roleIdsRaw.stream()
                .map(idValue -> {
                    if (idValue instanceof Long) {
                        return (Long) idValue;
                    } else if (idValue instanceof Integer) {
                        return ((Integer) idValue).longValue();
                    } else if (idValue instanceof Number) {
                        return ((Number) idValue).longValue();
                    } else {
                        throw new IllegalArgumentException("無效的角色 ID 類型: " + idValue.getClass().getName());
                    }
                })
                .collect(java.util.stream.Collectors.toList());
            ChurchUser user = churchUserService.assignRoles(uid, roleIds);
            return ResponseEntity.ok(ApiResponse.ok(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("分配失敗: " + e.getMessage()));
        }
    }

    /**
     * 為用戶分配權限
     */
    @PostMapping("/{uid}/permissions")
    public ResponseEntity<ApiResponse<ChurchUser>> assignPermissions(
            @PathVariable String uid, 
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
            ChurchUser user = churchUserService.assignPermissions(uid, permissionIds);
            return ResponseEntity.ok(ApiResponse.ok(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("分配失敗: " + e.getMessage()));
        }
    }
}

