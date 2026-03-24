package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.entity.invest.SystemSetting;
import com.example.helloworld.service.invest.system.InvestSystemSettingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/invest/system/maintenance/settings")
@CrossOrigin(origins = "*")
public class SystemMaintenanceController {

    private final InvestSystemSettingService investSystemSettingService;

    public SystemMaintenanceController(InvestSystemSettingService investSystemSettingService) {
        this.investSystemSettingService = investSystemSettingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemSetting>>> getAllSettings() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemSettingService.getAllSettings()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("取得系統設定失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<SystemSetting>>> getSettingsByCategory(@PathVariable String category) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemSettingService.getSettingsByCategory(category)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("取得分類設定失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<SystemSetting>> getSettingByKey(@PathVariable String key) {
        try {
            Optional<SystemSetting> setting = investSystemSettingService.getSettingByKey(key);
            if (setting.isPresent()) {
                return ResponseEntity.ok(ApiResponse.ok(setting.get()));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("找不到指定的系統設定"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("取得系統設定失敗：" + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SystemSetting>> createSetting(@RequestBody SystemSetting setting) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemSettingService.createSetting(setting)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("建立系統設定失敗：" + e.getMessage()));
        }
    }

    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<SystemSetting>> updateSetting(
        @PathVariable String key,
        @RequestBody SystemSetting settingUpdate
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemSettingService.updateSetting(key, settingUpdate)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新系統設定失敗：" + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refreshConfig() {
        try {
            investSystemSettingService.refreshConfig();
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刷新設定快取失敗：" + e.getMessage()));
        }
    }
}
