package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.Position;
import com.example.helloworld.entity.church.Person;
import com.example.helloworld.entity.church.PositionPerson;
import com.example.helloworld.entity.church.Group;
import com.example.helloworld.entity.church.GroupPerson;
import com.example.helloworld.repository.church.PositionRepository;
import com.example.helloworld.repository.church.PersonRepository;
import com.example.helloworld.repository.church.PositionPersonRepository;
import com.example.helloworld.repository.church.GroupRepository;
import com.example.helloworld.repository.church.GroupPersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PositionService {

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PositionPersonRepository positionPersonRepository;

    @Autowired
    private GroupPersonRepository groupPersonRepository;

    @Autowired
    private GroupRepository groupRepository;

    // ========== 崗位相關方法 ==========

    /**
     * 獲取所有崗位
     */
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public List<Position> getAllPositions() {
        return positionRepository.findAllByOrderBySortOrderAsc();
    }

    /**
     * 獲取所有啟用的崗位
     */
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public List<Position> getActivePositions() {
        return positionRepository.findByIsActiveTrueOrderBySortOrderAsc();
    }

    /**
     * 根據 ID 獲取崗位
     */
    public Optional<Position> getPositionById(Long id) {
        return positionRepository.findById(id);
    }

    /**
     * 根據代碼獲取崗位
     */
    public Optional<Position> getPositionByCode(String code) {
        return positionRepository.findByPositionCode(code);
    }

    /**
     * 創建崗位
     */
    @Transactional
    public Position createPosition(Position position) {
        return positionRepository.save(position);
    }

    /**
     * 更新崗位
     */
    @Transactional
    public Position updatePosition(Long id, Position positionData) {
        Position position = positionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("崗位不存在：ID = " + id));
        
        position.setPositionName(positionData.getPositionName());
        position.setDescription(positionData.getDescription());
        position.setIsActive(positionData.getIsActive());
        position.setSortOrder(positionData.getSortOrder());
        if (positionData.getAllowDuplicate() != null) {
            position.setAllowDuplicate(positionData.getAllowDuplicate());
        }
        
        return positionRepository.save(position);
    }

    /**
     * 刪除崗位
     */
    @Transactional
    public void deletePosition(Long id) {
        positionRepository.deleteById(id);
    }

    // ========== 人員相關方法 ==========

    /**
     * 獲取所有人員
     */
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public List<Person> getAllPersons() {
        return personRepository.findAllByOrderByPersonNameAsc();
    }

    /**
     * 獲取所有啟用的人員
     */
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public List<Person> getActivePersons() {
        return personRepository.findByIsActiveTrueOrderByPersonNameAsc();
    }

    /**
     * 根據 ID 獲取人員
     */
    public Optional<Person> getPersonById(Long id) {
        return personRepository.findById(id);
    }

    /**
     * 創建人員（支援多小組）
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public Person createPerson(Person person) {
        Person saved = personRepository.save(person);
        
        // 如果指定了 group_id（向後兼容），創建 group_persons 關聯
        if (saved.getGroupId() != null) {
            updatePersonGroupRelation(saved.getId(), saved.getGroupId());
        }
        
        return saved;
    }

    /**
     * 更新人員（支援多小組）
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public Person updatePerson(Long id, Person personData) {
        Person person = personRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("人員不存在：ID = " + id));
        
        Long oldGroupId = person.getGroupId();
        
        person.setPersonName(personData.getPersonName());
        person.setDisplayName(personData.getDisplayName());
        person.setPhone(personData.getPhone());
        person.setEmail(personData.getEmail());
        person.setNotes(personData.getNotes());
        person.setIsActive(personData.getIsActive());
        person.setMemberNo(personData.getMemberNo());
        person.setBirthday(personData.getBirthday());
        person.setGroupId(personData.getGroupId()); // 保留用於向後兼容
        
        Person saved = personRepository.save(person);
        
        // 如果 group_id 發生變化（向後兼容），更新 group_persons 關聯
        if (!Objects.equals(oldGroupId, saved.getGroupId())) {
            // 移除舊的關聯（標記為非活躍）
            if (oldGroupId != null) {
                Optional<GroupPerson> oldGp = groupPersonRepository.findByGroupIdAndPersonIdAndIsActiveTrue(oldGroupId, id);
                if (oldGp.isPresent()) {
                    GroupPerson gp = oldGp.get();
                    gp.setIsActive(false);
                    gp.setLeftAt(LocalDate.now());
                    groupPersonRepository.save(gp);
                }
            }
            // 創建新的關聯
            if (saved.getGroupId() != null) {
                updatePersonGroupRelation(id, saved.getGroupId());
            }
        }
        
        return saved;
    }
    
    /**
     * 更新人員與小組的關聯（支援多小組，保留歷史記錄）
     */
    @Transactional(transactionManager = "churchTransactionManager")
    private void updatePersonGroupRelation(Long personId, Long groupId) {
        if (groupId == null) {
            return;
        }
        
        // 檢查是否已經存在活躍的關聯
        Optional<GroupPerson> existing = groupPersonRepository.findByGroupIdAndPersonIdAndIsActiveTrue(groupId, personId);
        if (existing.isPresent()) {
            return; // 已經存在，不需要重複創建
        }
        
        // 檢查是否有歷史記錄
        Optional<GroupPerson> historical = groupPersonRepository.findByGroupIdAndPersonId(groupId, personId);
        if (historical.isPresent()) {
            // 恢復歷史記錄
            GroupPerson gp = historical.get();
            gp.setIsActive(true);
            gp.setLeftAt(null);
            gp.setJoinedAt(LocalDate.now());
            groupPersonRepository.save(gp);
            return;
        }
        
        // 創建新的關聯
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("人員不存在：" + personId));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("小組不存在：" + groupId));
        
        GroupPerson groupPerson = new GroupPerson();
        groupPerson.setPerson(person);
        groupPerson.setGroup(group);
        groupPerson.setJoinedAt(LocalDate.now());
        groupPerson.setIsActive(true);
        groupPersonRepository.save(groupPerson);
    }

    /**
     * 批量設置人員的小組關聯（支援多小組和指定加入時間）
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public void setPersonGroups(Long personId, List<Long> groupIds, LocalDate joinedAt) {
        if (joinedAt == null) {
            joinedAt = LocalDate.now();
        }
        
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("人員不存在：" + personId));
        
        // 獲取該人員目前所有活躍的小組關聯
        List<GroupPerson> currentGroupPersons = groupPersonRepository.findByPersonIdAndIsActiveTrue(personId);
        List<Long> currentGroupIds = currentGroupPersons.stream()
                .map(gp -> gp.getGroupId() != null ? gp.getGroupId() : gp.getGroup().getId())
                .collect(Collectors.toList());
        
        // 找出需要移除的小組（目前有但不在新列表中的）
        List<Long> toRemove = currentGroupIds.stream()
                .filter(id -> !groupIds.contains(id))
                .collect(Collectors.toList());
        
        // 找出需要添加的小組（在新列表中但目前沒有的）
        List<Long> toAdd = groupIds.stream()
                .filter(id -> !currentGroupIds.contains(id))
                .collect(Collectors.toList());
        
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

    /**
     * 刪除人員
     */
    @Transactional
    public void deletePerson(Long id) {
        personRepository.deleteById(id);
    }

    // ========== 崗位人員關聯相關方法 ==========

    /**
     * 獲取崗位的人員列表（按日期類型分組）
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public Map<String, List<Map<String, Object>>> getPositionPersonsByDayType(Long positionId) {
        List<PositionPerson> positionPersons = positionPersonRepository.findByPositionIdOrdered(positionId);
        
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        result.put("saturday", new ArrayList<>());
        result.put("sunday", new ArrayList<>());
        
        for (PositionPerson pp : positionPersons) {
            Map<String, Object> personInfo = new HashMap<>();
            personInfo.put("id", pp.getId());
            personInfo.put("personId", pp.getPerson().getId());
            personInfo.put("personName", pp.getPerson().getPersonName());
            personInfo.put("displayName", pp.getPerson().getDisplayName());
            personInfo.put("sortOrder", pp.getSortOrder());
            personInfo.put("includeInAutoSchedule", pp.getIncludeInAutoSchedule() != null ? pp.getIncludeInAutoSchedule() : true);
            
            result.get(pp.getDayType()).add(personInfo);
        }
        
        return result;
    }

    /**
     * 為崗位添加人員（指定日期類型）
     */
    @Transactional
    public PositionPerson addPersonToPosition(Long positionId, Long personId, String dayType) {
        Position position = positionRepository.findById(positionId)
            .orElseThrow(() -> new RuntimeException("崗位不存在：ID = " + positionId));
        
        Person person = personRepository.findById(personId)
            .orElseThrow(() -> new RuntimeException("人員不存在：ID = " + personId));
        
        // 檢查是否已存在
        Optional<PositionPerson> existing = positionPersonRepository
            .findByPositionIdAndPersonIdAndDayType(positionId, personId, dayType);
        
        if (existing.isPresent()) {
            throw new RuntimeException("該人員已在此崗位的 " + dayType + " 中");
        }
        
        PositionPerson positionPerson = new PositionPerson();
        positionPerson.setPosition(position);
        positionPerson.setPerson(person);
        positionPerson.setDayType(dayType);
        
        // 設置排序順序（設為最後）
        List<PositionPerson> existingPersons = positionPersonRepository
            .findByPositionIdAndDayType(positionId, dayType);
        int maxSortOrder = existingPersons.stream()
            .mapToInt(pp -> pp.getSortOrder() != null ? pp.getSortOrder() : 0)
            .max()
            .orElse(0);
        positionPerson.setSortOrder(maxSortOrder + 1);
        
        return positionPersonRepository.save(positionPerson);
    }

    /**
     * 從崗位移除人員
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public void removePersonFromPosition(Long positionId, Long personId, String dayType) {
        // 先檢查是否存在
        Optional<PositionPerson> existing = positionPersonRepository
            .findByPositionIdAndPersonIdAndDayType(positionId, personId, dayType);
        
        if (existing.isEmpty()) {
            throw new RuntimeException("該人員在此崗位的 " + dayType + " 中不存在");
        }
        
        // 執行刪除
        positionPersonRepository.deleteByPositionIdAndPersonIdAndDayType(positionId, personId, dayType);
    }

    /**
     * 更新崗位人員的排序
     */
    @Transactional
    public void updatePositionPersonOrder(Long positionId, String dayType, List<Long> personIds) {
        List<PositionPerson> positionPersons = positionPersonRepository
            .findByPositionIdAndDayType(positionId, dayType);
        
        Map<Long, PositionPerson> personMap = positionPersons.stream()
            .collect(Collectors.toMap(pp -> pp.getPerson().getId(), pp -> pp));
        
        for (int i = 0; i < personIds.size(); i++) {
            PositionPerson pp = personMap.get(personIds.get(i));
            if (pp != null) {
                pp.setSortOrder(i);
                positionPersonRepository.save(pp);
            }
        }
    }

    /**
     * 更新崗位人員的 includeInAutoSchedule 狀態
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public void updatePositionPersonIncludeInAutoSchedule(Long positionPersonId, Boolean includeInAutoSchedule) {
        PositionPerson positionPerson = positionPersonRepository.findById(positionPersonId)
            .orElseThrow(() -> new RuntimeException("崗位人員關聯不存在：ID = " + positionPersonId));
        
        positionPerson.setIncludeInAutoSchedule(includeInAutoSchedule != null ? includeInAutoSchedule : true);
        positionPersonRepository.save(positionPerson);
    }

    /**
     * 獲取完整的崗位配置（用於前端顯示）
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public Map<String, Object> getFullPositionConfig() {
        List<Position> positions = positionRepository.findByIsActiveTrueOrderBySortOrderAsc();
        Map<String, Object> config = new LinkedHashMap<>();
        
        for (Position position : positions) {
            Map<String, List<Map<String, Object>>> personsByDay = getPositionPersonsByDayType(position.getId());
            
            Map<String, Object> positionData = new HashMap<>();
            positionData.put("id", position.getId());
            positionData.put("positionCode", position.getPositionCode());
            positionData.put("positionName", position.getPositionName());
            positionData.put("sortOrder", position.getSortOrder() != null ? position.getSortOrder() : 0);
            positionData.put("allowDuplicate", position.getAllowDuplicate() != null ? position.getAllowDuplicate() : false);
            positionData.put("saturday", personsByDay.get("saturday"));
            positionData.put("sunday", personsByDay.get("sunday"));
            
            config.put(position.getPositionCode(), positionData);
        }
        
        return config;
    }
}

