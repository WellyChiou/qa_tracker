package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.ServiceSchedule;
import com.example.helloworld.entity.church.ServiceScheduleDate;
import com.example.helloworld.entity.church.ServiceSchedulePositionConfig;
import com.example.helloworld.entity.church.ServiceScheduleAssignment;
import com.example.helloworld.entity.church.Position;
import com.example.helloworld.entity.church.Person;
import com.example.helloworld.repository.church.ServiceScheduleRepository;
import com.example.helloworld.repository.church.PositionRepository;
import com.example.helloworld.repository.church.PersonRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ServiceScheduleService {

    @Autowired
    private ServiceScheduleRepository repository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private PersonRepository personRepository;

    @PersistenceContext(unitName = "churchEntityManagerFactory")
    private EntityManager entityManager;

    /**
     * 保存服事安排表（優化版本：減少資料庫查詢，使用批量操作）
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ServiceSchedule saveSchedule(String name, List<Map<String, Object>> scheduleData) {
        // 1. 創建主表記錄
        ServiceSchedule schedule = new ServiceSchedule();
        schedule.setName(name);
        schedule = repository.save(schedule);

        // 2. 如果有服事表數據，保存到關聯表
        if (scheduleData != null && !scheduleData.isEmpty()) {
            // 2.1 動態提取所有崗位代碼（從 scheduleData 中）
            Set<String> positionCodeSet = new HashSet<>();
            for (Map<String, Object> item : scheduleData) {
                for (String key : item.keySet()) {
                    if (key.endsWith("Id") && !key.equals("date")) {
                        String positionCode = key.substring(0, key.length() - 2);
                        positionCodeSet.add(positionCode);
                    } else if (!key.equals("date") && !key.equals("formattedDate") && !key.equals("dayOfWeek")) {
                        // 檢查是否為崗位名稱（不是以 Id 結尾，也不是日期相關欄位）
                        // 如果該 key 對應的值是字符串（人員名稱），則可能是崗位代碼
                        Object value = item.get(key);
                        if (value instanceof String && !value.toString().trim().isEmpty()) {
                            // 檢查是否有對應的 Id 欄位，如果沒有，則可能是崗位代碼
                            if (!item.containsKey(key + "Id")) {
                                positionCodeSet.add(key);
                            }
                        }
                    }
                }
            }
            
            // 2.2 預先載入所有需要的 Position 和 Person（減少資料庫查詢）
            Map<String, Position> positionMap = new HashMap<>();
            for (String positionCode : positionCodeSet) {
                positionRepository.findByPositionCode(positionCode)
                    .ifPresent(position -> positionMap.put(positionCode, position));
            }

            // 預先載入所有 Person 到 Map（使用 ID 作為 key，同時保留名稱映射以向後兼容）
            List<Person> allPersons = personRepository.findAll();
            Map<Long, Person> personMapById = new HashMap<>();
            Map<String, Person> personMapByName = new HashMap<>();
            for (Person person : allPersons) {
                personMapById.put(person.getId(), person);
                // 使用 personName 作為 key（向後兼容）
                personMapByName.put(person.getPersonName(), person);
                // 如果有 displayName，也加入 Map
                if (person.getDisplayName() != null && !person.getDisplayName().trim().isEmpty()) {
                    personMapByName.put(person.getDisplayName(), person);
                }
            }

            // 2.3 批量創建所有實體
            List<ServiceScheduleDate> datesToSave = new ArrayList<>();
            List<ServiceSchedulePositionConfig> configsToSave = new ArrayList<>();
            List<ServiceScheduleAssignment> assignmentsToSave = new ArrayList<>();

            for (Map<String, Object> item : scheduleData) {
                String dateStr = (String) item.get("date");
                if (dateStr != null && !dateStr.isEmpty()) {
                    LocalDate date = LocalDate.parse(dateStr);
                    ServiceScheduleDate scheduleDate = new ServiceScheduleDate();
                    scheduleDate.setServiceSchedule(schedule);
                    scheduleDate.setDate(date);
                    datesToSave.add(scheduleDate);

                    // 保存崗位配置和人員分配
                    for (String positionCode : positionCodeSet) {
                        // 優先使用 ID，如果沒有 ID 則使用名稱（向後兼容）
                        Long personId = null;
                        Object personIdObj = item.get(positionCode + "Id");
                        if (personIdObj != null) {
                            if (personIdObj instanceof Number) {
                                personId = ((Number) personIdObj).longValue();
                            } else if (personIdObj instanceof String && !((String) personIdObj).trim().isEmpty()) {
                                try {
                                    personId = Long.parseLong((String) personIdObj);
                                } catch (NumberFormatException e) {
                                    // 忽略
                                }
                            }
                        }
                        
                        Person person = null;
                        if (personId != null) {
                            person = personMapById.get(personId);
                        }
                        
                        // 如果通過 ID 找不到，嘗試使用名稱（向後兼容）
                        if (person == null) {
                            String personName = (String) item.get(positionCode);
                            if (personName != null && !personName.trim().isEmpty()) {
                                person = personMapByName.get(personName);
                            }
                        }
                        
                        if (person != null) {
                            Position position = positionMap.get(positionCode);
                            if (position != null) {
                                // 創建崗位配置
                                ServiceSchedulePositionConfig config = new ServiceSchedulePositionConfig();
                                config.setServiceScheduleDate(scheduleDate);
                                config.setPosition(position);
                                config.setPersonCount(1);
                                configsToSave.add(config);

                                // 創建人員分配
                                ServiceScheduleAssignment assignment = new ServiceScheduleAssignment();
                                assignment.setServiceSchedulePositionConfig(config);
                                assignment.setPerson(person);
                                assignment.setSortOrder(0);
                                assignmentsToSave.add(assignment);
            }
                        }
                    }
                }
            }

            // 2.3 批量保存（減少 flush 次數）
            int batchSize = 50;
            for (int i = 0; i < datesToSave.size(); i++) {
                entityManager.persist(datesToSave.get(i));
                if ((i + 1) % batchSize == 0) {
                    entityManager.flush();
                }
            }

            for (int i = 0; i < configsToSave.size(); i++) {
                entityManager.persist(configsToSave.get(i));
                if ((i + 1) % batchSize == 0) {
                    entityManager.flush();
        }
            }

            for (int i = 0; i < assignmentsToSave.size(); i++) {
                entityManager.persist(assignmentsToSave.get(i));
                if ((i + 1) % batchSize == 0) {
                    entityManager.flush();
                }
            }
        }

        // 3. 最後一次 flush 確保所有數據都保存
        entityManager.flush();
        entityManager.refresh(schedule);

        return schedule;
    }

    /**
     * 根據 ID 獲取安排表（包含 dates 關聯數據）
     * 注意：不能同時 FETCH 多個 List 集合，需要分步載入
     */
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public Optional<ServiceSchedule> getScheduleById(Long id) {
        // 第一步：載入 ServiceSchedule 和 dates
        String jpql1 = "SELECT DISTINCT s FROM ServiceSchedule s " +
                      "LEFT JOIN FETCH s.dates d " +
                      "WHERE s.id = :id";
        
        List<ServiceSchedule> results = entityManager.createQuery(jpql1, ServiceSchedule.class)
            .setParameter("id", id)
            .getResultList();
        
        if (results.isEmpty()) {
            return Optional.empty();
        }
        
        ServiceSchedule schedule = results.get(0);
        
        // 第二步：手動觸發載入 positionConfigs 和 assignments
        // 在 Session 內遍歷 dates，觸發懶加載
        if (schedule.getDates() != null) {
            for (ServiceScheduleDate date : schedule.getDates()) {
                // 觸發載入 positionConfigs
                date.getPositionConfigs().size();
                
                // 觸發載入每個 positionConfig 的 assignments 和 position
                if (date.getPositionConfigs() != null) {
                    for (ServiceSchedulePositionConfig config : date.getPositionConfigs()) {
                        // 觸發載入 position
                        config.getPosition().getPositionName();
                        
                        // 觸發載入 assignments
                        if (config.getAssignments() != null) {
                            config.getAssignments().size();
                            
                            // 觸發載入每個 assignment 的 person
                            for (ServiceScheduleAssignment assignment : config.getAssignments()) {
                                if (assignment.getPerson() != null) {
                                    assignment.getPerson().getPersonName();
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return Optional.of(schedule);
    }

    /**
     * 獲取所有安排
     */
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public List<ServiceSchedule> getAllSchedules() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 獲取所有安排（包含完整的關聯數據，用於排程器等需要完整數據的場景）
     * 使用多個 JOIN FETCH 查詢預先載入所有需要的關聯數據，避免 N+1 查詢問題
     */
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public List<ServiceSchedule> getAllSchedulesWithFullData() {
        // 第一步：載入 ServiceSchedule 和 dates
        String jpql1 = "SELECT DISTINCT s FROM ServiceSchedule s " +
                      "LEFT JOIN FETCH s.dates d " +
                      "ORDER BY s.createdAt DESC";
        
        List<ServiceSchedule> schedules = entityManager.createQuery(jpql1, ServiceSchedule.class)
            .getResultList();
        
        if (schedules.isEmpty()) {
            return schedules;
        }
        
        // 收集所有需要載入的 date IDs
        List<Long> dateIds = schedules.stream()
            .flatMap(s -> s.getDates() != null ? s.getDates().stream() : java.util.stream.Stream.empty())
            .map(ServiceScheduleDate::getId)
            .filter(id -> id != null)
            .distinct()
            .collect(java.util.stream.Collectors.toList());
        
        if (dateIds.isEmpty()) {
            return schedules;
        }
        
        // 第二步：批量載入 positionConfigs 和 position（避免 N+1）
        String jpql2 = "SELECT DISTINCT pc FROM ServiceSchedulePositionConfig pc " +
                      "LEFT JOIN FETCH pc.position p " +
                      "WHERE pc.serviceScheduleDate.id IN :dateIds";
        
        List<ServiceSchedulePositionConfig> configs = entityManager.createQuery(jpql2, ServiceSchedulePositionConfig.class)
            .setParameter("dateIds", dateIds)
            .getResultList();
        
        // 收集所有需要載入的 config IDs
        List<Long> configIds = configs.stream()
            .map(ServiceSchedulePositionConfig::getId)
            .filter(id -> id != null)
            .distinct()
            .collect(java.util.stream.Collectors.toList());
        
        // 建立 config ID 到 config 物件的映射，以便後續關聯 assignments
        Map<Long, ServiceSchedulePositionConfig> configMap = new HashMap<>();
        for (ServiceSchedulePositionConfig config : configs) {
            configMap.put(config.getId(), config);
        }
        
        if (!configIds.isEmpty()) {
            // 第三步：批量載入 assignments 和 person（避免 N+1）
            String jpql3 = "SELECT DISTINCT a FROM ServiceScheduleAssignment a " +
                          "LEFT JOIN FETCH a.person p " +
                          "WHERE a.serviceSchedulePositionConfig.id IN :configIds";
            
            List<ServiceScheduleAssignment> assignments = entityManager.createQuery(jpql3, ServiceScheduleAssignment.class)
                .setParameter("configIds", configIds)
                .getResultList();
            
            // 將 assignments 關聯回對應的 config（確保在 Session 中正確關聯）
            for (ServiceScheduleAssignment assignment : assignments) {
                ServiceSchedulePositionConfig config = assignment.getServiceSchedulePositionConfig();
                if (config != null && configMap.containsKey(config.getId())) {
                    // 確保 assignments 集合已初始化並包含這個 assignment
                    ServiceSchedulePositionConfig targetConfig = configMap.get(config.getId());
                    if (targetConfig.getAssignments() != null) {
                        // 觸發初始化並確保 assignment 在集合中
                        targetConfig.getAssignments().size();
                        // 如果 assignment 不在集合中，手動添加（但這不應該發生，因為 Hibernate 應該自動處理）
                        if (!targetConfig.getAssignments().contains(assignment)) {
                            // 使用反射或直接訪問來確保關聯正確
                            // 實際上，Hibernate 應該已經處理了這個關聯，我們只需要觸發初始化
                        }
                    }
                }
            }
        }
        
        // 第四步：確保所有關聯數據都已載入並初始化（觸發初始化，確保資料在 Session 中）
        for (ServiceSchedule schedule : schedules) {
            if (schedule.getDates() != null) {
                for (ServiceScheduleDate date : schedule.getDates()) {
                    // 這些調用會觸發初始化，確保資料已載入到 Session 中
                    if (date.getPositionConfigs() != null) {
                        date.getPositionConfigs().size(); // 觸發初始化
                        for (ServiceSchedulePositionConfig config : date.getPositionConfigs()) {
                            if (config.getPosition() != null) {
                                config.getPosition().getPositionName(); // 觸發初始化
                            }
                            // 重要：強制初始化 assignments 集合
                            if (config.getAssignments() != null) {
                                int size = config.getAssignments().size(); // 觸發初始化
                                // 遍歷所有 assignments 並觸發 person 的初始化
                                for (ServiceScheduleAssignment assignment : config.getAssignments()) {
                                    if (assignment.getPerson() != null) {
                                        assignment.getPerson().getPersonName(); // 觸發初始化
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return schedules;
    }
    
    /**
     * 獲取所有安排（包含日期範圍信息）
     */
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public List<Map<String, Object>> getAllSchedulesWithDateRange() {
        List<ServiceSchedule> schedules = repository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (ServiceSchedule schedule : schedules) {
            Map<String, Object> scheduleMap = new HashMap<>();
            scheduleMap.put("id", schedule.getId());
            scheduleMap.put("name", schedule.getName());
            scheduleMap.put("createdAt", schedule.getCreatedAt());
            scheduleMap.put("updatedAt", schedule.getUpdatedAt());
            
            // 計算日期範圍（在事務中訪問 LAZY 加載的 dates）
            List<ServiceScheduleDate> dates = schedule.getDates();
            if (dates != null && !dates.isEmpty()) {
                LocalDate minDate = null;
                LocalDate maxDate = null;
                
                for (ServiceScheduleDate date : dates) {
                    if (minDate == null || date.getDate().isBefore(minDate)) {
                        minDate = date.getDate();
                    }
                    if (maxDate == null || date.getDate().isAfter(maxDate)) {
                        maxDate = date.getDate();
                    }
                }
                
                if (minDate != null && maxDate != null) {
                    scheduleMap.put("startDate", minDate.toString());
                    scheduleMap.put("endDate", maxDate.toString());
                }
            }
            
            result.add(scheduleMap);
        }
        
        return result;
    }

    /**
     * 更新安排表
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ServiceSchedule updateSchedule(Long id, String name, List<Map<String, Object>> scheduleData) {
        // 1. 載入現有的服事表
        ServiceSchedule schedule = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("找不到指定的安排表"));

        // 2. 更新名稱
        schedule.setName(name);

        // 3. 刪除現有的 dates（會級聯刪除 positionConfigs 和 assignments）
        if (schedule.getDates() != null) {
            schedule.getDates().clear();
        }
        repository.save(schedule);
        entityManager.flush();

        // 4. 保存新的 dates 和人員分配（使用優化後的批量操作）
        if (scheduleData != null && !scheduleData.isEmpty()) {
            // 4.1 動態提取所有崗位代碼（從 scheduleData 中）
            Set<String> positionCodeSet = new HashSet<>();
            for (Map<String, Object> item : scheduleData) {
                for (String key : item.keySet()) {
                    if (key.endsWith("Id") && !key.equals("date")) {
                        String positionCode = key.substring(0, key.length() - 2);
                        positionCodeSet.add(positionCode);
                    } else if (!key.equals("date") && !key.equals("formattedDate") && !key.equals("dayOfWeek")) {
                        // 檢查是否為崗位名稱（不是以 Id 結尾，也不是日期相關欄位）
                        // 如果該 key 對應的值是字符串（人員名稱），則可能是崗位代碼
                        Object value = item.get(key);
                        if (value instanceof String && !value.toString().trim().isEmpty()) {
                            // 檢查是否有對應的 Id 欄位，如果沒有，則可能是崗位代碼
                            if (!item.containsKey(key + "Id")) {
                                positionCodeSet.add(key);
                            }
                        }
                    }
                }
            }
            
            // 4.2 預先載入所有需要的 Position 和 Person（減少資料庫查詢）
            Map<String, Position> positionMap = new HashMap<>();
            for (String positionCode : positionCodeSet) {
                positionRepository.findByPositionCode(positionCode)
                    .ifPresent(position -> positionMap.put(positionCode, position));
            }

            // 預先載入所有 Person 到 Map（使用 ID 作為 key，同時保留名稱映射以向後兼容）
            List<Person> allPersons = personRepository.findAll();
            Map<Long, Person> personMapById = new HashMap<>();
            Map<String, Person> personMapByName = new HashMap<>();
            for (Person person : allPersons) {
                personMapById.put(person.getId(), person);
                // 使用 personName 作為 key（向後兼容）
                personMapByName.put(person.getPersonName(), person);
                // 如果有 displayName，也加入 Map
                if (person.getDisplayName() != null && !person.getDisplayName().trim().isEmpty()) {
                    personMapByName.put(person.getDisplayName(), person);
                }
            }

            // 4.2 批量創建所有實體
            List<ServiceScheduleDate> datesToSave = new ArrayList<>();
            List<ServiceSchedulePositionConfig> configsToSave = new ArrayList<>();
            List<ServiceScheduleAssignment> assignmentsToSave = new ArrayList<>();

            for (Map<String, Object> item : scheduleData) {
                String dateStr = (String) item.get("date");
                if (dateStr != null && !dateStr.isEmpty()) {
                    LocalDate date = LocalDate.parse(dateStr);
                    ServiceScheduleDate scheduleDate = new ServiceScheduleDate();
                    scheduleDate.setServiceSchedule(schedule);
                    scheduleDate.setDate(date);
                    datesToSave.add(scheduleDate);

                    // 保存崗位配置和人員分配
                    for (String positionCode : positionCodeSet) {
                        // 優先使用 ID，如果沒有 ID 則使用名稱（向後兼容）
                        Long personId = null;
                        Object personIdObj = item.get(positionCode + "Id");
                        if (personIdObj != null) {
                            if (personIdObj instanceof Number) {
                                personId = ((Number) personIdObj).longValue();
                            } else if (personIdObj instanceof String && !((String) personIdObj).trim().isEmpty()) {
                                try {
                                    personId = Long.parseLong((String) personIdObj);
                                } catch (NumberFormatException e) {
                                    // 忽略
                                }
                            }
                        }
                        
                        Person person = null;
                        if (personId != null) {
                            person = personMapById.get(personId);
                        }
                        
                        // 如果通過 ID 找不到，嘗試使用名稱（向後兼容）
                        if (person == null) {
                            String personName = (String) item.get(positionCode);
                            if (personName != null && !personName.trim().isEmpty()) {
                                person = personMapByName.get(personName);
                            }
                        }
                        
                        if (person != null) {
                            Position position = positionMap.get(positionCode);
                            if (position != null) {
                                // 創建崗位配置
                                ServiceSchedulePositionConfig config = new ServiceSchedulePositionConfig();
                                config.setServiceScheduleDate(scheduleDate);
                                config.setPosition(position);
                                config.setPersonCount(1);
                                configsToSave.add(config);

                                // 創建人員分配
                                ServiceScheduleAssignment assignment = new ServiceScheduleAssignment();
                                assignment.setServiceSchedulePositionConfig(config);
                                assignment.setPerson(person);
                                assignment.setSortOrder(0);
                                assignmentsToSave.add(assignment);
                            }
                        }
                    }
                }
            }

            // 4.3 批量保存（減少 flush 次數）
            int batchSize = 50;
            for (int i = 0; i < datesToSave.size(); i++) {
                entityManager.persist(datesToSave.get(i));
                if ((i + 1) % batchSize == 0) {
                    entityManager.flush();
                }
            }

            for (int i = 0; i < configsToSave.size(); i++) {
                entityManager.persist(configsToSave.get(i));
                if ((i + 1) % batchSize == 0) {
                    entityManager.flush();
        }
            }

            for (int i = 0; i < assignmentsToSave.size(); i++) {
                entityManager.persist(assignmentsToSave.get(i));
                if ((i + 1) % batchSize == 0) {
                    entityManager.flush();
                }
            }
        }

        // 5. 最後一次 flush 確保所有數據都保存
        entityManager.flush();
        entityManager.refresh(schedule);

        return schedule;
    }

    /**
     * 刪除安排表
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public void deleteSchedule(Long id) {
        repository.deleteById(id);
    }
}

