package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.dto.invest.PortfolioRiskResultDetailDto;
import com.example.helloworld.dto.invest.PortfolioRiskResultPagedDto;
import com.example.helloworld.service.invest.PortfolioRiskResultService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/invest/portfolio-risk-results")
@CrossOrigin(origins = "*")
public class PortfolioRiskResultController {

    private final PortfolioRiskResultService portfolioRiskResultService;

    public PortfolioRiskResultController(PortfolioRiskResultService portfolioRiskResultService) {
        this.portfolioRiskResultService = portfolioRiskResultService;
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PageResponse<PortfolioRiskResultPagedDto>>> getPaged(
            @RequestParam(required = false) LocalDate tradeDate,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String recommendation,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Page<PortfolioRiskResultPagedDto> result = portfolioRiskResultService.getPaged(
                tradeDate, riskLevel, recommendation, page, size
            );
            PageResponse<PortfolioRiskResultPagedDto> response = new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢風險結果列表失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PortfolioRiskResultDetailDto>> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(portfolioRiskResultService.getById(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢風險結果失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<PortfolioRiskResultDetailDto>> getLatest(@RequestParam Long portfolioId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(portfolioRiskResultService.getLatest(portfolioId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢最新風險結果失敗：" + e.getMessage()));
        }
    }

    @PostMapping("/recalculate/{portfolioId}")
    public ResponseEntity<ApiResponse<PortfolioRiskResultDetailDto>> recalculate(@PathVariable Long portfolioId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(portfolioRiskResultService.recalculate(portfolioId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("重算風險結果失敗：" + e.getMessage()));
        }
    }
}
