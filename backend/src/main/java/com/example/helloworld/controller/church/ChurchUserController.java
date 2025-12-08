package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.ChurchUser;
import com.example.helloworld.service.church.ChurchUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
     * 獲取所有用戶
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_CHURCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        try {
            List<ChurchUser> users = churchUserService.getAllUsers();
            // 確保在事務內初始化所有懶加載的關聯
            for (ChurchUser user : users) {
                // 觸發懶加載，確保在事務內完成
                if (user.getRoles() != null) {
                    user.getRoles().size();
                    // 初始化角色的權限
                    for (var role : user.getRoles()) {
                        if (role.getPermissions() != null) {
                            role.getPermissions().size();
                        }
                    }
                }
                if (user.getPermissions() != null) {
                    user.getPermissions().size();
                }
            }
            Map<String, Object> response = new HashMap<>();
            response.put("users", users);
            response.put("message", "獲取用戶列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace(); // 輸出錯誤堆棧以便調試
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取用戶列表失敗：" + e.getMessage());
            error.put("details", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 根據 UID 獲取用戶
     */
    @GetMapping("/{uid}")
    @PreAuthorize("hasAuthority('PERM_CHURCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserByUid(@PathVariable String uid) {
        try {
            Optional<ChurchUser> user = churchUserService.getUserByUid(uid);
            if (user.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("user", user.get());
                response.put("message", "獲取用戶成功");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取用戶失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 創建用戶
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CHURCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody ChurchUser user) {
        try {
            ChurchUser created = churchUserService.createUser(user);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用戶創建成功");
            response.put("user", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "創建失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新用戶
     */
    @PutMapping("/{uid}")
    @PreAuthorize("hasAuthority('PERM_CHURCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable String uid, @RequestBody ChurchUser user) {
        try {
            ChurchUser updated = churchUserService.updateUser(uid, user);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用戶更新成功");
            response.put("user", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 刪除用戶
     */
    @DeleteMapping("/{uid}")
    @PreAuthorize("hasAuthority('PERM_CHURCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable String uid) {
        try {
            churchUserService.deleteUser(uid);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用戶刪除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "刪除失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 為用戶分配角色
     */
    @PostMapping("/{uid}/roles")
    @PreAuthorize("hasAuthority('PERM_CHURCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> assignRoles(
            @PathVariable String uid, 
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> roleIds = (List<Long>) request.get("roleIds");
            ChurchUser user = churchUserService.assignRoles(uid, roleIds);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "角色分配成功");
            response.put("user", user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "分配失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

