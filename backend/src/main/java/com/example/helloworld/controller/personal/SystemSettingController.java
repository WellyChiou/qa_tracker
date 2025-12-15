package com.example.helloworld.controller.personal;

import com.example.helloworld.entity.personal.SystemSetting;
import com.example.helloworld.repository.personal.SystemSettingRepository;
import com.example.helloworld.service.personal.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController("personalSystemSettingController")
@RequestMapping("/api/personal/system-settings")
@CrossOrigin(origins = "*")
public class SystemSettingController {

    @Autowired
    @Qualifier("personalSystemSettingRepository")
    private SystemSettingRepository systemSettingRepository;

    @Autowired
    @Qualifier("personalSystemSettingService")
    private SystemSettingService systemSettingService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllSettings() {
        try {
            List<SystemSetting> settings = systemSettingService.getAllSettings();
            
            // Map Config entities to the structure expected by the frontend (SystemSetting-like)
            // SystemSetting is already in the correct format, but we wrap it to match frontend expectation
            
            // If certain essential keys are missing, add them as defaults (so they appear in the UI)
            ensureDefaultSettingsExist(settings);
            
            // Reload after ensuring defaults
            settings = systemSettingService.getAllSettings();

            Map<String, Object> response = new HashMap<>();
            response.put("settings", settings);
            response.put("message", "獲取系統參數成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取系統參數失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{key}/value")
    public ResponseEntity<Map<String, String>> getSettingValue(@PathVariable String key) {
        String value = systemSettingService.getSettingValue(key, null);
        Map<String, String> response = new HashMap<>();
        if (value != null) {
            response.put("value", value);
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{key}")
    public ResponseEntity<Map<String, Object>> updateSetting(
            @PathVariable String key,
            @RequestBody Map<String, Object> request) {
        try {
            String value = (String) request.get("settingValue");
            // Also accept 'description' if provided
            String description = (String) request.get("description");
            
            SystemSetting update = new SystemSetting();
            update.setSettingValue(value);
            update.setDescription(description);
            
            SystemSetting saved = systemSettingService.updateSetting(key, update);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "更新系統參數成功");
            response.put("setting", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshConfig() {
        // Personal site reads directly from DB via SystemSettingService, no caching layer to refresh.
        // But we return success to satisfy the frontend.
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "配置已更新 (無緩存)");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "配置刷新失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    private void ensureDefaultSettingsExist(List<SystemSetting> settings) {
        Set<String> existingKeys = settings.stream()
            .map(SystemSetting::getSettingKey)
            .collect(Collectors.toSet());
            
        // Add default keys if missing
        addDefaultIfMissing(existingKeys, "backup.enabled", "true", "啟用資料庫備份", "backup", "boolean");
        addDefaultIfMissing(existingKeys, "backup.retention_days", "7", "備份保留天數", "backup", "number");
        addDefaultIfMissing(existingKeys, "backup.mysql_service", "mysql", "MySQL 服務名稱", "backup", "string");
        addDefaultIfMissing(existingKeys, "backup.mysql_root_password", "rootpassword", "MySQL Root 密碼", "backup", "string");
        
        addDefaultIfMissing(existingKeys, "line.bot.channel-token", "", "Channel Access Token", "linebot", "string");
        addDefaultIfMissing(existingKeys, "line.bot.channel-secret", "", "Channel Secret", "linebot", "string");
        addDefaultIfMissing(existingKeys, "line.bot.daily-reminder-enabled", "true", "啟用每日記帳提醒", "linebot", "boolean");
        addDefaultIfMissing(existingKeys, "line.bot.daily-reminder-time", "20:00", "每日提醒時間 (HH:mm)", "linebot", "string");
        
        addDefaultIfMissing(existingKeys, "jwt.secret", "F/cPluFKK3/44X5iX9GdY6P7Ye+BIDdBTw6uljBTl9o=", "JWT Secret", "jwt", "string");
        addDefaultIfMissing(existingKeys, "jwt.access-token-expiration", "3600000", "Access Token 過期時間 (ms)", "jwt", "number");
        addDefaultIfMissing(existingKeys, "jwt.refresh-token-expiration", "604800000", "Refresh Token 過期時間 (ms)", "jwt", "number");
        addDefaultIfMissing(existingKeys, "jwt.refresh-token-enabled", "true", "啟用 Refresh Token", "jwt", "boolean");
        
        addDefaultIfMissing(existingKeys, "gitlab_token", "", "GitLab API Token", "system", "string");
    }
    
    private void addDefaultIfMissing(Set<String> existingKeys,  
                                     String key, String defaultValue, String desc, String category, String type) {
        if (!existingKeys.contains(key)) {
            SystemSetting setting = new SystemSetting();
            setting.setSettingKey(key);
            setting.setSettingValue(defaultValue);
            setting.setDescription(desc);
            setting.setCategory(category);
            setting.setSettingType(type);
            setting.setIsEditable(true);
            try {
                systemSettingService.createSetting(setting);
            } catch (Exception e) {
                // Ignore if created concurrently
            }
        }
    }
}

