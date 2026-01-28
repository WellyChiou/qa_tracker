package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.entity.church.PrayerRequest;
import com.example.helloworld.service.church.PrayerRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/church/admin/prayer-requests")
@CrossOrigin(origins = "*")
public class PrayerRequestAdminController {

    @Autowired
    private PrayerRequestService prayerRequestService;

    /**
     * 獲取所有代禱事項（管理用，包含未啟用的，支援分頁）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PrayerRequest>>> getAllPrayerRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<PrayerRequest> prayerRequestsPage = prayerRequestService.getAllPrayerRequests(pageable);
            PageResponse<PrayerRequest> pageResponse = new PageResponse<>(
                    prayerRequestsPage.getContent(),
                    prayerRequestsPage.getTotalElements(),
                    prayerRequestsPage.getTotalPages(),
                    prayerRequestsPage.getNumber(),
                    prayerRequestsPage.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(pageResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取代禱事項失敗: " + e.getMessage()));
        }
    }

    /**
     * 根據 ID 獲取代禱事項
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PrayerRequest>> getPrayerRequestById(@PathVariable Long id) {
        try {
            PrayerRequest prayerRequest = prayerRequestService.getPrayerRequestById(id)
                .orElseThrow(() -> new RuntimeException("代禱事項不存在: " + id));
            return ResponseEntity.ok(ApiResponse.ok(prayerRequest));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage()));
        }
    }

    /**
     * 創建代禱事項
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PrayerRequest>> createPrayerRequest(@RequestBody PrayerRequest prayerRequest) {
        try {
            PrayerRequest created = prayerRequestService.createPrayerRequest(prayerRequest);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("建立代禱事項失敗: " + e.getMessage()));
        }
    }

    /**
     * 更新代禱事項
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PrayerRequest>> updatePrayerRequest(@PathVariable Long id, @RequestBody PrayerRequest prayerRequest) {
        try {
            PrayerRequest updated = prayerRequestService.updatePrayerRequest(id, prayerRequest);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage()));
        }
    }

    /**
     * 刪除代禱事項
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deletePrayerRequest(@PathVariable Long id) {
        try {
            prayerRequestService.deletePrayerRequest(id);
            return ResponseEntity.ok(ApiResponse.ok("代禱事項已刪除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("刪除代禱事項失敗: " + e.getMessage()));
        }
    }
}
