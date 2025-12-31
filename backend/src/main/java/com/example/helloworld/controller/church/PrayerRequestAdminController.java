package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.PrayerRequest;
import com.example.helloworld.service.church.PrayerRequestService;
import org.springframework.beans.factory.annotation.Autowired;
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
     * 獲取所有代禱事項（管理用，包含未啟用的）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPrayerRequests() {
        try {
            List<PrayerRequest> allPrayerRequests = prayerRequestService.getAllPrayerRequests();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", allPrayerRequests);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "獲取代禱事項失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 根據 ID 獲取代禱事項
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPrayerRequestById(@PathVariable Long id) {
        try {
            PrayerRequest prayerRequest = prayerRequestService.getPrayerRequestById(id)
                .orElseThrow(() -> new RuntimeException("代禱事項不存在: " + id));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", prayerRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 創建代禱事項
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createPrayerRequest(@RequestBody PrayerRequest prayerRequest) {
        try {
            PrayerRequest created = prayerRequestService.createPrayerRequest(prayerRequest);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", created);
            response.put("message", "代禱事項已建立");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "建立代禱事項失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新代禱事項
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updatePrayerRequest(@PathVariable Long id, @RequestBody PrayerRequest prayerRequest) {
        try {
            PrayerRequest updated = prayerRequestService.updatePrayerRequest(id, prayerRequest);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "代禱事項已更新");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 刪除代禱事項
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePrayerRequest(@PathVariable Long id) {
        try {
            prayerRequestService.deletePrayerRequest(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "代禱事項已刪除");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "刪除代禱事項失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

