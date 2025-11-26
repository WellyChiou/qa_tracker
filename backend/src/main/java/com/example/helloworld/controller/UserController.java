package com.example.helloworld.controller;

import com.example.helloworld.entity.User;
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
}

