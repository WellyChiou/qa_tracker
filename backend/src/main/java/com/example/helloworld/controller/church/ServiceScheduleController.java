package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.ServiceSchedule;
import com.example.helloworld.service.church.ServiceScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/church/service-schedules")
@CrossOrigin(origins = "*")
public class ServiceScheduleController {

    @Autowired
    private ServiceScheduleService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 保存服事安排表
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> saveSchedule(@RequestBody Map<String, Object> request) {
        try {
            LocalDate scheduleDate = LocalDate.parse((String) request.get("scheduleDate"));
            LocalDate startDate = LocalDate.parse((String) request.get("startDate"));
            LocalDate endDate = LocalDate.parse((String) request.get("endDate"));
            Object scheduleData = request.get("scheduleData");
            Object positionConfig = request.get("positionConfig");

            ServiceSchedule schedule = service.saveSchedule(
                scheduleDate, startDate, endDate, scheduleData, positionConfig
            );

            Map<String, Object> response = new HashMap<>();
            response.put("id", schedule.getId());
            response.put("scheduleDate", schedule.getScheduleDate().toString());
            response.put("version", schedule.getVersion());
            response.put("startDate", schedule.getStartDate().toString());
            response.put("endDate", schedule.getEndDate().toString());
            response.put("message", "安排表保存成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "保存失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 獲取所有安排表
     */
    @GetMapping
    public ResponseEntity<List<ServiceSchedule>> getAllSchedules() {
        List<ServiceSchedule> schedules = service.getAllSchedules();
        return ResponseEntity.ok(schedules);
    }

    /**
     * 根據 ID 獲取安排表
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getScheduleById(@PathVariable Long id) {
        return service.getScheduleById(id)
            .map(schedule -> {
                try {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", schedule.getId());
                    response.put("scheduleDate", schedule.getScheduleDate().toString());
                    response.put("version", schedule.getVersion());
                    response.put("startDate", schedule.getStartDate().toString());
                    response.put("endDate", schedule.getEndDate().toString());
                    response.put("scheduleData", objectMapper.readValue(schedule.getScheduleData(), Object.class));
                    if (schedule.getPositionConfig() != null) {
                        response.put("positionConfig", objectMapper.readValue(schedule.getPositionConfig(), Object.class));
                    }
                    response.put("createdAt", schedule.getCreatedAt());
                    response.put("updatedAt", schedule.getUpdatedAt());
                    return ResponseEntity.ok(response);
                } catch (Exception e) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "解析數據失敗：" + e.getMessage());
                    return ResponseEntity.badRequest().body(error);
                }
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根據日期獲取所有版本
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<List<ServiceSchedule>> getSchedulesByDate(@PathVariable String date) {
        LocalDate scheduleDate = LocalDate.parse(date);
        List<ServiceSchedule> schedules = service.getSchedulesByDate(scheduleDate);
        return ResponseEntity.ok(schedules);
    }

    /**
     * 根據日期範圍獲取安排表
     */
    @GetMapping("/range")
    public ResponseEntity<List<ServiceSchedule>> getSchedulesInRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        List<ServiceSchedule> schedules = service.getSchedulesInDateRange(start, end);
        return ResponseEntity.ok(schedules);
    }

    /**
     * 更新安排表
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateSchedule(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            LocalDate startDate = LocalDate.parse((String) request.get("startDate"));
            LocalDate endDate = LocalDate.parse((String) request.get("endDate"));
            Object scheduleData = request.get("scheduleData");
            Object positionConfig = request.get("positionConfig");

            ServiceSchedule schedule = service.updateSchedule(id, startDate, endDate, scheduleData, positionConfig);

            Map<String, Object> response = new HashMap<>();
            response.put("id", schedule.getId());
            response.put("scheduleDate", schedule.getScheduleDate().toString());
            response.put("version", schedule.getVersion());
            response.put("startDate", schedule.getStartDate().toString());
            response.put("endDate", schedule.getEndDate().toString());
            response.put("message", "安排表更新成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "更新失敗：" + e.getMessage());
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

