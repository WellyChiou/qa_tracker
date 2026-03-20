package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.dto.invest.PortfolioResponseDto;
import com.example.helloworld.dto.invest.PortfolioUpsertRequestDto;
import com.example.helloworld.service.invest.PortfolioService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invest/portfolios")
@CrossOrigin(origins = "*")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PageResponse<PortfolioResponseDto>>> getPaged(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Long stockId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Page<PortfolioResponseDto> result = portfolioService.getPaged(userId, stockId, isActive, page, size);
            PageResponse<PortfolioResponseDto> response = new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢持股列表失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<PortfolioResponseDto>>> getAll(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Long stockId,
            @RequestParam(required = false) Boolean isActive
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(portfolioService.getAll(userId, stockId, isActive)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢持股列表失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PortfolioResponseDto>> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(portfolioService.getById(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢持股明細失敗：" + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PortfolioResponseDto>> create(@RequestBody PortfolioUpsertRequestDto request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(portfolioService.create(request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("新增持股失敗：" + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PortfolioResponseDto>> update(
            @PathVariable Long id,
            @RequestBody PortfolioUpsertRequestDto request
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(portfolioService.update(id, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新持股失敗：" + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            portfolioService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除持股失敗：" + e.getMessage()));
        }
    }
}
