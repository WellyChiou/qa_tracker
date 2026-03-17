package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.entity.church.SystemSetting;
import com.example.helloworld.service.church.SystemSettingService;
import com.example.helloworld.service.church.ConfigurationRefreshService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/church/admin/system-settings")
@CrossOrigin(origins = "*")
@Component("churchSystemSettingController")
public class SystemSettingController {

    @Autowired
    @Qualifier("churchSystemSettingService")
    private SystemSettingService systemSettingService;

    @Autowired
    private ConfigurationRefreshService configurationRefreshService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemSetting>>> getAllSettings() {
        try {
            List<SystemSetting> settings = systemSettingService.getAllSettings();
            return ResponseEntity.ok(ApiResponse.ok(settings));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取系統參數失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<SystemSetting>>> getSettingsByCategory(@PathVariable String category) {
        try {
            List<SystemSetting> settings = systemSettingService.getSettingsByCategory(category);
            return ResponseEntity.ok(ApiResponse.ok(settings));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取系統參數失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<SystemSetting>> getSettingByKey(@PathVariable String key) {
        try {
            Optional<SystemSetting> setting = systemSettingService.getSettingByKey(key);
            if (setting.isPresent()) {
                return ResponseEntity.ok(ApiResponse.ok(setting.get()));
            } else {
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(ApiResponse.fail("找不到指定的系統參數"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取系統參數失敗：" + e.getMessage()));
        }
    }

    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<SystemSetting>> updateSetting(
            @PathVariable String key,
            @RequestBody SystemSetting settingUpdate) {
        try {
            SystemSetting updated = systemSettingService.updateSetting(key, settingUpdate);
            // 更新配置後，刷新配置緩存
            configurationRefreshService.refreshConfig(key);
            return ResponseEntity.ok(ApiResponse.ok(updated));
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
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("創建失敗: " + e.getMessage()));
        }
    }
}

