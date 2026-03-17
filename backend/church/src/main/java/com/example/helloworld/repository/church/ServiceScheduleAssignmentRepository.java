package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.ServiceScheduleAssignment;
import com.example.helloworld.entity.church.ServiceSchedulePositionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceScheduleAssignmentRepository extends JpaRepository<ServiceScheduleAssignment, Long> {
    List<ServiceScheduleAssignment> findByServiceSchedulePositionConfig(ServiceSchedulePositionConfig config);
    void deleteByServiceSchedulePositionConfig(ServiceSchedulePositionConfig config);
}

