package com.example.helloworld.controller;

import com.example.helloworld.entity.User;
import com.example.helloworld.service.LineBotService;
import com.example.helloworld.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    /**
     * 獲取所有用戶
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * 根據 UID 獲取用戶
     */
    @GetMapping("/{uid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> getUserByUid(@PathVariable String uid) {
        Optional<User> user = userService.getUserByUid(uid);
        return user.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 創建用戶
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
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
     * 綁定用戶 LINE 帳號
     */
    @PostMapping("/{uid}/bind-line")
    public ResponseEntity<Map<String, Object>> bindLineAccount(
            @PathVariable String uid,
            @RequestBody Map<String, String> request) {

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
                    "• 支出 餐費 150 午餐\n" +
                    "• 收入 薪水 50000\n\n" +
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

            user.setLineUserId(null);
            userService.updateUser(uid, user);

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

