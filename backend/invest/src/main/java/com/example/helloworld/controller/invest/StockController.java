package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.dto.invest.StockOptionDto;
import com.example.helloworld.dto.invest.StockResponseDto;
import com.example.helloworld.dto.invest.StockUpsertRequestDto;
import com.example.helloworld.service.invest.StockService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invest/stocks")
@CrossOrigin(origins = "*")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PageResponse<StockResponseDto>>> getPaged(
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String ticker,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Page<StockResponseDto> result = stockService.getPaged(market, ticker, name, isActive, page, size);
            PageResponse<StockResponseDto> response = new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢股票列表失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<List<StockOptionDto>>> getOptions() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(stockService.getOptions()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢股票選項失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<StockResponseDto>>> getAll(
            @RequestParam(required = false) Boolean isActive
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(stockService.getAll(isActive)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢股票列表失敗：" + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StockResponseDto>> create(@RequestBody StockUpsertRequestDto request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(stockService.create(request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("新增股票失敗：" + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StockResponseDto>> update(
            @PathVariable Long id,
            @RequestBody StockUpsertRequestDto request
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(stockService.update(id, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新股票失敗：" + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            stockService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除股票失敗：" + e.getMessage()));
        }
    }
}
