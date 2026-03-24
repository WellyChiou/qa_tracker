package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.dto.invest.PortfolioAlertEventPagedDto;
import com.example.helloworld.service.invest.PortfolioAlertEventService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/invest/portfolio-alert-events")
@CrossOrigin(origins = "*")
public class PortfolioAlertEventController {

    private final PortfolioAlertEventService portfolioAlertEventService;

    public PortfolioAlertEventController(PortfolioAlertEventService portfolioAlertEventService) {
        this.portfolioAlertEventService = portfolioAlertEventService;
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PageResponse<PortfolioAlertEventPagedDto>>> getPaged(
        @RequestParam(required = false) Long portfolioId,
        @RequestParam(required = false) String triggerType,
        @RequestParam(required = false) LocalDateTime triggeredFrom,
        @RequestParam(required = false) LocalDateTime triggeredTo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Page<PortfolioAlertEventPagedDto> result = portfolioAlertEventService.getPaged(
                portfolioId, triggerType, triggeredFrom, triggeredTo, page, size
            );
            PageResponse<PortfolioAlertEventPagedDto> response = new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢警示事件列表失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<List<PortfolioAlertEventPagedDto>>> getLatest(
        @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(portfolioAlertEventService.getLatest(limit)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢最新警示事件失敗：" + e.getMessage()));
        }
    }
}
