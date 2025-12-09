package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.AboutInfo;
import com.example.helloworld.entity.church.Activity;
import com.example.helloworld.service.church.AboutInfoService;
import com.example.helloworld.service.church.ActivityService;
import com.example.helloworld.service.church.ChurchInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/church/public")
@CrossOrigin(origins = "*")
public class FrontendDataController {

    @Autowired
    private ChurchInfoService churchInfoService;

    @Autowired
    private AboutInfoService aboutInfoService;

    @Autowired
    private ActivityService activityService;

    /**
     * 獲取教會基本資訊（公開訪問）
     */
    @GetMapping("/church-info")
    public ResponseEntity<Map<String, Object>> getChurchInfo() {
        try {
            Map<String, String> info = churchInfoService.getAllActiveInfo();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", info);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "獲取教會資訊失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 獲取關於我們資訊（公開訪問）
     */
    @GetMapping("/about-info")
    public ResponseEntity<Map<String, Object>> getAboutInfo() {
        try {
            List<AboutInfo> info = aboutInfoService.getAllActiveInfo();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", info);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "獲取關於我們資訊失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 獲取活動資訊（公開訪問）
     */
    @GetMapping("/activities")
    public ResponseEntity<Map<String, Object>> getActivities() {
        try {
            List<Activity> activities = activityService.getAllActiveActivities();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", activities);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "獲取活動資訊失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

