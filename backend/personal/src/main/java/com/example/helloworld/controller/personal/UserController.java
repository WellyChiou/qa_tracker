package com.example.helloworld.controller.personal;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.entity.personal.User;
import com.example.helloworld.repository.personal.UserRepository;
import com.example.helloworld.service.line.LineBotService;
import com.example.helloworld.service.personal.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/personal/users")
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
    @Deprecated
    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PageResponse<User>>> getAllUsersPaged(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Boolean isEnabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<User> usersPage = userService.getAllUsers(username, email, roleId, isEnabled, page, size);
            PageResponse<User> pageResponse = new PageResponse<>(
                usersPage.getContent(),
                usersPage.getTotalElements(),
                usersPage.getTotalPages(),
                usersPage.getNumber(),
                usersPage.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(pageResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取用戶列表失敗：" + e.getMessage()));
        }
    }

    /**
     * 根據 UID 獲取用戶
     */
    @GetMapping("/{uid}")
    public ResponseEntity<ApiResponse<User>> getUserByUid(@PathVariable String uid) {
        Optional<User> user = userService.getUserByUid(uid);
        return user.map(u -> ResponseEntity.ok(ApiResponse.ok(u)))
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("用戶不存在")));
    }

    /**
     * 創建用戶
     */
    @PostMapping
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody User user) {
        try {
            if (user.getUsername() != null && userService.usernameExists(user.getUsername())) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("用戶名已存在: " + user.getUsername()));
            }
            if (user.getEmail() != null && userService.emailExists(user.getEmail())) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("郵箱已存在: " + user.getEmail()));
            }
            User created = userService.createUser(user);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("創建失敗: " + e.getMessage()));
        }
    }

    /**
     * 更新用戶
     */
    @PutMapping("/{uid}")
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable String uid, @RequestBody User user) {
        try {
            User updated = userService.updateUser(uid, user);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新失敗: " + e.getMessage()));
        }
    }

    /**
     * 刪除用戶
     */
    @DeleteMapping("/{uid}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String uid) {
        try {
            userService.deleteUser(uid);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除失敗: " + e.getMessage()));
        }
    }

    /**
     * 更新用戶角色
     */
    @PutMapping("/{uid}/roles")
    public ResponseEntity<ApiResponse<User>> updateUserRoles(@PathVariable String uid, @RequestBody Map<String, Object> request) {
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
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新失敗: " + e.getMessage()));
        }
    }

    /**
     * 更新用戶權限
     */
    @PutMapping("/{uid}/permissions")
    public ResponseEntity<ApiResponse<User>> updateUserPermissions(@PathVariable String uid, @RequestBody Map<String, Object> request) {
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
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新失敗: " + e.getMessage()));
        }
    }

    /**
     * 綁定用戶 LINE 帳號
     */
    @PostMapping("/{uid}/bind-line")
    public ResponseEntity<ApiResponse<Void>> bindLineAccount(
            @PathVariable String uid,
            @RequestBody Map<String, String> request) {

        if (!hasPermission(uid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail("無權限訪問此資源"));
        }

        try {
            String lineUserId = request.get("lineUserId");
            if (lineUserId == null || lineUserId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("LINE 用戶 ID 不能為空"));
            }

            boolean success = lineBotService.bindUserLineId(uid, lineUserId.trim());

            if (success) {
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
                return ResponseEntity.ok(ApiResponse.ok(null));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.fail("用戶不存在或綁定失敗"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("綁定失敗: " + e.getMessage()));
        }
    }

    /**
     * 解除綁定用戶 LINE 帳號
     */
    @PostMapping("/{uid}/unbind-line")
    public ResponseEntity<ApiResponse<Void>> unbindLineAccount(@PathVariable String uid) {
        if (!hasPermission(uid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail("無權限訪問此資源"));
        }

        try {
            Optional<User> userOpt = userService.getUserByUid(uid);
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("用戶不存在"));
            }

            User user = userOpt.get();
            String lineUserId = user.getLineUserId();

            user.setLineUserId(null);
            userRepository.save(user);

            if (lineUserId != null && !lineUserId.trim().isEmpty()) {
                lineBotService.sendPushMessage(lineUserId, "🔌 您的 LINE 帳號已解除綁定，將不再收到費用提醒通知。");
            }

            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("解除綁定失敗: " + e.getMessage()));
        }
    }

    /**
     * 獲取用戶 LINE 綁定狀態
     */
    @GetMapping("/{uid}/line-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLineBindingStatus(@PathVariable String uid) {
        if (!hasPermission(uid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail("無權限訪問此資源"));
        }

        try {
            Optional<User> userOpt = userService.getUserByUid(uid);
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("用戶不存在"));
            }

            User user = userOpt.get();
            Map<String, Object> data = new HashMap<>();
            data.put("isBound", user.getLineUserId() != null && !user.getLineUserId().trim().isEmpty());
            data.put("lineUserId", user.getLineUserId());

            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取綁定狀態失敗: " + e.getMessage()));
        }
    }
}
