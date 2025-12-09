package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.AboutInfo;
import com.example.helloworld.service.church.AboutInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/church/admin/about-info")
@CrossOrigin(origins = "*")
public class AboutInfoAdminController {

    @Autowired
    private AboutInfoService aboutInfoService;

    /**
     * 獲取所有關於我們資訊（管理用，包含未啟用的）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllAboutInfo() {
        try {
            // 獲取所有資訊（包含未啟用的）
            List<AboutInfo> allInfo = aboutInfoService.getAllInfo();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", allInfo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "獲取關於我們資訊失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 根據 ID 獲取關於我們資訊
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAboutInfoById(@PathVariable Long id) {
        try {
            AboutInfo info = aboutInfoService.getInfoById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", info);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 創建關於我們資訊
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAboutInfo(@RequestBody AboutInfo aboutInfo) {
        try {
            AboutInfo created = aboutInfoService.createInfo(aboutInfo);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", created);
            response.put("message", "關於我們資訊已建立");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "建立關於我們資訊失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新關於我們資訊
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateAboutInfo(@PathVariable Long id, @RequestBody AboutInfo aboutInfo) {
        try {
            AboutInfo updated = aboutInfoService.updateInfo(id, aboutInfo);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "關於我們資訊已更新");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 刪除關於我們資訊
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteAboutInfo(@PathVariable Long id) {
        try {
            aboutInfoService.deleteInfo(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "關於我們資訊已刪除");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "刪除關於我們資訊失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
