package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.dto.invest.StockPriceDailyResponseDto;
import com.example.helloworld.dto.invest.StockPriceDailyUpsertRequestDto;
import com.example.helloworld.service.invest.StockPriceDailyService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/invest/stock-price-dailies")
@CrossOrigin(origins = "*")
public class StockPriceDailyController {

    private final StockPriceDailyService stockPriceDailyService;

    public StockPriceDailyController(StockPriceDailyService stockPriceDailyService) {
        this.stockPriceDailyService = stockPriceDailyService;
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PageResponse<StockPriceDailyResponseDto>>> getPaged(
            @RequestParam(required = false) Long stockId,
            @RequestParam(required = false) String ticker,
            @RequestParam(required = false) LocalDate tradeDateFrom,
            @RequestParam(required = false) LocalDate tradeDateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Page<StockPriceDailyResponseDto> result = stockPriceDailyService.getPaged(stockId, ticker, tradeDateFrom, tradeDateTo, page, size);
            PageResponse<StockPriceDailyResponseDto> response = new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢每日行情列表失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<StockPriceDailyResponseDto>>> getAll(
            @RequestParam(required = false) Long stockId,
            @RequestParam(required = false) String ticker,
            @RequestParam(required = false) LocalDate tradeDateFrom,
            @RequestParam(required = false) LocalDate tradeDateTo
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                stockPriceDailyService.getAll(stockId, ticker, tradeDateFrom, tradeDateTo)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢每日行情列表失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<List<StockPriceDailyResponseDto>>> getLatest(
            @RequestParam(required = false) Long stockId,
            @RequestParam(required = false) String ticker,
            @RequestParam(required = false) LocalDate tradeDateFrom,
            @RequestParam(required = false) LocalDate tradeDateTo
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                stockPriceDailyService.getLatest(stockId, ticker, tradeDateFrom, tradeDateTo)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢最新每日行情失敗：" + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StockPriceDailyResponseDto>> create(@RequestBody StockPriceDailyUpsertRequestDto request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(stockPriceDailyService.create(request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("新增每日行情失敗：" + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StockPriceDailyResponseDto>> update(
            @PathVariable Long id,
            @RequestBody StockPriceDailyUpsertRequestDto request
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(stockPriceDailyService.update(id, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新每日行情失敗：" + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            stockPriceDailyService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除每日行情失敗：" + e.getMessage()));
        }
    }
}
