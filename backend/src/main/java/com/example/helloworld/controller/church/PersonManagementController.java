package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.Person;
import com.example.helloworld.service.church.PositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/church/persons")
@CrossOrigin(origins = "*")
public class PersonManagementController {

    @Autowired
    private PositionService positionService;

    /**
     * 獲取所有人員
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPersons() {
        try {
            List<Person> persons = positionService.getAllPersons();
            Map<String, Object> response = new HashMap<>();
            response.put("persons", persons);
            response.put("message", "獲取人員列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取人員列表失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 獲取所有啟用的人員
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActivePersons() {
        try {
            List<Person> persons = positionService.getActivePersons();
            Map<String, Object> response = new HashMap<>();
            response.put("persons", persons);
            response.put("message", "獲取啟用人員列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取人員列表失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 根據 ID 獲取人員
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPersonById(@PathVariable Long id) {
        try {
            return positionService.getPersonById(id)
                .map(person -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("person", person);
                    response.put("message", "獲取人員成功");
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取人員失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 創建人員
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createPerson(@RequestBody Person person) {
        try {
            Person created = positionService.createPerson(person);
            Map<String, Object> response = new HashMap<>();
            response.put("person", created);
            response.put("message", "人員創建成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "創建人員失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 更新人員
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updatePerson(@PathVariable Long id, @RequestBody Person person) {
        try {
            Person updated = positionService.updatePerson(id, person);
            Map<String, Object> response = new HashMap<>();
            response.put("person", updated);
            response.put("message", "人員更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "更新人員失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 刪除人員
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePerson(@PathVariable Long id) {
        try {
            positionService.deletePerson(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "人員刪除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "刪除人員失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 搜尋人員
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchPersons(@RequestParam String keyword) {
        try {
            List<Person> persons = positionService.getAllPersons().stream()
                .filter(p -> p.getPersonName().contains(keyword) || 
                           (p.getDisplayName() != null && p.getDisplayName().contains(keyword)))
                .toList();
            Map<String, Object> response = new HashMap<>();
            response.put("persons", persons);
            response.put("message", "搜尋成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "搜尋失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}

