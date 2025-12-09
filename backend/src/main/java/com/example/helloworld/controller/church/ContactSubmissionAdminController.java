package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.ContactSubmission;
import com.example.helloworld.service.church.ContactSubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/church/admin/contact-submissions")
@CrossOrigin(origins = "*")
public class ContactSubmissionAdminController {

    @Autowired
    private ContactSubmissionService contactSubmissionService;

    /**
     * 獲取所有聯絡表單提交記錄（管理用，需要管理權限）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllSubmissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean isRead) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ContactSubmission> submissions;
            
            if (isRead != null) {
                submissions = contactSubmissionService.getSubmissionsByIsRead(isRead, pageable);
            } else {
                submissions = contactSubmissionService.getAllSubmissions(pageable);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", submissions.getContent());
            response.put("totalElements", submissions.getTotalElements());
            response.put("totalPages", submissions.getTotalPages());
            response.put("currentPage", submissions.getNumber());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "獲取聯絡表單記錄失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 根據 ID 獲取聯絡表單提交記錄（管理用）
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getSubmissionById(@PathVariable Long id) {
        Optional<ContactSubmission> submission = contactSubmissionService.getSubmissionById(id);
        if (submission.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", submission.get());
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "聯絡表單記錄不存在");
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 標記聯絡表單提交記錄為已讀（管理用）
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        try {
            // 獲取當前登入用戶
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String readBy = authentication != null ? authentication.getName() : null;
            
            ContactSubmission updated = contactSubmissionService.markAsRead(id, readBy);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "標記為已讀成功");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 刪除聯絡表單提交記錄（管理用）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteSubmission(@PathVariable Long id) {
        try {
            contactSubmissionService.deleteSubmission(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "聯絡表單記錄刪除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "刪除失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 獲取統計資訊（管理用）
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("unread", contactSubmissionService.countByIsRead(false));
            stats.put("read", contactSubmissionService.countByIsRead(true));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "獲取統計資訊失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

