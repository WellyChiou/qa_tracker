package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.Person;
import com.example.helloworld.entity.church.ServiceSchedule;
import com.example.helloworld.entity.church.ServiceScheduleDate;
import com.example.helloworld.entity.church.ServiceSchedulePositionConfig;
import com.example.helloworld.entity.church.ServiceScheduleAssignment;
import com.example.helloworld.service.church.ServiceScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/church/service-schedules")
@CrossOrigin(origins = "*")
public class ServiceScheduleController {

    @Autowired
    private ServiceScheduleService service;

    /**
     * 保存服事安排表
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> saveSchedule(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            if (name == null || name.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "名稱不能為空");
                return ResponseEntity.badRequest().body(error);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scheduleData = (List<Map<String, Object>>) request.get("scheduleData");

            ServiceSchedule schedule = service.saveSchedule(name, scheduleData);

            Map<String, Object> response = new HashMap<>();
            response.put("id", schedule.getId());
            response.put("name", schedule.getName());
            response.put("createdAt", schedule.getCreatedAt());
            response.put("updatedAt", schedule.getUpdatedAt());
            response.put("message", "安排表保存成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "保存失敗：" + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 獲取所有安排表（包含日期範圍信息）
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllSchedules() {
        List<Map<String, Object>> schedules = service.getAllSchedulesWithDateRange();
        return ResponseEntity.ok(schedules);
    }

    /**
     * 根據 ID 獲取安排表（包含 dates 數據）
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getScheduleById(@PathVariable Long id) {
        return service.getScheduleById(id)
            .map(schedule -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", schedule.getId());
                response.put("name", schedule.getName());
                response.put("createdAt", schedule.getCreatedAt());
                response.put("updatedAt", schedule.getUpdatedAt());
                
                // 載入 dates 數據並轉換為前端格式
                if (schedule.getDates() != null && !schedule.getDates().isEmpty()) {
                    List<Map<String, Object>> scheduleData = new ArrayList<>();
                    LocalDate minDate = null;
                    LocalDate maxDate = null;
                    
                    for (ServiceScheduleDate date : schedule.getDates()) {
                        Map<String, Object> dateItem = new HashMap<>();
                        dateItem.put("date", date.getDate().toString());
                        dateItem.put("dayOfWeek", date.getDayOfWeekLabel());
                        
                        // 格式化日期顯示
                        String formattedDate = formatDate(date.getDate(), date.getDayOfWeekLabel());
                        dateItem.put("formattedDate", formattedDate);
                        
                        // 載入崗位配置和人員分配
                        if (date.getPositionConfigs() != null) {
                            for (ServiceSchedulePositionConfig config : date.getPositionConfigs()) {
                                String positionCode = config.getPosition().getPositionCode();
                                
                                // 獲取該崗位的人員分配
                                if (config.getAssignments() != null && !config.getAssignments().isEmpty()) {
                                    ServiceScheduleAssignment firstAssignment = config.getAssignments().stream()
                                        .sorted(Comparator.comparing(ServiceScheduleAssignment::getSortOrder))
                                        .findFirst()
                                        .orElse(null);
                                    
                                    if (firstAssignment != null && firstAssignment.getPerson() != null) {
                                        Person person = firstAssignment.getPerson();
                                        // 同時返回 ID 和名稱
                                        dateItem.put(positionCode, person.getDisplayName() != null ? 
                                            person.getDisplayName() : person.getPersonName());
                                        dateItem.put(positionCode + "Id", person.getId());
                                    }
                                }
                            }
                        }
                        
                        scheduleData.add(dateItem);
                        
                        // 計算日期範圍
                        if (minDate == null || date.getDate().isBefore(minDate)) {
                            minDate = date.getDate();
                        }
                        if (maxDate == null || date.getDate().isAfter(maxDate)) {
                            maxDate = date.getDate();
                        }
                    }
                    
                    // 按日期排序
                    scheduleData.sort((a, b) -> {
                        LocalDate dateA = LocalDate.parse((String) a.get("date"));
                        LocalDate dateB = LocalDate.parse((String) b.get("date"));
                        return dateA.compareTo(dateB);
                    });
                    
                    response.put("scheduleData", scheduleData);
                    if (minDate != null && maxDate != null) {
                        response.put("startDate", minDate.toString());
                        response.put("endDate", maxDate.toString());
                    }
                } else {
                    response.put("scheduleData", new ArrayList<>());
                }
                
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 格式化日期顯示
     */
    private String formatDate(LocalDate date, String dayOfWeekLabel) {
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        return String.format("%d/%02d/%02d(%s)", year, month, day, dayOfWeekLabel);
    }

    /**
     * 更新安排表
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateSchedule(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            if (name == null || name.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "名稱不能為空");
                return ResponseEntity.badRequest().body(error);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scheduleData = (List<Map<String, Object>>) request.get("scheduleData");

            ServiceSchedule schedule = service.updateSchedule(id, name, scheduleData);

            Map<String, Object> response = new HashMap<>();
            response.put("id", schedule.getId());
            response.put("name", schedule.getName());
            response.put("createdAt", schedule.getCreatedAt());
            response.put("updatedAt", schedule.getUpdatedAt());
            response.put("message", "安排表更新成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "更新失敗：" + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 刪除安排表
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSchedule(@PathVariable Long id) {
        service.deleteSchedule(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "安排表刪除成功");
        return ResponseEntity.ok(response);
    }
}

