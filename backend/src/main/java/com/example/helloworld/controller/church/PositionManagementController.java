package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.entity.church.Position;
import com.example.helloworld.entity.church.Person;
import com.example.helloworld.service.church.PositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
     * 獲取所有崗位（支持分頁和過濾）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Position>>> getAllPositions(
            @RequestParam(required = false) String positionCode,
            @RequestParam(required = false) String positionName,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<Position> positionsPage = positionService.getAllPositions(positionCode, positionName, isActive, page, size);
            PageResponse<Position> pageResponse = new PageResponse<>(
                positionsPage.getContent(),
                positionsPage.getTotalElements(),
                positionsPage.getTotalPages(),
                positionsPage.getNumber(),
                positionsPage.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(pageResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取崗位列表失敗：" + e.getMessage()));
        }
    }

    /**
     * 獲取所有啟用的崗位
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Position>>> getActivePositions() {
        try {
            List<Position> positions = positionService.getActivePositions();
            return ResponseEntity.ok(ApiResponse.ok(positions));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取崗位列表失敗：" + e.getMessage()));
        }
    }

    /**
     * 根據 ID 獲取崗位
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Position>> getPositionById(@PathVariable Long id) {
        try {
            return positionService.getPositionById(id)
                .map(position -> ResponseEntity.ok(ApiResponse.ok(position)))
                .orElse(ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(ApiResponse.fail("找不到指定的崗位")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取崗位失敗：" + e.getMessage()));
        }
    }

    /**
     * 創建崗位
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Position>> createPosition(@RequestBody Position position) {
        try {
            Position created = positionService.createPosition(position);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.fail("創建崗位失敗：" + e.getMessage()));
        }
    }

    /**
     * 更新崗位
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Position>> updatePosition(@PathVariable Long id, @RequestBody Position position) {
        try {
            Position updated = positionService.updatePosition(id, position);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新崗位失敗：" + e.getMessage()));
        }
    }

    /**
     * 刪除崗位
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePosition(@PathVariable Long id) {
        try {
            positionService.deletePosition(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除崗位失敗：" + e.getMessage()));
        }
    }

    /**
     * 獲取崗位的人員列表（按日期類型分組）
     */
    @GetMapping("/{id}/persons")
    public ResponseEntity<ApiResponse<Map<String, List<Map<String, Object>>>>> getPositionPersons(@PathVariable Long id) {
        try {
            Map<String, List<Map<String, Object>>> persons = positionService.getPositionPersonsByDayType(id);
            return ResponseEntity.ok(ApiResponse.ok(persons));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取崗位人員列表失敗：" + e.getMessage()));
        }
    }

    /**
     * 為崗位添加人員
     */
    @PostMapping("/{positionId}/persons/{personId}")
    public ResponseEntity<ApiResponse<Void>> addPersonToPosition(
            @PathVariable Long positionId,
            @PathVariable Long personId,
            @RequestParam String dayType) {
        try {
            positionService.addPersonToPosition(positionId, personId, dayType);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("添加人員失敗：" + e.getMessage()));
        }
    }

    /**
     * 從崗位移除人員
     */
    @DeleteMapping("/{positionId}/persons/{personId}")
    public ResponseEntity<ApiResponse<Void>> removePersonFromPosition(
            @PathVariable Long positionId,
            @PathVariable Long personId,
            @RequestParam String dayType) {
        try {
            positionService.removePersonFromPosition(positionId, personId, dayType);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            e.printStackTrace(); // 打印錯誤堆棧
            return ResponseEntity.badRequest().body(ApiResponse.fail("移除人員失敗：" + e.getMessage()));
        }
    }

    /**
     * 更新崗位人員的 includeInAutoSchedule 狀態
     */
    @PutMapping("/position-persons/{positionPersonId}/include-in-auto-schedule")
    public ResponseEntity<ApiResponse<Void>> updatePositionPersonIncludeInAutoSchedule(
            @PathVariable Long positionPersonId,
            @RequestBody Map<String, Object> request) {
        try {
            Boolean includeInAutoSchedule = request.get("includeInAutoSchedule") != null 
                ? Boolean.valueOf(request.get("includeInAutoSchedule").toString()) 
                : true;
            
            positionService.updatePositionPersonIncludeInAutoSchedule(positionPersonId, includeInAutoSchedule);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新失敗：" + e.getMessage()));
        }
    }

    /**
     * 更新崗位人員排序
     */
    @PutMapping("/{id}/persons/order")
    public ResponseEntity<ApiResponse<Void>> updatePositionPersonOrder(
            @PathVariable Long id,
            @RequestParam String dayType,
            @RequestBody List<Long> personIds) {
        try {
            positionService.updatePositionPersonOrder(id, dayType, personIds);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新排序失敗：" + e.getMessage()));
        }
    }

    /**
     * 獲取完整的崗位配置
     */
    @GetMapping("/config/full")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFullPositionConfig() {
        try {
            Map<String, Object> config = positionService.getFullPositionConfig();
            return ResponseEntity.ok(ApiResponse.ok(config));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取配置失敗：" + e.getMessage()));
        }
    }
}

