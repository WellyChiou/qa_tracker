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
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
    public ServiceSchedule saveSchedule(List<Map<String, Object>> scheduleData) {
        // 0. 計算年度
        Integer year = calculateYearFromScheduleData(scheduleData);
        if (year == null) {
            // 如果無法計算年度，使用當前年份
            year = LocalDate.now().getYear();
        }
        
        // 檢查該年度是否已存在服事表（主鍵會自動處理，但我們提供更友好的錯誤訊息）
        if (repository.existsById(year)) {
            throw new RuntimeException("該年度（" + year + "年）已存在服事表，每個年度只能有一個版本");
        }
        
        // 1. 創建主表記錄
        ServiceSchedule schedule = new ServiceSchedule();
        schedule.setYear(year);
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

                    // 保存崗位配置和人員分配（支援多人）
                    for (String positionCode : positionCodeSet) {
                        Position position = positionMap.get(positionCode);
                        if (position == null) {
                            continue;
                        }
                        
                        // 使用 IDs 陣列（多人）
                        List<Long> personIds = new ArrayList<>();
                        Object personIdsObj = item.get(positionCode + "Ids");
                        if (personIdsObj instanceof List) {
                            // 處理 ID 陣列
                            @SuppressWarnings("unchecked")
                            List<Object> idsList = (List<Object>) personIdsObj;
                            for (Object idObj : idsList) {
                                if (idObj instanceof Number) {
                                    personIds.add(((Number) idObj).longValue());
                                } else if (idObj instanceof String && !((String) idObj).trim().isEmpty()) {
                                    try {
                                        personIds.add(Long.parseLong((String) idObj));
                                    } catch (NumberFormatException e) {
                                        // 忽略
                                    }
                                }
                            }
                        }
                        
                        // 如果通過 ID 找不到，嘗試使用名稱（向後兼容）
                        if (personIds.isEmpty()) {
                            String personName = (String) item.get(positionCode);
                            if (personName != null && !personName.trim().isEmpty()) {
                                // 如果名稱包含 "/"，嘗試分割
                                if (personName.contains("/")) {
                                    String[] names = personName.split("/");
                                    for (String name : names) {
                                        Person person = personMapByName.get(name.trim());
                                        if (person != null) {
                                            personIds.add(person.getId());
                                        }
                                    }
                                } else {
                                    Person person = personMapByName.get(personName);
                                    if (person != null) {
                                        personIds.add(person.getId());
                                    }
                                }
                            }
                        }
                        
                        if (!personIds.isEmpty()) {
                            // 創建崗位配置
                            ServiceSchedulePositionConfig config = new ServiceSchedulePositionConfig();
                            config.setServiceScheduleDate(scheduleDate);
                            config.setPosition(position);
                            config.setPersonCount(personIds.size());
                            configsToSave.add(config);

                            // 為每個人員創建分配記錄
                            for (int i = 0; i < personIds.size(); i++) {
                                Long personId = personIds.get(i);
                                Person person = personMapById.get(personId);
                                if (person != null) {
                                    ServiceScheduleAssignment assignment = new ServiceScheduleAssignment();
                                    assignment.setServiceSchedulePositionConfig(config);
                                    assignment.setPerson(person);
                                    assignment.setSortOrder(i); // 使用索引作為 sortOrder
                                    assignmentsToSave.add(assignment);
                                }
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
     * 根據年度獲取安排表（包含 dates 關聯數據）
     * 注意：不能同時 FETCH 多個 List 集合，需要分步載入
     */
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public Optional<ServiceSchedule> getScheduleByYear(Integer year) {
        // 第一步：載入 ServiceSchedule 和 dates
        String jpql1 = "SELECT DISTINCT s FROM ServiceSchedule s " +
                      "LEFT JOIN FETCH s.dates d " +
                      "WHERE s.year = :year";
        
        List<ServiceSchedule> results = entityManager.createQuery(jpql1, ServiceSchedule.class)
            .setParameter("year", year)
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
        // 使用單一查詢載入所有需要的資料，避免 N+1 問題
        // 注意：由於 Hibernate 的限制，無法在同一個查詢中對多個集合使用 JOIN FETCH
        // 所以我們需要分步載入，但確保在事務內完成所有初始化
        
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
        
        // 第二步：批量載入 positionConfigs 和 position
        String jpql2 = "SELECT DISTINCT pc FROM ServiceSchedulePositionConfig pc " +
                      "LEFT JOIN FETCH pc.position p " +
                      "WHERE pc.serviceScheduleDate.id IN :dateIds";
        
        List<ServiceSchedulePositionConfig> configs = entityManager.createQuery(jpql2, ServiceSchedulePositionConfig.class)
            .setParameter("dateIds", dateIds)
            .getResultList();
        
        // 建立 date ID 到 configs 的映射
        Map<Long, List<ServiceSchedulePositionConfig>> configsByDateId = new HashMap<>();
        for (ServiceSchedulePositionConfig config : configs) {
            Long dateId = config.getServiceScheduleDate().getId();
            configsByDateId.computeIfAbsent(dateId, k -> new ArrayList<>()).add(config);
        }
        
        // 收集所有需要載入的 config IDs
        List<Long> configIds = configs.stream()
            .map(ServiceSchedulePositionConfig::getId)
            .filter(id -> id != null)
            .distinct()
            .collect(java.util.stream.Collectors.toList());
        
        // 建立 config ID 到 config 物件的映射
        Map<Long, ServiceSchedulePositionConfig> configMap = new HashMap<>();
        for (ServiceSchedulePositionConfig config : configs) {
            configMap.put(config.getId(), config);
        }
        
        // 第三步：批量載入 assignments 和 person
        Map<Long, List<ServiceScheduleAssignment>> assignmentsByConfigId = new HashMap<>();
        if (!configIds.isEmpty()) {
            String jpql3 = "SELECT DISTINCT a FROM ServiceScheduleAssignment a " +
                          "LEFT JOIN FETCH a.person p " +
                          "WHERE a.serviceSchedulePositionConfig.id IN :configIds";
            
            List<ServiceScheduleAssignment> assignments = entityManager.createQuery(jpql3, ServiceScheduleAssignment.class)
                .setParameter("configIds", configIds)
                .getResultList();
            
            // 按 config ID 分組 assignments
            for (ServiceScheduleAssignment assignment : assignments) {
                Long configId = assignment.getServiceSchedulePositionConfig().getId();
                assignmentsByConfigId.computeIfAbsent(configId, k -> new ArrayList<>()).add(assignment);
            }
        }
        
        // 第四步：在事務內強制初始化所有集合，確保資料正確載入
        // 使用 Hibernate.initialize() 和手動設置來確保關聯正確
        for (ServiceSchedule schedule : schedules) {
            if (schedule.getDates() != null) {
                for (ServiceScheduleDate date : schedule.getDates()) {
                    Long dateId = date.getId();
                    
                    // 獲取批量載入的 configs
                    List<ServiceSchedulePositionConfig> dateConfigs = configsByDateId.get(dateId);
                    
                    // 強制初始化並設置 positionConfigs 集合
                    if (dateConfigs != null && !dateConfigs.isEmpty()) {
                        // 如果 positionConfigs 為 null 或空，直接設置批量載入的數據
                        if (date.getPositionConfigs() == null || date.getPositionConfigs().isEmpty()) {
                            date.setPositionConfigs(dateConfigs);
                        } else {
                            // 如果已存在，使用 Hibernate.initialize() 確保已初始化
                            Hibernate.initialize(date.getPositionConfigs());
                        }
                        
                        // 為每個 config 強制設置 assignments
                        for (ServiceSchedulePositionConfig config : dateConfigs) {
                            Long configId = config.getId();
                            
                            // 獲取批量載入的 assignments
                            List<ServiceScheduleAssignment> configAssignments = assignmentsByConfigId.get(configId);
                            
                            if (configAssignments != null && !configAssignments.isEmpty()) {
                                // 強制設置 assignments，避免後續訪問時觸發懶加載
                                config.setAssignments(configAssignments);
                                
                                // 使用 Hibernate.initialize() 確保集合已初始化
                                Hibernate.initialize(config.getAssignments());
                                
                                // 確保每個 assignment 的 person 已載入（已通過 JOIN FETCH 載入）
                                for (ServiceScheduleAssignment assignment : configAssignments) {
                                    if (assignment.getPerson() != null) {
                                        Hibernate.initialize(assignment.getPerson());
                                    }
                                }
                            } else {
                                // 如果沒有 assignments，確保集合已初始化為空列表
                                if (config.getAssignments() == null) {
                                    config.setAssignments(new ArrayList<>());
                                }
                                Hibernate.initialize(config.getAssignments());
                            }
                            
                            // 確保 position 已初始化（已通過 JOIN FETCH 載入）
                            if (config.getPosition() != null) {
                                Hibernate.initialize(config.getPosition());
                            }
                        }
                    } else {
                        // 如果沒有 configs，確保集合已初始化為空列表
                        if (date.getPositionConfigs() == null) {
                            date.setPositionConfigs(new ArrayList<>());
                        }
                        Hibernate.initialize(date.getPositionConfigs());
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
            scheduleMap.put("year", schedule.getYear());
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
     * 獲取所有安排表（支持分頁和過濾）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Page<Map<String, Object>> getAllSchedulesWithDateRange(Integer year, Pageable pageable) {
        List<ServiceSchedule> allSchedules = repository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 過濾和轉換數據
        for (ServiceSchedule schedule : allSchedules) {
            // 過濾年度
            if (year != null && !schedule.getYear().equals(year)) {
                continue;
            }
            
            Map<String, Object> scheduleMap = new HashMap<>();
            scheduleMap.put("year", schedule.getYear());
            scheduleMap.put("createdAt", schedule.getCreatedAt());
            scheduleMap.put("updatedAt", schedule.getUpdatedAt());
            
            // 計算日期範圍
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
        
        // 手動分頁
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), result.size());
        List<Map<String, Object>> paginatedContent = result.subList(start, end);
        
        return new PageImpl<>(paginatedContent, pageable, result.size());
    }

    /**
     * 更新安排表
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ServiceSchedule updateSchedule(Integer year, List<Map<String, Object>> scheduleData) {
        // 1. 載入現有的服事表
        ServiceSchedule schedule = repository.findById(year)
            .orElseThrow(() -> new RuntimeException("找不到指定年度（" + year + "年）的安排表"));

        // 2. 計算新的年度並檢查
        Integer newYear = calculateYearFromScheduleData(scheduleData);
        if (newYear == null) {
            // 如果無法計算年度，保持原年度不變
            newYear = schedule.getYear();
        }
        
        // 如果年度改變了，需要處理主鍵變更
        if (!newYear.equals(schedule.getYear())) {
            // 檢查新年度是否已有服事表
            if (repository.existsById(newYear)) {
                throw new RuntimeException("該年度（" + newYear + "年）已存在服事表，每個年度只能有一個版本");
            }
            
            // 由於主鍵變更，需要先刪除舊記錄再創建新記錄
            // 保存關聯資料的引用
            List<ServiceScheduleDate> datesToMove = schedule.getDates();
            
            // 刪除舊記錄（會級聯刪除 dates）
            repository.delete(schedule);
            entityManager.flush();
            
            // 創建新記錄
            ServiceSchedule newSchedule = new ServiceSchedule();
            newSchedule.setYear(newYear);
            newSchedule = repository.save(newSchedule);
            
            // 重新關聯 dates（如果有的話，但通常會被級聯刪除，所以需要重新創建）
            schedule = newSchedule;
        }

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

                    // 保存崗位配置和人員分配（支援多人）
                    for (String positionCode : positionCodeSet) {
                        Position position = positionMap.get(positionCode);
                        if (position == null) {
                            continue;
                        }
                        
                        // 使用 IDs 陣列（多人）
                        List<Long> personIds = new ArrayList<>();
                        Object personIdsObj = item.get(positionCode + "Ids");
                        if (personIdsObj instanceof List) {
                            // 處理 ID 陣列
                            @SuppressWarnings("unchecked")
                            List<Object> idsList = (List<Object>) personIdsObj;
                            for (Object idObj : idsList) {
                                if (idObj instanceof Number) {
                                    personIds.add(((Number) idObj).longValue());
                                } else if (idObj instanceof String && !((String) idObj).trim().isEmpty()) {
                                    try {
                                        personIds.add(Long.parseLong((String) idObj));
                                    } catch (NumberFormatException e) {
                                        // 忽略
                                    }
                                }
                            }
                        }
                        
                        // 如果通過 ID 找不到，嘗試使用名稱（向後兼容）
                        if (personIds.isEmpty()) {
                            String personName = (String) item.get(positionCode);
                            if (personName != null && !personName.trim().isEmpty()) {
                                // 如果名稱包含 "/"，嘗試分割
                                if (personName.contains("/")) {
                                    String[] names = personName.split("/");
                                    for (String name : names) {
                                        Person person = personMapByName.get(name.trim());
                                        if (person != null) {
                                            personIds.add(person.getId());
                                        }
                                    }
                                } else {
                                    Person person = personMapByName.get(personName);
                                    if (person != null) {
                                        personIds.add(person.getId());
                                    }
                                }
                            }
                        }
                        
                        if (!personIds.isEmpty()) {
                            // 創建崗位配置
                            ServiceSchedulePositionConfig config = new ServiceSchedulePositionConfig();
                            config.setServiceScheduleDate(scheduleDate);
                            config.setPosition(position);
                            config.setPersonCount(personIds.size());
                            configsToSave.add(config);

                            // 為每個人員創建分配記錄
                            for (int i = 0; i < personIds.size(); i++) {
                                Long personId = personIds.get(i);
                                Person person = personMapById.get(personId);
                                if (person != null) {
                                    ServiceScheduleAssignment assignment = new ServiceScheduleAssignment();
                                    assignment.setServiceSchedulePositionConfig(config);
                                    assignment.setPerson(person);
                                    assignment.setSortOrder(i); // 使用索引作為 sortOrder
                                    assignmentsToSave.add(assignment);
                                }
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
    public void deleteSchedule(Integer year) {
        repository.deleteById(year);
    }

    /**
     * 從 scheduleData 中計算年度
     * 使用最早的日期作為年度依據
     */
    private Integer calculateYearFromScheduleData(List<Map<String, Object>> scheduleData) {
        if (scheduleData == null || scheduleData.isEmpty()) {
            return null;
        }
        
        LocalDate earliestDate = null;
        for (Map<String, Object> item : scheduleData) {
            String dateStr = (String) item.get("date");
            if (dateStr != null && !dateStr.isEmpty()) {
                try {
                    LocalDate date = LocalDate.parse(dateStr);
                    if (earliestDate == null || date.isBefore(earliestDate)) {
                        earliestDate = date;
                    }
                } catch (Exception e) {
                    // 忽略解析錯誤
                }
            }
        }
        
        return earliestDate != null ? earliestDate.getYear() : null;
    }
}

