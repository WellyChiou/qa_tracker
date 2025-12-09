package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.ChurchInfo;
import com.example.helloworld.service.church.ChurchInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/church/admin/church-info")
@CrossOrigin(origins = "*")
public class ChurchInfoAdminController {

    @Autowired
    private ChurchInfoService churchInfoService;

    /**
     * 獲取所有教會資訊（管理用，包含未啟用的）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllChurchInfo() {
        try {
            // 獲取所有資訊（包含未啟用的）
            List<ChurchInfo> allInfo = churchInfoService.getAllInfo();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", allInfo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "獲取教會資訊失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 根據 key 獲取教會資訊
     */
    @GetMapping("/{infoKey}")
    public ResponseEntity<Map<String, Object>> getChurchInfoByKey(@PathVariable String infoKey) {
        try {
            Optional<ChurchInfo> info = churchInfoService.getInfoByKey(infoKey);
            Map<String, Object> response = new HashMap<>();
            if (info.isPresent()) {
                response.put("success", true);
                response.put("data", info.get());
            } else {
                response.put("success", false);
                response.put("message", "找不到指定的教會資訊");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "獲取教會資訊失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 創建或更新教會資訊
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> saveOrUpdateChurchInfo(@RequestBody ChurchInfo churchInfo) {
        try {
            ChurchInfo saved = churchInfoService.saveOrUpdateInfo(churchInfo);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", saved);
            response.put("message", "教會資訊已儲存");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "儲存教會資訊失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 批量更新教會資訊
     */
    @PutMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchUpdateChurchInfo(@RequestBody List<ChurchInfo> churchInfoList) {
        try {
            for (ChurchInfo info : churchInfoList) {
                churchInfoService.saveOrUpdateInfo(info);
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "批量更新失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
