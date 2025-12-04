package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.Position;
import com.example.helloworld.entity.church.Person;
import com.example.helloworld.service.church.PositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/church/positions")
@CrossOrigin(origins = "*")
public class PositionManagementController {
    
    // 確保所有響應使用 UTF-8 編碼
    private static final String UTF8_CHARSET = "UTF-8";

    @Autowired
    private PositionService positionService;

    // ========== 崗位相關 API ==========

    /**
     * 獲取所有崗位
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPositions() {
        try {
            List<Position> positions = positionService.getAllPositions();
            Map<String, Object> response = new HashMap<>();
            response.put("positions", positions);
            response.put("message", "獲取崗位列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取崗位列表失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 獲取所有啟用的崗位
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActivePositions() {
        try {
            List<Position> positions = positionService.getActivePositions();
            Map<String, Object> response = new HashMap<>();
            response.put("positions", positions);
            response.put("message", "獲取啟用崗位列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取崗位列表失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 根據 ID 獲取崗位
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPositionById(@PathVariable Long id) {
        try {
            return positionService.getPositionById(id)
                .map(position -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("position", position);
                    response.put("message", "獲取崗位成功");
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取崗位失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 創建崗位
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createPosition(@RequestBody Position position) {
        try {
            Position created = positionService.createPosition(position);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("position", created);
            response.put("message", "崗位創建成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "創建崗位失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 更新崗位
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updatePosition(@PathVariable Long id, @RequestBody Position position) {
        try {
            Position updated = positionService.updatePosition(id, position);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("position", updated);
            response.put("message", "崗位更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "更新崗位失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 刪除崗位
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePosition(@PathVariable Long id) {
        try {
            positionService.deletePosition(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "崗位刪除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "刪除崗位失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 獲取崗位的人員列表（按日期類型分組）
     */
    @GetMapping("/{id}/persons")
    public ResponseEntity<Map<String, Object>> getPositionPersons(@PathVariable Long id) {
        try {
            Map<String, List<Map<String, Object>>> persons = positionService.getPositionPersonsByDayType(id);
            Map<String, Object> response = new HashMap<>();
            response.put("persons", persons);
            response.put("message", "獲取崗位人員列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取崗位人員列表失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 為崗位添加人員
     */
    @PostMapping("/{positionId}/persons/{personId}")
    public ResponseEntity<Map<String, Object>> addPersonToPosition(
            @PathVariable Long positionId,
            @PathVariable Long personId,
            @RequestParam String dayType) {
        try {
            positionService.addPersonToPosition(positionId, personId, dayType);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "人員添加成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "添加人員失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 從崗位移除人員
     */
    @DeleteMapping("/{positionId}/persons/{personId}")
    public ResponseEntity<Map<String, Object>> removePersonFromPosition(
            @PathVariable Long positionId,
            @PathVariable Long personId,
            @RequestParam String dayType) {
        try {
            positionService.removePersonFromPosition(positionId, personId, dayType);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "人員移除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace(); // 打印錯誤堆棧
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "移除人員失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 更新崗位人員的 includeInAutoSchedule 狀態
     */
    @PutMapping("/position-persons/{positionPersonId}/include-in-auto-schedule")
    public ResponseEntity<Map<String, Object>> updatePositionPersonIncludeInAutoSchedule(
            @PathVariable Long positionPersonId,
            @RequestBody Map<String, Object> request) {
        try {
            Boolean includeInAutoSchedule = request.get("includeInAutoSchedule") != null 
                ? Boolean.valueOf(request.get("includeInAutoSchedule").toString()) 
                : true;
            
            positionService.updatePositionPersonIncludeInAutoSchedule(positionPersonId, includeInAutoSchedule);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "更新失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 更新崗位人員排序
     */
    @PutMapping("/{id}/persons/order")
    public ResponseEntity<Map<String, Object>> updatePositionPersonOrder(
            @PathVariable Long id,
            @RequestParam String dayType,
            @RequestBody List<Long> personIds) {
        try {
            positionService.updatePositionPersonOrder(id, dayType, personIds);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "排序更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "更新排序失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 獲取完整的崗位配置
     */
    @GetMapping("/config/full")
    public ResponseEntity<Map<String, Object>> getFullPositionConfig() {
        try {
            Map<String, Object> config = positionService.getFullPositionConfig();
            Map<String, Object> response = new HashMap<>();
            response.put("config", config);
            response.put("message", "獲取完整配置成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取配置失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}

