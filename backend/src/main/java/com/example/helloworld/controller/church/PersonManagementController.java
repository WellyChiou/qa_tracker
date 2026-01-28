package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.entity.church.GroupPerson;
import com.example.helloworld.entity.church.Person;
import com.example.helloworld.repository.church.GroupPersonRepository;
import com.example.helloworld.service.church.PositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
     * 獲取所有人員（支援分頁）
     */
    @GetMapping
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public ResponseEntity<ApiResponse<PageResponse<Person>>> getAllPersons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("personName").ascending());
            Page<Person> personsPage = positionService.getAllPersons(pageable);
            List<Person> persons = personsPage.getContent();
            
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
            
            PageResponse<Person> pageResponse = new PageResponse<>(
                persons,
                personsPage.getTotalElements(),
                personsPage.getTotalPages(),
                personsPage.getNumber(),
                personsPage.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(pageResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取人員列表失敗：" + e.getMessage()));
        }
    }

    /**
     * 獲取所有啟用的人員
     */
    @GetMapping("/active")
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public ResponseEntity<ApiResponse<List<Person>>> getActivePersons() {
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
            return ResponseEntity.ok(ApiResponse.ok(persons));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取人員列表失敗：" + e.getMessage()));
        }
    }

    /**
     * 根據 ID 獲取人員
     */
    @GetMapping("/{id}")
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public ResponseEntity<ApiResponse<Person>> getPersonById(@PathVariable Long id) {
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
                    return ResponseEntity.ok(ApiResponse.ok(person));
                })
                .orElse(ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(ApiResponse.fail("找不到指定的人員")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取人員失敗：" + e.getMessage()));
        }
    }

    /**
     * 創建人員
     */
    @PostMapping
    @Transactional(transactionManager = "churchTransactionManager")
    public ResponseEntity<ApiResponse<Person>> createPerson(@RequestBody Person person) {
        try {
            Person created = positionService.createPerson(person);
            // 在事務內初始化懶加載的關聯，避免序列化時出現 LazyInitializationException
            if (created.getGroupPersons() != null) {
                created.getGroupPersons().size(); // 觸發初始化
            }
            if (created.getPositionPersons() != null) {
                created.getPositionPersons().size(); // 觸發初始化
            }
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("創建人員失敗：" + e.getMessage()));
        }
    }

    /**
     * 更新人員
     */
    @PutMapping("/{id}")
    @Transactional(transactionManager = "churchTransactionManager")
    public ResponseEntity<ApiResponse<Person>> updatePerson(@PathVariable Long id, @RequestBody Person person) {
        try {
            Person updated = positionService.updatePerson(id, person);
            // 在事務內初始化懶加載的關聯，避免序列化時出現 LazyInitializationException
            if (updated.getGroupPersons() != null) {
                updated.getGroupPersons().size(); // 觸發初始化
            }
            if (updated.getPositionPersons() != null) {
                updated.getPositionPersons().size(); // 觸發初始化
            }
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新人員失敗：" + e.getMessage()));
        }
    }

    /**
     * 刪除人員
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePerson(@PathVariable Long id) {
        try {
            positionService.deletePerson(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除人員失敗：" + e.getMessage()));
        }
    }

    /**
     * 搜尋人員
     */
    @GetMapping("/search")
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public ResponseEntity<ApiResponse<List<Person>>> searchPersons(@RequestParam String keyword) {
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
            return ResponseEntity.ok(ApiResponse.ok(persons));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("搜尋失敗：" + e.getMessage()));
        }
    }

    /**
     * 獲取人員所屬的小組列表（包含加入時間、是否活躍）
     */
    @GetMapping("/{id}/groups")
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPersonGroups(@PathVariable Long id) {
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
            
            return ResponseEntity.ok(ApiResponse.ok(groups));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取人員小組列表失敗：" + e.getMessage()));
        }
    }

    /**
     * 批量獲取多個人員的小組列表
     */
    @PostMapping("/groups/batch")
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public ResponseEntity<ApiResponse<Map<Long, List<Map<String, Object>>>>> getPersonsGroupsBatch(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> personIdsRaw = (List<Object>) request.get("personIds");
            List<Long> personIds = new ArrayList<>();
            
            if (personIdsRaw != null && !personIdsRaw.isEmpty()) {
                for (Object item : personIdsRaw) {
                    if (item instanceof Long) {
                        personIds.add((Long) item);
                    } else if (item instanceof Integer) {
                        personIds.add(((Integer) item).longValue());
                    } else if (item instanceof Number) {
                        personIds.add(((Number) item).longValue());
                    }
                }
            }
            
            if (personIds.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.ok(new HashMap<>()));
            }
            
            // 批量查詢所有相關的 GroupPerson 記錄
            List<GroupPerson> allGroupPersons = groupPersonRepository.findAllGroupsByPersonIds(personIds);
            
            // 按人員 ID 分組
            Map<Long, List<Map<String, Object>>> personGroupsMap = new HashMap<>();
            
            for (GroupPerson gp : allGroupPersons) {
                Long personId = gp.getPersonId() != null ? gp.getPersonId() : gp.getPerson().getId();
                
                Map<String, Object> groupInfo = new HashMap<>();
                groupInfo.put("groupId", gp.getGroup().getId());
                groupInfo.put("groupName", gp.getGroup().getGroupName());
                groupInfo.put("joinedAt", gp.getJoinedAt());
                groupInfo.put("leftAt", gp.getLeftAt());
                groupInfo.put("isActive", gp.getIsActive());
                
                personGroupsMap.computeIfAbsent(personId, k -> new ArrayList<>()).add(groupInfo);
            }
            
            // 確保所有人員 ID 都有對應的列表（即使是空的）
            for (Long personId : personIds) {
                personGroupsMap.putIfAbsent(personId, new ArrayList<>());
            }
            
            return ResponseEntity.ok(ApiResponse.ok(personGroupsMap));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("批量獲取人員小組列表失敗：" + e.getMessage()));
        }
    }

    /**
     * 批量設置人員的小組關聯（支援多小組和指定加入時間）
     */
    @PutMapping("/{id}/groups")
    @Transactional(transactionManager = "churchTransactionManager")
    public ResponseEntity<ApiResponse<Void>> setPersonGroups(
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
            
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("設置人員小組關聯失敗：" + e.getMessage()));
        }
    }
}

