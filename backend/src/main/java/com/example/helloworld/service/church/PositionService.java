package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.Position;
import com.example.helloworld.entity.church.Person;
import com.example.helloworld.entity.church.PositionPerson;
import com.example.helloworld.repository.church.PositionRepository;
import com.example.helloworld.repository.church.PersonRepository;
import com.example.helloworld.repository.church.PositionPersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * 創建人員
     */
    @Transactional
    public Person createPerson(Person person) {
        return personRepository.save(person);
    }

    /**
     * 更新人員
     */
    @Transactional
    public Person updatePerson(Long id, Person personData) {
        Person person = personRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("人員不存在：ID = " + id));
        
        person.setPersonName(personData.getPersonName());
        person.setDisplayName(personData.getDisplayName());
        person.setPhone(personData.getPhone());
        person.setEmail(personData.getEmail());
        person.setNotes(personData.getNotes());
        person.setIsActive(personData.getIsActive());
        
        return personRepository.save(person);
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
        Map<String, Object> config = new HashMap<>();
        
        for (Position position : positions) {
            Map<String, List<Map<String, Object>>> personsByDay = getPositionPersonsByDayType(position.getId());
            
            Map<String, Object> positionData = new HashMap<>();
            positionData.put("id", position.getId());
            positionData.put("positionCode", position.getPositionCode());
            positionData.put("positionName", position.getPositionName());
            positionData.put("saturday", personsByDay.get("saturday"));
            positionData.put("sunday", personsByDay.get("sunday"));
            
            config.put(position.getPositionCode(), positionData);
        }
        
        return config;
    }
}

