package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.entity.church.SundayMessage;
import com.example.helloworld.service.church.SundayMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/church/admin/sunday-messages")
@CrossOrigin(origins = "*")
public class SundayMessageAdminController {

    @Autowired
    private SundayMessageService sundayMessageService;

    /**
     * 獲取所有主日信息（管理用，包含未啟用的，支持分頁和過濾）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SundayMessage>>> getAllMessages(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            LocalDate filterStartDate = (startDate != null && !startDate.trim().isEmpty()) ? LocalDate.parse(startDate) : null;
            LocalDate filterEndDate = (endDate != null && !endDate.trim().isEmpty()) ? LocalDate.parse(endDate) : null;
            Page<SundayMessage> messagesPage = sundayMessageService.getAllMessages(
                title, filterStartDate, filterEndDate, serviceType, isActive, page, size);
            PageResponse<SundayMessage> pageResponse = new PageResponse<>(
                    messagesPage.getContent(),
                    messagesPage.getTotalElements(),
                    messagesPage.getTotalPages(),
                    messagesPage.getNumber(),
                    messagesPage.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(pageResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取主日信息失敗: " + e.getMessage()));
        }
    }

    /**
     * 根據 ID 獲取主日信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SundayMessage>> getMessageById(@PathVariable Long id) {
        try {
            SundayMessage message = sundayMessageService.getMessageById(id)
                .orElseThrow(() -> new RuntimeException("主日信息不存在: " + id));
            return ResponseEntity.ok(ApiResponse.ok(message));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage()));
        }
    }

    /**
     * 創建主日信息
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SundayMessage>> createMessage(@RequestBody SundayMessage message) {
        try {
            SundayMessage created = sundayMessageService.createMessage(message);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("建立主日信息失敗: " + e.getMessage()));
        }
    }

    /**
     * 更新主日信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SundayMessage>> updateMessage(@PathVariable Long id, @RequestBody SundayMessage message) {
        try {
            SundayMessage updated = sundayMessageService.updateMessage(id, message);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage()));
        }
    }

    /**
     * 刪除主日信息
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteMessage(@PathVariable Long id) {
        try {
            sundayMessageService.deleteMessage(id);
            return ResponseEntity.ok(ApiResponse.ok("主日信息已刪除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("刪除主日信息失敗: " + e.getMessage()));
        }
    }
}

