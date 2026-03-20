package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.invest.PortfolioDashboardOverviewDto;
import com.example.helloworld.service.invest.PortfolioDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invest/portfolio-dashboard")
@CrossOrigin(origins = "*")
public class PortfolioDashboardController {

    private final PortfolioDashboardService portfolioDashboardService;

    public PortfolioDashboardController(PortfolioDashboardService portfolioDashboardService) {
        this.portfolioDashboardService = portfolioDashboardService;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<PortfolioDashboardOverviewDto>> getOverview() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(portfolioDashboardService.getOverview()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢持股總覽失敗：" + e.getMessage()));
        }
    }
}
