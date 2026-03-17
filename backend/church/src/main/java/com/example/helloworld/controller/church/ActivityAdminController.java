package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.entity.church.Activity;
import com.example.helloworld.service.church.ActivityService;
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
@RequestMapping("/api/church/admin/activities")
@CrossOrigin(origins = "*")
public class ActivityAdminController {

    @Autowired
    private ActivityService activityService;

    /**
     * 獲取所有活動（管理用，包含未啟用的，支援分頁）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Activity>>> getAllActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("activityDate").descending());
            Page<Activity> activitiesPage = activityService.getAllActivities(pageable);
            PageResponse<Activity> pageResponse = new PageResponse<>(
                    activitiesPage.getContent(),
                    activitiesPage.getTotalElements(),
                    activitiesPage.getTotalPages(),
                    activitiesPage.getNumber(),
                    activitiesPage.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(pageResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取活動資訊失敗: " + e.getMessage()));
        }
    }

    /**
     * 根據 ID 獲取活動
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Activity>> getActivityById(@PathVariable Long id) {
        try {
            Activity activity = activityService.getActivityById(id)
                .orElseThrow(() -> new RuntimeException("活動不存在: " + id));
            return ResponseEntity.ok(ApiResponse.ok(activity));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage()));
        }
    }

    /**
     * 創建活動
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Activity>> createActivity(@RequestBody Activity activity) {
        try {
            Activity created = activityService.createActivity(activity);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("建立活動失敗: " + e.getMessage()));
        }
    }

    /**
     * 更新活動
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Activity>> updateActivity(@PathVariable Long id, @RequestBody Activity activity) {
        try {
            Activity updated = activityService.updateActivity(id, activity);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage()));
        }
    }

    /**
     * 刪除活動
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteActivity(@PathVariable Long id) {
        try {
            activityService.deleteActivity(id);
            return ResponseEntity.ok(ApiResponse.ok("活動已刪除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("刪除活動失敗: " + e.getMessage()));
        }
    }
}
