package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.GroupPerson;
import com.example.helloworld.entity.church.Person;
import com.example.helloworld.repository.church.GroupPersonRepository;
import com.example.helloworld.service.church.PositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/church/persons")
@CrossOrigin(origins = "*")
public class PersonManagementController {

    @Autowired
    private PositionService positionService;

    @Autowired
    private GroupPersonRepository groupPersonRepository;

    /**
     * 獲取所有人員
     */
    @GetMapping
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public ResponseEntity<Map<String, Object>> getAllPersons() {
        try {
            List<Person> persons = positionService.getAllPersons();
            // 在事務內初始化懶加載的關聯，避免序列化時出現 LazyInitializationException
            for (Person person : persons) {
                // 觸發 groupPersons 的初始化（即使有 @JsonIgnore，也要確保不會出錯）
                if (person.getGroupPersons() != null) {
                    person.getGroupPersons().size(); // 觸發初始化
                }
                if (person.getPositionPersons() != null) {
                    person.getPositionPersons().size(); // 觸發初始化
                }
            }
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
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public ResponseEntity<Map<String, Object>> getActivePersons() {
        try {
            List<Person> persons = positionService.getActivePersons();
            // 在事務內初始化懶加載的關聯，避免序列化時出現 LazyInitializationException
            for (Person person : persons) {
                // 觸發 groupPersons 的初始化（即使有 @JsonIgnore，也要確保不會出錯）
                if (person.getGroupPersons() != null) {
                    person.getGroupPersons().size(); // 觸發初始化
                }
                if (person.getPositionPersons() != null) {
                    person.getPositionPersons().size(); // 觸發初始化
                }
            }
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
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public ResponseEntity<Map<String, Object>> getPersonById(@PathVariable Long id) {
        try {
            return positionService.getPersonById(id)
                .map(person -> {
                    // 在事務內初始化懶加載的關聯
                    if (person.getGroupPersons() != null) {
                        person.getGroupPersons().size(); // 觸發初始化
                    }
                    if (person.getPositionPersons() != null) {
                        person.getPositionPersons().size(); // 觸發初始化
                    }
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
    @Transactional(transactionManager = "churchTransactionManager")
    public ResponseEntity<Map<String, Object>> createPerson(@RequestBody Person person) {
        try {
            Person created = positionService.createPerson(person);
            // 在事務內初始化懶加載的關聯，避免序列化時出現 LazyInitializationException
            if (created.getGroupPersons() != null) {
                created.getGroupPersons().size(); // 觸發初始化
            }
            if (created.getPositionPersons() != null) {
                created.getPositionPersons().size(); // 觸發初始化
            }
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
    @Transactional(transactionManager = "churchTransactionManager")
    public ResponseEntity<Map<String, Object>> updatePerson(@PathVariable Long id, @RequestBody Person person) {
        try {
            Person updated = positionService.updatePerson(id, person);
            // 在事務內初始化懶加載的關聯，避免序列化時出現 LazyInitializationException
            if (updated.getGroupPersons() != null) {
                updated.getGroupPersons().size(); // 觸發初始化
            }
            if (updated.getPositionPersons() != null) {
                updated.getPositionPersons().size(); // 觸發初始化
            }
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
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public ResponseEntity<Map<String, Object>> searchPersons(@RequestParam String keyword) {
        try {
            List<Person> persons = positionService.getAllPersons().stream()
                .filter(p -> p.getPersonName().contains(keyword) || 
                           (p.getDisplayName() != null && p.getDisplayName().contains(keyword)))
                .toList();
            // 在事務內初始化懶加載的關聯
            for (Person person : persons) {
                if (person.getGroupPersons() != null) {
                    person.getGroupPersons().size(); // 觸發初始化
                }
                if (person.getPositionPersons() != null) {
                    person.getPositionPersons().size(); // 觸發初始化
                }
            }
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

    /**
     * 獲取人員所屬的小組列表（包含加入時間、是否活躍）
     */
    @GetMapping("/{id}/groups")
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public ResponseEntity<Map<String, Object>> getPersonGroups(@PathVariable Long id) {
        try {
            List<GroupPerson> groupPersons = groupPersonRepository.findAllGroupsByPersonId(id);
            List<Map<String, Object>> groups = new ArrayList<>();
            
            for (GroupPerson gp : groupPersons) {
                Map<String, Object> groupInfo = new HashMap<>();
                groupInfo.put("groupId", gp.getGroup().getId());
                groupInfo.put("groupName", gp.getGroup().getGroupName());
                groupInfo.put("joinedAt", gp.getJoinedAt());
                groupInfo.put("leftAt", gp.getLeftAt());
                groupInfo.put("isActive", gp.getIsActive());
                groups.add(groupInfo);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("groups", groups);
            response.put("message", "獲取人員小組列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取人員小組列表失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 批量設置人員的小組關聯（支援多小組和指定加入時間）
     */
    @PutMapping("/{id}/groups")
    @Transactional(transactionManager = "churchTransactionManager")
    public ResponseEntity<Map<String, Object>> setPersonGroups(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> groupIdsRaw = (List<Object>) request.get("groupIds");
            List<Long> groupIds = new ArrayList<>();
            
            if (groupIdsRaw != null) {
                // 安全地將 Integer 或 Long 轉換為 Long
                for (Object item : groupIdsRaw) {
                    if (item instanceof Long) {
                        groupIds.add((Long) item);
                    } else if (item instanceof Integer) {
                        groupIds.add(((Integer) item).longValue());
                    } else if (item instanceof Number) {
                        groupIds.add(((Number) item).longValue());
                    } else {
                        groupIds.add(Long.parseLong(item.toString()));
                    }
                }
            }
            
            // 解析加入時間（可選）
            LocalDate joinedAt = null;
            if (request.containsKey("joinedAt")) {
                Object joinedAtObj = request.get("joinedAt");
                if (joinedAtObj != null) {
                    if (joinedAtObj instanceof String) {
                        joinedAt = LocalDate.parse((String) joinedAtObj);
                    }
                }
            }
            
            positionService.setPersonGroups(id, groupIds, joinedAt);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "設置人員小組關聯成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "設置人員小組關聯失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}

