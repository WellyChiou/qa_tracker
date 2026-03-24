package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.invest.RunAlertPollingJobResponseDto;
import com.example.helloworld.dto.invest.RunDailyPortfolioRiskJobResponseDto;
import com.example.helloworld.dto.invest.RunMarketAnalysisResponseDto;
import com.example.helloworld.dto.invest.RunPriceBackfillResponseDto;
import com.example.helloworld.dto.invest.RunPriceUpdateResponseDto;
import com.example.helloworld.service.invest.InvestJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/invest/jobs")
@CrossOrigin(origins = "*")
public class InvestJobController {

    private final InvestJobService investJobService;

    public InvestJobController(InvestJobService investJobService) {
        this.investJobService = investJobService;
    }

    @PostMapping("/run-daily-portfolio-risk-report")
    public ResponseEntity<ApiResponse<RunDailyPortfolioRiskJobResponseDto>> runDailyPortfolioRiskReport(
        @RequestParam(required = false) LocalDate reportDate
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                investJobService.runDailyPortfolioRiskReportForCurrentUser(reportDate)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("執行每日報告批次失敗：" + e.getMessage()));
        }
    }

    @PostMapping("/run-alert-polling")
    public ResponseEntity<ApiResponse<RunAlertPollingJobResponseDto>> runAlertPolling() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                investJobService.runAlertPollingForCurrentUser()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("執行警示輪詢失敗：" + e.getMessage()));
        }
    }

    @PostMapping("/run-price-update")
    public ResponseEntity<ApiResponse<RunPriceUpdateResponseDto>> runPriceUpdate() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                investJobService.runPriceUpdateForCurrentUser()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("執行持股行情更新失敗：" + e.getMessage()));
        }
    }

    @PostMapping("/run-price-backfill")
    public ResponseEntity<ApiResponse<RunPriceBackfillResponseDto>> runPriceBackfill(
        @RequestParam(defaultValue = "30") Integer days,
        @RequestParam(defaultValue = "HOLDINGS_AND_WATCHLIST") String scope
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                investJobService.runPriceBackfillForCurrentUser(days, scope)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("執行歷史行情回補失敗：" + e.getMessage()));
        }
    }

    @PostMapping("/run-market-analysis")
    public ResponseEntity<ApiResponse<RunMarketAnalysisResponseDto>> runMarketAnalysis(
        @RequestParam(defaultValue = "HOLDINGS_AND_WATCHLIST") String scope
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                investJobService.runMarketAnalysisForCurrentUser(scope)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("執行市場分析失敗：" + e.getMessage()));
        }
    }
}
