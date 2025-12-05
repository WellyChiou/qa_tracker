package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.ServiceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceScheduleRepository extends JpaRepository<ServiceSchedule, Long> {
    // 查找所有安排，按建立時間排序
    List<ServiceSchedule> findAllByOrderByCreatedAtDesc();
}

