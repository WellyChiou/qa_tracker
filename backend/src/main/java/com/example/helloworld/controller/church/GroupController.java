package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.Group;
import com.example.helloworld.entity.church.Person;
import com.example.helloworld.service.church.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/church/groups")
@CrossOrigin(origins = "*")
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    /**
     * 獲取所有小組列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllGroups() {
        try {
            List<Group> groups = groupService.getAllGroups();
            Map<String, Object> response = new HashMap<>();
            response.put("groups", groups);
            response.put("message", "獲取小組列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取小組列表失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 獲取所有啟用的小組列表
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveGroups() {
        try {
            List<Group> groups = groupService.getActiveGroups();
            Map<String, Object> response = new HashMap<>();
            response.put("groups", groups);
            response.put("message", "獲取啟用小組列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取啟用小組列表失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 根據 ID 獲取小組詳情（包含成員列表）
     */
    @GetMapping("/{id}")
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public ResponseEntity<Map<String, Object>> getGroupById(@PathVariable Long id) {
        try {
            return groupService.getGroupById(id)
                .map(group -> {
                    List<Person> members = groupService.getGroupMembers(id);
                    // 在事務內初始化所有懶加載的關聯，避免序列化時出現 LazyInitializationException
                    for (Person member : members) {
                        // 訪問實體屬性，觸發代理初始化
                        member.getId();
                        member.getPersonName();
                        member.getDisplayName();
                        member.getMemberNo();
                        // 初始化懶加載的集合
                        if (member.getGroupPersons() != null) {
                            member.getGroupPersons().size(); // 觸發初始化
                        }
                        if (member.getPositionPersons() != null) {
                            member.getPositionPersons().size(); // 觸發初始化
                        }
                    }
                    Map<String, Object> response = new HashMap<>();
                    response.put("group", group);
                    response.put("members", members);
                    response.put("memberCount", members.size());
                    response.put("message", "獲取小組成功");
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取小組失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 創建小組
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createGroup(@RequestBody Group group) {
        try {
            Group created = groupService.createGroup(group);
            Map<String, Object> response = new HashMap<>();
            response.put("group", created);
            response.put("message", "小組創建成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "創建小組失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 更新小組
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateGroup(@PathVariable Long id, @RequestBody Group group) {
        try {
            Group updated = groupService.updateGroup(id, group);
            Map<String, Object> response = new HashMap<>();
            response.put("group", updated);
            response.put("message", "小組更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "更新小組失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 刪除小組
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteGroup(@PathVariable Long id) {
        try {
            groupService.deleteGroup(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "小組刪除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "刪除小組失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 獲取小組成員列表
     */
    @GetMapping("/{id}/members")
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public ResponseEntity<Map<String, Object>> getGroupMembers(@PathVariable Long id) {
        try {
            List<Person> members = groupService.getGroupMembers(id);
            // 在事務內初始化懶加載的關聯，避免序列化時出現 LazyInitializationException
            for (Person member : members) {
                if (member.getGroupPersons() != null) {
                    member.getGroupPersons().size(); // 觸發初始化
                }
                if (member.getPositionPersons() != null) {
                    member.getPositionPersons().size(); // 觸發初始化
                }
            }
            Map<String, Object> response = new HashMap<>();
            response.put("members", members);
            response.put("message", "獲取小組成員列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取小組成員列表失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 獲取未加入小組的人員列表（編輯時使用，排除當前小組的成員）
     */
    @GetMapping("/{id}/non-members")
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public ResponseEntity<Map<String, Object>> getNonGroupMembers(@PathVariable Long id) {
        try {
            List<Person> nonMembers = groupService.getNonGroupMembers(id);
            // 在事務內初始化懶加載的關聯，避免序列化時出現 LazyInitializationException
            for (Person person : nonMembers) {
                if (person.getGroupPersons() != null) {
                    person.getGroupPersons().size(); // 觸發初始化
                }
                if (person.getPositionPersons() != null) {
                    person.getPositionPersons().size(); // 觸發初始化
                }
            }
            Map<String, Object> response = new HashMap<>();
            response.put("nonMembers", nonMembers);
            response.put("message", "獲取未加入小組的人員列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取未加入小組的人員列表失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 獲取未加入任何小組的人員列表（新增時使用）
     */
    @GetMapping("/non-members")
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public ResponseEntity<Map<String, Object>> getPersonsWithoutGroup() {
        try {
            List<Person> nonMembers = groupService.getPersonsWithoutGroup();
            // 在事務內初始化懶加載的關聯，避免序列化時出現 LazyInitializationException
            for (Person person : nonMembers) {
                if (person.getGroupPersons() != null) {
                    person.getGroupPersons().size(); // 觸發初始化
                }
                if (person.getPositionPersons() != null) {
                    person.getPositionPersons().size(); // 觸發初始化
                }
            }
            Map<String, Object> response = new HashMap<>();
            response.put("nonMembers", nonMembers);
            response.put("message", "獲取未加入任何小組的人員列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取未加入任何小組的人員列表失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 批量添加成員到小組
     */
    @PostMapping("/{id}/members")
    public ResponseEntity<Map<String, Object>> addMembersToGroup(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<?> personIdsRaw = (List<?>) request.get("personIds");
            
            // 安全地將 Integer 或 Long 轉換為 Long
            List<Long> personIds = personIdsRaw.stream()
                    .map(item -> {
                        if (item instanceof Long) {
                            return (Long) item;
                        } else if (item instanceof Integer) {
                            return ((Integer) item).longValue();
                        } else if (item instanceof Number) {
                            return ((Number) item).longValue();
                        } else {
                            return Long.parseLong(item.toString());
                        }
                    })
                    .collect(Collectors.toList());
            
            LocalDate joinedAt = null;
            if (request.containsKey("joinedAt") && request.get("joinedAt") != null) {
                joinedAt = LocalDate.parse(request.get("joinedAt").toString());
            }
            
            groupService.addMembersToGroup(id, personIds, joinedAt);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "批量添加成員成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "批量添加成員失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 從小組移除成員
     */
    @DeleteMapping("/{id}/members/{personId}")
    public ResponseEntity<Map<String, Object>> removeMemberFromGroup(
            @PathVariable Long id,
            @PathVariable Long personId) {
        try {
            groupService.removeMemberFromGroup(id, personId);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "移除成員成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "移除成員失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}

