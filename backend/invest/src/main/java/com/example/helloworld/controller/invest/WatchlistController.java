package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.dto.invest.WatchlistDefaultDto;
import com.example.helloworld.dto.invest.WatchlistItemDto;
import com.example.helloworld.dto.invest.WatchlistItemUpsertRequestDto;
import com.example.helloworld.service.invest.WatchlistService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invest/watchlists")
@CrossOrigin(origins = "*")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping("/default")
    public ResponseEntity<ApiResponse<WatchlistDefaultDto>> getDefaultWatchlist() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(watchlistService.getDefaultWatchlist()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢預設觀察清單失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/default/items/paged")
    public ResponseEntity<ApiResponse<PageResponse<WatchlistItemDto>>> getDefaultItemsPaged(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Page<WatchlistItemDto> result = watchlistService.getDefaultItemsPaged(page, size);
            PageResponse<WatchlistItemDto> response = new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢觀察清單標的失敗：" + e.getMessage()));
        }
    }

    @PostMapping("/default/items")
    public ResponseEntity<ApiResponse<WatchlistItemDto>> addDefaultItem(
        @RequestBody WatchlistItemUpsertRequestDto request
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(watchlistService.addDefaultItem(request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("新增觀察清單標的失敗：" + e.getMessage()));
        }
    }

    @DeleteMapping("/default/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeDefaultItem(@PathVariable Long itemId) {
        try {
            watchlistService.removeDefaultItem(itemId);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("移除觀察清單標的失敗：" + e.getMessage()));
        }
    }
}
