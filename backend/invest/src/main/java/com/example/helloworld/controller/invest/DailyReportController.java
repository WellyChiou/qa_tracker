package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.dto.invest.DailyReportDetailDto;
import com.example.helloworld.dto.invest.DailyReportPagedDto;
import com.example.helloworld.service.invest.DailyReportService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/invest/daily-reports")
@CrossOrigin(origins = "*")
public class DailyReportController {

    private final DailyReportService dailyReportService;

    public DailyReportController(DailyReportService dailyReportService) {
        this.dailyReportService = dailyReportService;
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PageResponse<DailyReportPagedDto>>> getPaged(
        @RequestParam(required = false) LocalDate reportDateFrom,
        @RequestParam(required = false) LocalDate reportDateTo,
        @RequestParam(required = false) String reportType,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Page<DailyReportPagedDto> result = dailyReportService.getPaged(
                reportDateFrom, reportDateTo, reportType, status, page, size
            );
            PageResponse<DailyReportPagedDto> response = new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢每日報告列表失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<DailyReportDetailDto>> getLatest(
        @RequestParam(defaultValue = "PORTFOLIO_RISK_DAILY") String reportType
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(dailyReportService.getLatest(reportType)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢最新每日報告失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DailyReportDetailDto>> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(dailyReportService.getById(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢每日報告失敗：" + e.getMessage()));
        }
    }
}
