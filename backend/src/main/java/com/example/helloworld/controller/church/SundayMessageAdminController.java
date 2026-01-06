package com.example.helloworld.controller.church;

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
    public ResponseEntity<Map<String, Object>> getAllMessages(
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
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", messagesPage.getContent());
            response.put("content", messagesPage.getContent());
            response.put("totalElements", messagesPage.getTotalElements());
            response.put("totalPages", messagesPage.getTotalPages());
            response.put("currentPage", messagesPage.getNumber());
            response.put("size", messagesPage.getSize());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "獲取主日信息失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 根據 ID 獲取主日信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMessageById(@PathVariable Long id) {
        try {
            SundayMessage message = sundayMessageService.getMessageById(id)
                .orElseThrow(() -> new RuntimeException("主日信息不存在: " + id));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", message);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 創建主日信息
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createMessage(@RequestBody SundayMessage message) {
        try {
            SundayMessage created = sundayMessageService.createMessage(message);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", created);
            response.put("message", "主日信息已建立");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "建立主日信息失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新主日信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateMessage(@PathVariable Long id, @RequestBody SundayMessage message) {
        try {
            SundayMessage updated = sundayMessageService.updateMessage(id, message);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "主日信息已更新");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 刪除主日信息
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteMessage(@PathVariable Long id) {
        try {
            sundayMessageService.deleteMessage(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "主日信息已刪除");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "刪除主日信息失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

