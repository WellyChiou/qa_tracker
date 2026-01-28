package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.entity.church.Announcement;
import com.example.helloworld.service.church.AnnouncementService;
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
@RequestMapping("/api/church/admin/announcements")
@CrossOrigin(origins = "*")
public class AnnouncementAdminController {

    @Autowired
    private AnnouncementService announcementService;

    /**
     * 獲取所有公告（管理用，包含未啟用的，支援分頁）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Announcement>>> getAllAnnouncements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("isPinned").descending().and(Sort.by("publishDate").descending()));
            Page<Announcement> announcementsPage = announcementService.getAllAnnouncements(pageable);
            PageResponse<Announcement> pageResponse = new PageResponse<>(
                    announcementsPage.getContent(),
                    announcementsPage.getTotalElements(),
                    announcementsPage.getTotalPages(),
                    announcementsPage.getNumber(),
                    announcementsPage.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(pageResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取公告失敗: " + e.getMessage()));
        }
    }

    /**
     * 根據 ID 獲取公告
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Announcement>> getAnnouncementById(@PathVariable Long id) {
        try {
            Announcement announcement = announcementService.getAnnouncementById(id)
                .orElseThrow(() -> new RuntimeException("公告不存在: " + id));
            return ResponseEntity.ok(ApiResponse.ok(announcement));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage()));
        }
    }

    /**
     * 創建公告
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Announcement>> createAnnouncement(@RequestBody Announcement announcement) {
        try {
            Announcement created = announcementService.createAnnouncement(announcement);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("建立公告失敗: " + e.getMessage()));
        }
    }

    /**
     * 更新公告
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Announcement>> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement announcement) {
        try {
            Announcement updated = announcementService.updateAnnouncement(id, announcement);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage()));
        }
    }

    /**
     * 刪除公告
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteAnnouncement(@PathVariable Long id) {
        try {
            announcementService.deleteAnnouncement(id);
            return ResponseEntity.ok(ApiResponse.ok("公告已刪除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("刪除公告失敗: " + e.getMessage()));
        }
    }
}
