package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.dto.invest.OpportunitySignalDetailDto;
import com.example.helloworld.dto.invest.OpportunitySignalPagedDto;
import com.example.helloworld.service.invest.OpportunitySignalService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/invest/opportunity-signals")
@CrossOrigin(origins = "*")
public class OpportunitySignalController {

    private final OpportunitySignalService opportunitySignalService;

    public OpportunitySignalController(OpportunitySignalService opportunitySignalService) {
        this.opportunitySignalService = opportunitySignalService;
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PageResponse<OpportunitySignalPagedDto>>> getPaged(
        @RequestParam(required = false) LocalDate tradeDate,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String recommendation,
        @RequestParam(required = false) String signalType,
        @RequestParam(required = false) String ticker,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Page<OpportunitySignalPagedDto> result = opportunitySignalService.getPaged(
                tradeDate,
                status,
                recommendation,
                signalType,
                ticker,
                page,
                size
            );
            PageResponse<OpportunitySignalPagedDto> response = new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢機會訊號列表失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OpportunitySignalDetailDto>> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(opportunitySignalService.getById(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢機會訊號明細失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<OpportunitySignalDetailDto>> getLatest(@RequestParam Long stockId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(opportunitySignalService.getLatest(stockId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢最新機會訊號失敗：" + e.getMessage()));
        }
    }
}
