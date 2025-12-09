package com.example.helloworld.controller.personal;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用於生成 BCrypt 密碼 Hash 的工具 Controller
 * 僅用於開發環境，生產環境應該移除或禁用
 */
@RestController
@RequestMapping("/api/utils")
@CrossOrigin(origins = "*")
public class PasswordHashController {

    @GetMapping("/hash-password")
    public Map<String, String> hashPassword(@RequestParam String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(password);
        
        Map<String, String> result = new HashMap<>();
        result.put("password", password);
        result.put("hash", hash);
        result.put("message", "使用此 hash 更新數據庫中的密碼欄位");
        
        return result;
    }

    @GetMapping("/verify-password")
    public Map<String, Object> verifyPassword(@RequestParam String password, @RequestParam String hash) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean matches = encoder.matches(password, hash);
        
        Map<String, Object> result = new HashMap<>();
        result.put("matches", matches);
        result.put("message", matches ? "密碼匹配" : "密碼不匹配");
        
        return result;
    }
}
