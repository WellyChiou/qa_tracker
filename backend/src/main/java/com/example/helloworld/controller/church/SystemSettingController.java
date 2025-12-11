package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.SystemSetting;
import com.example.helloworld.service.church.SystemSettingService;
import com.example.helloworld.service.church.ConfigurationRefreshService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/church/admin/system-settings")
@CrossOrigin(origins = "*")
public class SystemSettingController {

    @Autowired
    private SystemSettingService systemSettingService;

    @Autowired
    private ConfigurationRefreshService configurationRefreshService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllSettings() {
        try {
            List<SystemSetting> settings = systemSettingService.getAllSettings();
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

    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getSettingsByCategory(@PathVariable String category) {
        try {
            List<SystemSetting> settings = systemSettingService.getSettingsByCategory(category);
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

    @GetMapping("/{key}")
    public ResponseEntity<Map<String, Object>> getSettingByKey(@PathVariable String key) {
        try {
            Optional<SystemSetting> setting = systemSettingService.getSettingByKey(key);
            if (setting.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("setting", setting.get());
                response.put("message", "獲取系統參數成功");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取系統參數失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{key}")
    public ResponseEntity<Map<String, Object>> updateSetting(
            @PathVariable String key,
            @RequestBody SystemSetting settingUpdate) {
        try {
            SystemSetting updated = systemSettingService.updateSetting(key, settingUpdate);
            // 更新配置後，刷新配置緩存
            configurationRefreshService.refreshConfig(key);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "更新系統參數成功，配置已刷新");
            response.put("setting", updated);
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
        try {
            configurationRefreshService.refreshConfig();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "配置刷新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "配置刷新失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSetting(@RequestBody SystemSetting setting) {
        try {
            SystemSetting created = systemSettingService.createSetting(setting);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "創建系統參數成功");
            response.put("setting", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "創建失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

