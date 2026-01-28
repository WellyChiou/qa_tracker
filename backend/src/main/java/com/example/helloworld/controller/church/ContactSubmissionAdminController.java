package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
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
    public ResponseEntity<ApiResponse<PageResponse<ContactSubmission>>> getAllSubmissions(
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
            
            PageResponse<ContactSubmission> pageResponse = new PageResponse<>(
                submissions.getContent(),
                submissions.getTotalElements(),
                submissions.getTotalPages(),
                submissions.getNumber(),
                submissions.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(pageResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取聯絡表單記錄失敗: " + e.getMessage()));
        }
    }

    /**
     * 根據 ID 獲取聯絡表單提交記錄（管理用）
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactSubmission>> getSubmissionById(@PathVariable Long id) {
        Optional<ContactSubmission> submission = contactSubmissionService.getSubmissionById(id);
        if (submission.isPresent()) {
            return ResponseEntity.ok(ApiResponse.ok(submission.get()));
        } else {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(ApiResponse.fail("聯絡表單記錄不存在"));
        }
    }

    /**
     * 標記聯絡表單提交記錄為已讀（管理用）
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<ContactSubmission>> markAsRead(@PathVariable Long id) {
        try {
            // 獲取當前登入用戶
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String readBy = authentication != null ? authentication.getName() : null;
            
            ContactSubmission updated = contactSubmissionService.markAsRead(id, readBy);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新失敗: " + e.getMessage()));
        }
    }

    /**
     * 刪除聯絡表單提交記錄（管理用）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSubmission(@PathVariable Long id) {
        try {
            contactSubmissionService.deleteSubmission(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除失敗: " + e.getMessage()));
        }
    }

    /**
     * 獲取統計資訊（管理用）
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("unread", contactSubmissionService.countByIsRead(false));
            stats.put("read", contactSubmissionService.countByIsRead(true));
            
            return ResponseEntity.ok(ApiResponse.ok(stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取統計資訊失敗: " + e.getMessage()));
        }
    }
}

