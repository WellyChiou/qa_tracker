package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.invest.PortfolioAlertSettingDto;
import com.example.helloworld.dto.invest.PortfolioAlertSettingUpsertRequestDto;
import com.example.helloworld.service.invest.PortfolioAlertSettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invest/portfolio-alert-settings")
@CrossOrigin(origins = "*")
public class PortfolioAlertSettingController {

    private final PortfolioAlertSettingService portfolioAlertSettingService;

    public PortfolioAlertSettingController(PortfolioAlertSettingService portfolioAlertSettingService) {
        this.portfolioAlertSettingService = portfolioAlertSettingService;
    }

    @GetMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<PortfolioAlertSettingDto>> getByPortfolioId(@PathVariable Long portfolioId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(portfolioAlertSettingService.getByPortfolioId(portfolioId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢警示設定失敗：" + e.getMessage()));
        }
    }

    @PutMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<PortfolioAlertSettingDto>> upsert(@PathVariable Long portfolioId,
                                                                         @RequestBody PortfolioAlertSettingUpsertRequestDto request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(portfolioAlertSettingService.upsert(portfolioId, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("儲存警示設定失敗：" + e.getMessage()));
        }
    }
}
