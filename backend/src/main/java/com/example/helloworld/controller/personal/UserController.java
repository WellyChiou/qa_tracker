package com.example.helloworld.controller.personal;

import com.example.helloworld.entity.personal.User;
import com.example.helloworld.repository.personal.UserRepository;
import com.example.helloworld.service.personal.LineBotService;
import com.example.helloworld.service.personal.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private LineBotService lineBotService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 檢查當前用戶是否有權限訪問指定 UID 的資源
     * 用戶只能訪問自己的資源，除非是 ADMIN
     */
    private boolean hasPermission(String uid) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // ADMIN 可以訪問所有資源
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }

        // 普通用戶只能訪問自己的資源
        String username = authentication.getName();
        Optional<User> currentUser = userRepository.findByUsername(username);
        if (currentUser.isPresent() && currentUser.get().getUid().equals(uid)) {
            return true;
        }

        return false;
    }

    /**
     * 獲取所有用戶
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * 根據 UID 獲取用戶
     */
    @GetMapping("/{uid}")
    public ResponseEntity<User> getUserByUid(@PathVariable String uid) {
        Optional<User> user = userService.getUserByUid(uid);
        return user.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 創建用戶
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody User user) {
        try {
            // 檢查用戶名是否已存在
            if (user.getUsername() != null && userService.usernameExists(user.getUsername())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "用戶名已存在: " + user.getUsername());
                return ResponseEntity.badRequest().body(response);
            }

            // 檢查郵箱是否已存在
            if (user.getEmail() != null && userService.emailExists(user.getEmail())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "郵箱已存在: " + user.getEmail());
                return ResponseEntity.badRequest().body(response);
            }

            User created = userService.createUser(user);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用戶創建成功");
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
     * 更新用戶
     */
    @PutMapping("/{uid}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable String uid, @RequestBody User user) {
        try {
            User updated = userService.updateUser(uid, user);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用戶更新成功");
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
     * 刪除用戶
     */
    @DeleteMapping("/{uid}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable String uid) {
        try {
            userService.deleteUser(uid);
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
     * 更新用戶角色
     */
    @PutMapping("/{uid}/roles")
    public ResponseEntity<Map<String, Object>> updateUserRoles(@PathVariable String uid, @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> roleIdsObj = (List<Object>) request.get("roleIds");
            List<Long> roleIds = new ArrayList<>();
            if (roleIdsObj != null) {
                for (Object id : roleIdsObj) {
                    if (id instanceof Number) {
                        roleIds.add(((Number) id).longValue());
                    } else if (id instanceof String) {
                        roleIds.add(Long.parseLong((String) id));
                    }
                }
            }
            User updated = userService.updateUserRoles(uid, roleIds);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用戶角色更新成功");
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
     * 更新用戶權限
     */
    @PutMapping("/{uid}/permissions")
    public ResponseEntity<Map<String, Object>> updateUserPermissions(@PathVariable String uid, @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> permissionIdsObj = (List<Object>) request.get("permissionIds");
            List<Long> permissionIds = new ArrayList<>();
            if (permissionIdsObj != null) {
                for (Object id : permissionIdsObj) {
                    if (id instanceof Number) {
                        permissionIds.add(((Number) id).longValue());
                    } else if (id instanceof String) {
                        permissionIds.add(Long.parseLong((String) id));
                    }
                }
            }
            User updated = userService.updateUserPermissions(uid, permissionIds);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用戶權限更新成功");
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
     * 綁定用戶 LINE 帳號
     */
    @PostMapping("/{uid}/bind-line")
    public ResponseEntity<Map<String, Object>> bindLineAccount(
            @PathVariable String uid,
            @RequestBody Map<String, String> request) {

        // 檢查權限：用戶只能綁定自己的帳號，除非是 ADMIN
        if (!hasPermission(uid)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "無權限訪問此資源");
            return ResponseEntity.status(403).body(response);
        }

        try {
            String lineUserId = request.get("lineUserId");
            if (lineUserId == null || lineUserId.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "LINE 用戶 ID 不能為空");
                return ResponseEntity.badRequest().body(response);
            }

            boolean success = lineBotService.bindUserLineId(uid, lineUserId.trim());

            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "LINE 帳號綁定成功");

                // 發送歡迎訊息
                String welcomeMessage = String.format(
                    "🎉 綁定成功！\n\n歡迎 %s 使用費用記錄 LINE Bot！\n\n" +
                    "📝 您現在可以直接在 LINE 中記錄費用：\n" +
                    "• 支出 食 外食 150 午餐\n" +
                    "• 收入 薪資 本薪 50000\n\n" +
                    "格式：類型 主類別 細項 金額 描述\n" +
                    "💡 輸入「幫助」查看更多功能",
                    userService.getUserByUid(uid).map(User::getDisplayName).orElse("用戶")
                );
                lineBotService.sendPushMessage(lineUserId, welcomeMessage);

                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "用戶不存在或綁定失敗");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "綁定失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 解除綁定用戶 LINE 帳號
     */
    @PostMapping("/{uid}/unbind-line")
    public ResponseEntity<Map<String, Object>> unbindLineAccount(@PathVariable String uid) {
        // 檢查權限：用戶只能解除綁定自己的帳號，除非是 ADMIN
        if (!hasPermission(uid)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "無權限訪問此資源");
            return ResponseEntity.status(403).body(response);
        }

        try {
            Optional<User> userOpt = userService.getUserByUid(uid);
            if (!userOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "用戶不存在");
                return ResponseEntity.badRequest().body(response);
            }

            User user = userOpt.get();
            String lineUserId = user.getLineUserId();

            // 直接更新 LINE User ID，避免觸發密碼更新邏輯
            user.setLineUserId(null);
            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "LINE 帳號解除綁定成功");

            // 發送通知訊息
            if (lineUserId != null && !lineUserId.trim().isEmpty()) {
                lineBotService.sendPushMessage(lineUserId, "🔌 您的 LINE 帳號已解除綁定，將不再收到費用提醒通知。");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "解除綁定失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 獲取用戶 LINE 綁定狀態
     */
    @GetMapping("/{uid}/line-status")
    public ResponseEntity<Map<String, Object>> getLineBindingStatus(@PathVariable String uid) {
        // 檢查權限：用戶只能查看自己的綁定狀態，除非是 ADMIN
        if (!hasPermission(uid)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "無權限訪問此資源");
            return ResponseEntity.status(403).body(response);
        }

        try {
            Optional<User> userOpt = userService.getUserByUid(uid);
            if (!userOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("isBound", user.getLineUserId() != null && !user.getLineUserId().trim().isEmpty());
            response.put("lineUserId", user.getLineUserId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "獲取綁定狀態失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
