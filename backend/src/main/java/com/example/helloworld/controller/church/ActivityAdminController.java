package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.Activity;
import com.example.helloworld.service.church.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
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
     * 獲取所有活動（管理用，包含未啟用的）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllActivities() {
        try {
            List<Activity> allActivities = activityService.getAllActivities();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", allActivities);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "獲取活動資訊失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 根據 ID 獲取活動
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getActivityById(@PathVariable Long id) {
        try {
            Activity activity = activityService.getActivityById(id)
                .orElseThrow(() -> new RuntimeException("活動不存在: " + id));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", activity);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 創建活動
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createActivity(@RequestBody Activity activity) {
        try {
            Activity created = activityService.createActivity(activity);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", created);
            response.put("message", "活動已建立");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "建立活動失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新活動
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateActivity(@PathVariable Long id, @RequestBody Activity activity) {
        try {
            Activity updated = activityService.updateActivity(id, activity);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "活動已更新");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 刪除活動
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteActivity(@PathVariable Long id) {
        try {
            activityService.deleteActivity(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "活動已刪除");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "刪除活動失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
