package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.ServiceSchedule;
import com.example.helloworld.repository.church.ServiceScheduleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ServiceScheduleService {

    @Autowired
    private ServiceScheduleRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 保存服事安排表
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ServiceSchedule saveSchedule(LocalDate scheduleDate, LocalDate startDate, LocalDate endDate,
                                       Object scheduleData, Object positionConfig) {
        // 查找該日期的最新版本號
        Integer maxVersion = repository.findMaxVersionByScheduleDate(scheduleDate);
        Integer newVersion = (maxVersion != null ? maxVersion : 0) + 1; // Handle null for first entry

        ServiceSchedule schedule = new ServiceSchedule();
        schedule.setScheduleDate(scheduleDate);
        schedule.setVersion(newVersion);
        schedule.setStartDate(startDate);
        schedule.setEndDate(endDate);

        try {
            schedule.setScheduleData(objectMapper.writeValueAsString(scheduleData));
            if (positionConfig != null) {
                schedule.setPositionConfig(objectMapper.writeValueAsString(positionConfig));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("無法序列化安排表數據", e);
        }

        return repository.save(schedule);
    }

    /**
     * 根據 ID 獲取安排表
     */
    public Optional<ServiceSchedule> getScheduleById(Long id) {
        return repository.findById(id);
    }

    /**
     * 根據日期和版本獲取安排表
     */
    public Optional<ServiceSchedule> getScheduleByDateAndVersion(LocalDate scheduleDate, Integer version) {
        return repository.findByScheduleDateAndVersion(scheduleDate, version);
    }

    /**
     * 根據日期獲取所有版本
     */
    public List<ServiceSchedule> getSchedulesByDate(LocalDate scheduleDate) {
        return repository.findByScheduleDateOrderByVersionDesc(scheduleDate);
    }

    /**
     * 獲取日期範圍內的所有安排
     */
    public List<ServiceSchedule> getSchedulesInDateRange(LocalDate startDate, LocalDate endDate) {
        return repository.findSchedulesInDateRange(startDate, endDate);
    }

    /**
     * 獲取所有安排
     */
    public List<ServiceSchedule> getAllSchedules() {
        return repository.findAllByOrderByScheduleDateDescVersionDesc();
    }

    /**
     * 更新安排表
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ServiceSchedule updateSchedule(Long id, LocalDate startDate, LocalDate endDate,
                                         Object scheduleData, Object positionConfig) {
        ServiceSchedule schedule = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("找不到指定的安排表"));

        schedule.setStartDate(startDate);
        schedule.setEndDate(endDate);

        try {
            schedule.setScheduleData(objectMapper.writeValueAsString(scheduleData));
            if (positionConfig != null) {
                schedule.setPositionConfig(objectMapper.writeValueAsString(positionConfig));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("無法序列化安排表數據", e);
        }

        return repository.save(schedule);
    }

    /**
     * 刪除安排表
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public void deleteSchedule(Long id) {
        repository.deleteById(id);
    }
}

