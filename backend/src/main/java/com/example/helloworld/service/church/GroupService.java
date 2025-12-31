package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.Group;
import com.example.helloworld.entity.church.GroupPerson;
import com.example.helloworld.entity.church.Person;
import com.example.helloworld.repository.church.GroupPersonRepository;
import com.example.helloworld.repository.church.GroupRepository;
import com.example.helloworld.repository.church.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GroupService {
    private final GroupRepository groupRepository;
    private final GroupPersonRepository groupPersonRepository;
    private final PersonRepository personRepository;

    public GroupService(GroupRepository groupRepository, GroupPersonRepository groupPersonRepository, PersonRepository personRepository) {
        this.groupRepository = groupRepository;
        this.groupPersonRepository = groupPersonRepository;
        this.personRepository = personRepository;
    }

    // CRUD 操作
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public List<Group> getAllGroups() {
        List<Group> groups = groupRepository.findAll();
        // 批量查詢所有小組的成員數量
        setMemberCountsForGroups(groups);
        return groups;
    }

    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public List<Group> getActiveGroups() {
        List<Group> groups = groupRepository.findByIsActiveTrue();
        // 批量查詢所有小組的成員數量
        setMemberCountsForGroups(groups);
        return groups;
    }

    // 批量設置小組的成員數量（優化查詢）
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    private void setMemberCountsForGroups(List<Group> groups) {
        if (groups == null || groups.isEmpty()) {
            return;
        }

        // 獲取所有小組 ID
        List<Long> groupIds = groups.stream().map(Group::getId).toList();

        // 批量查詢所有小組的活躍成員數量
        List<GroupPerson> allGroupPersons = groupPersonRepository.findByGroupIdInAndIsActiveTrue(groupIds);

        // 按小組 ID 分組統計
        Map<Long, Long> memberCountMap = allGroupPersons.stream()
                .collect(Collectors.groupingBy(
                        GroupPerson::getGroupId,
                        Collectors.counting()
                ));

        // 設置每個小組的成員數量
        for (Group group : groups) {
            Long count = memberCountMap.getOrDefault(group.getId(), 0L);
            group.setMemberCount(count.intValue());
        }
    }

    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public Optional<Group> getGroupById(Long id) {
        return groupRepository.findById(id);
    }

    @Transactional(transactionManager = "churchTransactionManager")
    public Group createGroup(Group group) {
        // 檢查小組名稱是否已存在
        if (groupRepository.findByGroupName(group.getGroupName()).isPresent()) {
            throw new RuntimeException("小組名稱已存在：" + group.getGroupName());
        }
        return groupRepository.save(group);
    }

    @Transactional(transactionManager = "churchTransactionManager")
    public Group updateGroup(Long id, Group group) {
        Group existingGroup = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("小組不存在：" + id));
        
        // 檢查小組名稱是否已被其他小組使用
        Optional<Group> groupWithSameName = groupRepository.findByGroupName(group.getGroupName());
        if (groupWithSameName.isPresent() && !groupWithSameName.get().getId().equals(id)) {
            throw new RuntimeException("小組名稱已存在：" + group.getGroupName());
        }
        
        existingGroup.setGroupName(group.getGroupName());
        existingGroup.setDescription(group.getDescription());
        existingGroup.setMeetingFrequency(group.getMeetingFrequency());
        existingGroup.setCategory(group.getCategory());
        existingGroup.setMeetingLocation(group.getMeetingLocation());
        existingGroup.setIsActive(group.getIsActive());
        return groupRepository.save(existingGroup);
    }

    @Transactional(transactionManager = "churchTransactionManager")
    public void deleteGroup(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("小組不存在：" + id));
        
        // 標記所有關聯的人員關係為非活躍（保留歷史記錄）
        List<GroupPerson> groupPersons = groupPersonRepository.findByGroupId(id);
        for (GroupPerson gp : groupPersons) {
            gp.setIsActive(false);
            gp.setLeftAt(LocalDate.now());
            groupPersonRepository.save(gp);
        }
        
        // 刪除小組
        groupRepository.delete(group);
    }

    // 獲取小組成員列表
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public List<Person> getGroupMembers(Long groupId) {
        List<GroupPerson> groupPersons = groupPersonRepository.findActiveMembersByGroupId(groupId);
        List<Person> members = new ArrayList<>();
        for (GroupPerson gp : groupPersons) {
            Person person = gp.getPerson();
            // 觸發懶加載，確保在事務內初始化
            if (person != null) {
                // 訪問實體的基本屬性，觸發代理初始化
                person.getId();
                person.getPersonName();
                members.add(person);
            }
        }
        return members;
    }

    // 獲取小組成員列表（包含角色）
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public List<GroupPerson> getGroupMembersWithRole(Long groupId) {
        return groupPersonRepository.findActiveMembersByGroupId(groupId);
    }

    // 獲取未加入小組的人員列表（只返回沒有加入任何活躍小組的人員）
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public List<Person> getNonGroupMembers(Long groupId) {
        // 獲取所有活躍人員
        List<Person> allActivePersons = personRepository.findByIsActiveTrueOrderByPersonNameAsc();
        
        // 獲取所有已加入任何活躍小組的人員ID
        List<GroupPerson> allActiveGroupPersons = groupPersonRepository.findAll().stream()
                .filter(gp -> gp.getIsActive() != null && gp.getIsActive())
                .collect(java.util.stream.Collectors.toList());
        List<Long> allMemberIds = allActiveGroupPersons.stream()
                .map(gp -> gp.getPersonId() != null ? gp.getPersonId() : gp.getPerson().getId())
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        
        // 只返回沒有加入任何活躍小組的人員
        return allActivePersons.stream()
                .filter(p -> !allMemberIds.contains(p.getId()))
                .collect(java.util.stream.Collectors.toList());
    }

    // 獲取未加入任何活躍小組的人員列表（用於新增小組時）
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public List<Person> getPersonsWithoutGroup() {
        // 獲取所有活躍人員
        List<Person> allActivePersons = personRepository.findByIsActiveTrueOrderByPersonNameAsc();
        
        // 獲取所有已加入任何活躍小組的人員ID
        List<GroupPerson> allActiveGroupPersons = groupPersonRepository.findAll().stream()
                .filter(gp -> gp.getIsActive() != null && gp.getIsActive())
                .collect(java.util.stream.Collectors.toList());
        List<Long> allMemberIds = allActiveGroupPersons.stream()
                .map(gp -> gp.getPersonId() != null ? gp.getPersonId() : gp.getPerson().getId())
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        
        // 只返回沒有加入任何活躍小組的人員
        return allActivePersons.stream()
                .filter(p -> !allMemberIds.contains(p.getId()))
                .collect(java.util.stream.Collectors.toList());
    }

    // 批量添加成員到小組（支援多小組）
    @Transactional(transactionManager = "churchTransactionManager")
    public void addMembersToGroup(Long groupId, List<Long> personIds, LocalDate joinedAt) {
        addMembersToGroupWithRoles(groupId, personIds, null, joinedAt);
    }

    // 批量添加成員到小組（支援多小組和角色設定）
    @Transactional(transactionManager = "churchTransactionManager")
    public void addMembersToGroupWithRoles(Long groupId, List<Long> personIds, Map<Long, String> personRoles, LocalDate joinedAt) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("小組不存在：" + groupId));
        
        if (joinedAt == null) {
            joinedAt = LocalDate.now();
        }
        
        for (Long personId : personIds) {
            Person person = personRepository.findById(personId)
                    .orElseThrow(() -> new RuntimeException("人員不存在：" + personId));
            
            // 檢查是否已經加入該小組（包括歷史記錄）
            Optional<GroupPerson> existing = groupPersonRepository.findByGroupIdAndPersonId(groupId, personId);
            if (existing.isPresent()) {
                GroupPerson gp = existing.get();
                // 如果是歷史記錄，恢復為活躍
                if (gp.getIsActive() == null || !gp.getIsActive()) {
                    gp.setIsActive(true);
                    gp.setLeftAt(null);
                    gp.setJoinedAt(joinedAt); // 更新加入時間
                    // 更新角色（如果提供）
                    if (personRoles != null && personRoles.containsKey(personId)) {
                        gp.setRole(personRoles.get(personId));
                    }
                    groupPersonRepository.save(gp);
                } else if (personRoles != null && personRoles.containsKey(personId)) {
                    // 更新現有成員的角色
                    gp.setRole(personRoles.get(personId));
                    groupPersonRepository.save(gp);
                }
                continue; // 已經加入，跳過
            }
            
            // 創建新的關聯（不刪除其他小組的關聯，支援多小組）
            GroupPerson groupPerson = new GroupPerson();
            groupPerson.setGroup(group);
            groupPerson.setPerson(person);
            groupPerson.setJoinedAt(joinedAt);
            groupPerson.setIsActive(true);
            // 設定角色（如果提供）
            if (personRoles != null && personRoles.containsKey(personId)) {
                groupPerson.setRole(personRoles.get(personId));
            } else {
                groupPerson.setRole("MEMBER");
            }
            groupPersonRepository.save(groupPerson);
        }
    }

    // 從小組移除成員（保留歷史記錄）
    @Transactional(transactionManager = "churchTransactionManager")
    public void removeMemberFromGroup(Long groupId, Long personId) {
        GroupPerson groupPerson = groupPersonRepository.findByGroupIdAndPersonIdAndIsActiveTrue(groupId, personId)
                .orElseThrow(() -> new RuntimeException("人員不在該小組中或已離開"));
        
        // 不刪除記錄，而是標記為非活躍
        groupPerson.setIsActive(false);
        groupPerson.setLeftAt(LocalDate.now());
        groupPersonRepository.save(groupPerson);
    }

    // 批量移除成員
    @Transactional(transactionManager = "churchTransactionManager")
    public void removeMembersFromGroup(Long groupId, List<Long> personIds) {
        for (Long personId : personIds) {
            removeMemberFromGroup(groupId, personId);
        }
    }

    // 批量設置人員的小組關聯（支援多小組和指定加入時間）
    @Transactional(transactionManager = "churchTransactionManager")
    public void setPersonGroups(Long personId, List<Long> groupIds, LocalDate joinedAt) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("人員不存在：" + personId));
        
        if (joinedAt == null) {
            joinedAt = LocalDate.now();
        }
        
        // 獲取該人員目前所有活躍的小組關聯
        List<GroupPerson> currentGroupPersons = groupPersonRepository.findByPersonIdAndIsActiveTrue(personId);
        List<Long> currentGroupIds = currentGroupPersons.stream()
                .map(gp -> gp.getGroupId() != null ? gp.getGroupId() : gp.getGroup().getId())
                .collect(java.util.stream.Collectors.toList());
        
        // 找出需要移除的小組（目前有但不在新列表中的）
        List<Long> toRemove = currentGroupIds.stream()
                .filter(id -> !groupIds.contains(id))
                .collect(java.util.stream.Collectors.toList());
        
        // 找出需要添加的小組（在新列表中但目前沒有的）
        List<Long> toAdd = groupIds.stream()
                .filter(id -> !currentGroupIds.contains(id))
                .collect(java.util.stream.Collectors.toList());
        
        // 移除不再屬於的小組（標記為非活躍）
        for (Long groupId : toRemove) {
            Optional<GroupPerson> gp = groupPersonRepository.findByGroupIdAndPersonIdAndIsActiveTrue(groupId, personId);
            if (gp.isPresent()) {
                GroupPerson groupPerson = gp.get();
                groupPerson.setIsActive(false);
                groupPerson.setLeftAt(LocalDate.now());
                groupPersonRepository.save(groupPerson);
            }
        }
        
        // 添加新的小組關聯
        for (Long groupId : toAdd) {
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("小組不存在：" + groupId));
            
            // 檢查是否有歷史記錄
            Optional<GroupPerson> existing = groupPersonRepository.findByGroupIdAndPersonId(groupId, personId);
            if (existing.isPresent()) {
                // 恢復歷史記錄
                GroupPerson gp = existing.get();
                gp.setIsActive(true);
                gp.setLeftAt(null);
                gp.setJoinedAt(joinedAt);
                groupPersonRepository.save(gp);
            } else {
                // 創建新記錄
                GroupPerson groupPerson = new GroupPerson();
                groupPerson.setGroup(group);
                groupPerson.setPerson(person);
                groupPerson.setJoinedAt(joinedAt);
                groupPerson.setIsActive(true);
                groupPersonRepository.save(groupPerson);
            }
        }
    }

    // 更新成員角色
    @Transactional(transactionManager = "churchTransactionManager")
    public void updateMemberRole(Long groupId, Long personId, String role) {
        GroupPerson groupPerson = groupPersonRepository.findByGroupIdAndPersonIdAndIsActiveTrue(groupId, personId)
                .orElseThrow(() -> new RuntimeException("人員不在該小組中或已離開"));
        
        if (role == null || (!role.equals("MEMBER") && !role.equals("LEADER") && !role.equals("ASSISTANT_LEADER"))) {
            throw new RuntimeException("無效的角色：" + role);
        }
        
        groupPerson.setRole(role);
        groupPersonRepository.save(groupPerson);
    }
}

