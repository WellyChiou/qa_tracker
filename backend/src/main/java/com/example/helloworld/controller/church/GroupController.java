package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.entity.church.Group;
import com.example.helloworld.entity.church.GroupPerson;
import com.example.helloworld.entity.church.Person;
import com.example.helloworld.service.church.GroupService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/church/groups")
@CrossOrigin(origins = "*")
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    /**
     * 獲取所有小組列表（支援分頁）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Group>>> getAllGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Group> groupsPage = groupService.getAllGroups(pageable);
            PageResponse<Group> pageResponse = new PageResponse<>(
                    groupsPage.getContent(),
                    groupsPage.getTotalElements(),
                    groupsPage.getTotalPages(),
                    groupsPage.getNumber(),
                    groupsPage.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(pageResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取小組列表失敗：" + e.getMessage()));
        }
    }

    /**
     * 獲取所有啟用的小組列表
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Group>>> getActiveGroups() {
        try {
            List<Group> groups = groupService.getActiveGroups();
            return ResponseEntity.ok(ApiResponse.ok(groups));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取啟用小組列表失敗：" + e.getMessage()));
        }
    }

    /**
     * 根據 ID 獲取小組詳情（包含成員列表）
     */
    @GetMapping("/{id}")
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGroupById(@PathVariable Long id) {
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
                    Map<String, Object> result = new HashMap<>();
                    result.put("group", group);
                    result.put("members", members);
                    result.put("memberCount", members.size());
                    return ResponseEntity.ok(ApiResponse.ok(result));
                })
                .orElse(ResponseEntity.badRequest()
                        .body(ApiResponse.fail("小組不存在")));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取小組失敗：" + e.getMessage()));
        }
    }

    /**
     * 創建小組
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Group>> createGroup(@RequestBody Group group) {
        try {
            Group created = groupService.createGroup(group);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("創建小組失敗：" + e.getMessage()));
        }
    }

    /**
     * 更新小組
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Group>> updateGroup(@PathVariable Long id, @RequestBody Group group) {
        try {
            Group updated = groupService.updateGroup(id, group);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("更新小組失敗：" + e.getMessage()));
        }
    }

    /**
     * 刪除小組
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteGroup(@PathVariable Long id) {
        try {
            groupService.deleteGroup(id);
            return ResponseEntity.ok(ApiResponse.ok("小組刪除成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("刪除小組失敗：" + e.getMessage()));
        }
    }

    /**
     * 獲取小組成員列表（包含角色）
     */
    @GetMapping("/{id}/members")
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getGroupMembers(@PathVariable Long id) {
        try {
            List<GroupPerson> groupPersons = groupService.getGroupMembersWithRole(id);
            // 在事務內初始化懶加載的關聯，避免序列化時出現 LazyInitializationException
            List<Map<String, Object>> members = new ArrayList<>();
            for (GroupPerson gp : groupPersons) {
                Person person = gp.getPerson();
                if (person != null) {
                    // 觸發初始化
                    person.getId();
                    person.getPersonName();
                    if (person.getGroupPersons() != null) {
                        person.getGroupPersons().size();
                    }
                    if (person.getPositionPersons() != null) {
                        person.getPositionPersons().size();
                    }
                    
                    Map<String, Object> memberData = new HashMap<>();
                    memberData.put("id", person.getId());
                    memberData.put("personName", person.getPersonName());
                    memberData.put("displayName", person.getDisplayName());
                    memberData.put("memberNo", person.getMemberNo());
                    memberData.put("role", gp.getRole() != null ? gp.getRole() : "MEMBER");
                    members.add(memberData);
                }
            }
            return ResponseEntity.ok(ApiResponse.ok(members));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取小組成員列表失敗：" + e.getMessage()));
        }
    }

    /**
     * 獲取未加入小組的人員列表（編輯時使用，排除當前小組的成員）
     */
    @GetMapping("/{id}/non-members")
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public ResponseEntity<ApiResponse<List<Person>>> getNonGroupMembers(@PathVariable Long id) {
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
            return ResponseEntity.ok(ApiResponse.ok(nonMembers));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取未加入小組的人員列表失敗：" + e.getMessage()));
        }
    }

    /**
     * 獲取未加入任何小組的人員列表（新增時使用）
     */
    @GetMapping("/non-members")
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public ResponseEntity<ApiResponse<List<Person>>> getPersonsWithoutGroup() {
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
            return ResponseEntity.ok(ApiResponse.ok(nonMembers));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取未加入任何小組的人員列表失敗：" + e.getMessage()));
        }
    }

    /**
     * 批量添加成員到小組（支援角色設定）
     */
    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<String>> addMembersToGroup(
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
            
            // 處理角色設定（可選）
            @SuppressWarnings("unchecked")
            Map<String, String> personRolesMap = (Map<String, String>) request.get("personRoles");
            Map<Long, String> personRoles = null;
            if (personRolesMap != null) {
                personRoles = new HashMap<>();
                for (Map.Entry<String, String> entry : personRolesMap.entrySet()) {
                    Long personId = Long.parseLong(entry.getKey());
                    personRoles.put(personId, entry.getValue());
                }
            }
            
            groupService.addMembersToGroupWithRoles(id, personIds, personRoles, joinedAt);
            return ResponseEntity.ok(ApiResponse.ok("批量添加成員成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("批量添加成員失敗：" + e.getMessage()));
        }
    }

    /**
     * 更新成員角色
     */
    @PutMapping("/{id}/members/{personId}/role")
    public ResponseEntity<ApiResponse<String>> updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long personId,
            @RequestBody Map<String, String> request) {
        try {
            String role = request.get("role");
            if (role == null || (!role.equals("MEMBER") && !role.equals("LEADER") && !role.equals("ASSISTANT_LEADER"))) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("無效的角色：" + role));
            }
            
            groupService.updateMemberRole(id, personId, role);
            return ResponseEntity.ok(ApiResponse.ok("更新成員角色成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("更新成員角色失敗：" + e.getMessage()));
        }
    }

    /**
     * 從小組移除成員
     */
    @DeleteMapping("/{id}/members/{personId}")
    public ResponseEntity<ApiResponse<String>> removeMemberFromGroup(
            @PathVariable Long id,
            @PathVariable Long personId) {
        try {
            groupService.removeMemberFromGroup(id, personId);
            return ResponseEntity.ok(ApiResponse.ok("移除成員成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("移除成員失敗：" + e.getMessage()));
        }
    }
}

