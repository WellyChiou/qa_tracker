package com.example.helloworld.controller;

import com.example.helloworld.entity.Record;
import com.example.helloworld.service.RecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/records")
@CrossOrigin(origins = "*")
public class RecordController {
    
    @Autowired
    private RecordService recordService;
    
    // 取得所有記錄（分頁）
    @GetMapping
    public ResponseEntity<Map<String, Object>> getRecords(
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) Integer category,
        @RequestParam(required = false) String testPlan,
        @RequestParam(required = false) Integer bugFound,
        @RequestParam(required = false) Integer issueNumber,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<Record> records = recordService.searchRecords(
            status, category, testPlan, bugFound, issueNumber, keyword, page, size
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", records.getContent());
        response.put("totalElements", records.getTotalElements());
        response.put("totalPages", records.getTotalPages());
        response.put("currentPage", page);
        response.put("size", size);
        
        return ResponseEntity.ok(response);
    }
    
    // 取得所有記錄（不分頁，用於匯出）
    @GetMapping("/export")
    public ResponseEntity<List<Record>> getRecordsForExport(
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) Integer category,
        @RequestParam(required = false) String testPlan,
        @RequestParam(required = false) Integer bugFound,
        @RequestParam(required = false) Integer issueNumber,
        @RequestParam(required = false) String keyword
    ) {
        List<Record> records = recordService.getAllRecordsForExport(
            status, category, testPlan, bugFound, issueNumber, keyword
        );
        return ResponseEntity.ok(records);
    }
    
    // 根據 ID 取得記錄
    @GetMapping("/{id}")
    public ResponseEntity<Record> getRecordById(@PathVariable Long id) {
        Optional<Record> record = recordService.getRecordById(id);
        return record.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }
    
    // 建立記錄
    @PostMapping
    public ResponseEntity<Record> createRecord(@RequestBody Record record) {
        Record created = recordService.createRecord(record);
        return ResponseEntity.ok(created);
    }
    
    // 更新記錄
    @PutMapping("/{id}")
    public ResponseEntity<Record> updateRecord(@PathVariable Long id, @RequestBody Record record) {
        try {
            Record updated = recordService.updateRecord(id, record);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // 刪除記錄
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteRecord(@PathVariable Long id) {
        recordService.deleteRecord(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Record deleted successfully");
        return ResponseEntity.ok(response);
    }
    
    // 統計執行中筆數
    @GetMapping("/stats/in-progress")
    public ResponseEntity<Map<String, Long>> getInProgressCount() {
        long count = recordService.countInProgress();
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }
}

