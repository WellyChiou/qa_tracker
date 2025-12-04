package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.ServiceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceScheduleRepository extends JpaRepository<ServiceSchedule, Long> {
    
    // 根據日期和版本查找
    Optional<ServiceSchedule> findByScheduleDateAndVersion(LocalDate scheduleDate, Integer version);
    
    // 根據日期查找所有版本
    List<ServiceSchedule> findByScheduleDateOrderByVersionDesc(LocalDate scheduleDate);
    
    // 根據日期範圍查找
    List<ServiceSchedule> findByStartDateBetweenOrEndDateBetween(
        LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2
    );
    
    // 查找指定日期範圍內的所有安排
    @Query("SELECT s FROM ServiceSchedule s WHERE s.startDate <= :endDate AND s.endDate >= :startDate ORDER BY s.scheduleDate DESC, s.version DESC")
    List<ServiceSchedule> findSchedulesInDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // 查找指定日期的最新版本號
    @Query("SELECT COALESCE(MAX(s.version), 0) FROM ServiceSchedule s WHERE s.scheduleDate = :scheduleDate")
    Integer findMaxVersionByScheduleDate(@Param("scheduleDate") LocalDate scheduleDate);
    
    // 查找所有安排，按日期和版本排序
    List<ServiceSchedule> findAllByOrderByScheduleDateDescVersionDesc();
}

