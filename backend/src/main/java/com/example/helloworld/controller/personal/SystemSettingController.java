package com.example.helloworld.controller.personal;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.entity.personal.SystemSetting;
import com.example.helloworld.repository.personal.SystemSettingRepository;
import com.example.helloworld.service.personal.SystemSettingService;
import com.example.helloworld.service.personal.ConfigurationRefreshService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController("personalSystemSettingController")
@RequestMapping("/api/personal/system-settings")
@CrossOrigin(origins = "*")
public class SystemSettingController {

    @Autowired
    @Qualifier("personalSystemSettingService")
    private SystemSettingService systemSettingService;

    @Autowired
    private ConfigurationRefreshService configurationRefreshService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllSettings() {
        try {
            List<SystemSetting> settings = systemSettingService.getAllSettings();
            ensureDefaultSettingsExist(settings);
            settings = systemSettingService.getAllSettings();
            Map<String, Object> data = new HashMap<>();
            data.put("settings", settings);
            data.put("message", "獲取系統參數成功");
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取系統參數失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/{key}/value")
    public ResponseEntity<ApiResponse<Map<String, String>>> getSettingValue(@PathVariable String key) {
        String value = systemSettingService.getSettingValue(key, null);
        Map<String, String> data = new HashMap<>();
        if (value != null) {
            data.put("value", value);
            return ResponseEntity.ok(ApiResponse.ok(data));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("參數不存在"));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettingsByCategory(@PathVariable String category) {
        try {
            List<SystemSetting> settings = systemSettingService.getSettingsByCategory(category);
            Map<String, Object> data = new HashMap<>();
            data.put("settings", settings);
            data.put("message", "獲取系統參數成功");
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取系統參數失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettingByKey(@PathVariable String key) {
        try {
            Optional<SystemSetting> setting = systemSettingService.getSettingByKey(key);
            if (setting.isPresent()) {
                Map<String, Object> data = new HashMap<>();
                data.put("setting", setting.get());
                data.put("message", "獲取系統參數成功");
                return ResponseEntity.ok(ApiResponse.ok(data));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("參數不存在"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取系統參數失敗：" + e.getMessage()));
        }
    }

    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSetting(
            @PathVariable String key,
            @RequestBody Map<String, Object> request) {
        try {
            String value = (String) request.get("settingValue");
            String description = (String) request.get("description");
            SystemSetting update = new SystemSetting();
            update.setSettingValue(value);
            if (description != null) update.setDescription(description);
            SystemSetting saved = systemSettingService.updateSetting(key, update);
            try {
                configurationRefreshService.refreshConfig(key);
            } catch (Exception ex) {
                System.err.println("Failed to refresh config cache: " + ex.getMessage());
            }
            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("message", "更新系統參數成功");
            data.put("setting", saved);
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新失敗: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refreshConfig() {
        try {
            configurationRefreshService.refreshConfig();
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("配置刷新失敗: " + e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<SystemSetting>> createSetting(@RequestBody SystemSetting setting) {
        try {
            SystemSetting created = systemSettingService.createSetting(setting);
            configurationRefreshService.refreshConfig(created.getSettingKey());
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("創建失敗: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<ApiResponse<Void>> deleteSetting(@PathVariable String key) {
        try {
            systemSettingService.deleteSetting(key);
            configurationRefreshService.refreshConfig(key);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除失敗: " + e.getMessage()));
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
