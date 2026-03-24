package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.invest.StrategySettingsDto;
import com.example.helloworld.dto.invest.StrategySettingsUpdateRequestDto;
import com.example.helloworld.service.invest.system.InvestStrategySettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invest/system/strategy-settings")
@CrossOrigin(origins = "*")
public class SystemStrategySettingController {

    private final InvestStrategySettingService investStrategySettingService;

    public SystemStrategySettingController(InvestStrategySettingService investStrategySettingService) {
        this.investStrategySettingService = investStrategySettingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<StrategySettingsDto>> getStrategySettings() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investStrategySettingService.getSettings()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("取得策略設定失敗：" + e.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<ApiResponse<StrategySettingsDto>> updateStrategySettings(
        @RequestBody StrategySettingsUpdateRequestDto request
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investStrategySettingService.updateSettings(request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新策略設定失敗：" + e.getMessage()));
        }
    }
}
