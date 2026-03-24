package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.dto.invest.StrengthSnapshotDetailDto;
import com.example.helloworld.dto.invest.StrengthSnapshotPagedDto;
import com.example.helloworld.service.invest.StrengthSnapshotService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/invest/strength-snapshots")
@CrossOrigin(origins = "*")
public class StrengthSnapshotController {

    private final StrengthSnapshotService strengthSnapshotService;

    public StrengthSnapshotController(StrengthSnapshotService strengthSnapshotService) {
        this.strengthSnapshotService = strengthSnapshotService;
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PageResponse<StrengthSnapshotPagedDto>>> getPaged(
        @RequestParam(required = false) LocalDate tradeDate,
        @RequestParam(required = false) String strengthLevel,
        @RequestParam(required = false) String universeType,
        @RequestParam(required = false) Long watchlistId,
        @RequestParam(required = false) String ticker,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Page<StrengthSnapshotPagedDto> result = strengthSnapshotService.getPaged(
                tradeDate,
                strengthLevel,
                universeType,
                watchlistId,
                ticker,
                page,
                size
            );
            PageResponse<StrengthSnapshotPagedDto> response = new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢強勢分析列表失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StrengthSnapshotDetailDto>> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(strengthSnapshotService.getById(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢強勢分析明細失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<StrengthSnapshotDetailDto>> getLatest(
        @RequestParam Long stockId,
        @RequestParam(required = false) String universeType
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(strengthSnapshotService.getLatest(stockId, universeType)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢最新強勢分析失敗：" + e.getMessage()));
        }
    }
}
