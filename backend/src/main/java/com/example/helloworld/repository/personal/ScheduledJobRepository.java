package com.example.helloworld.repository.personal;

import com.example.helloworld.entity.personal.ScheduledJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("personalScheduledJobRepository")
public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, Long> {
    List<ScheduledJob> findByEnabledTrue();
    List<ScheduledJob> findByJobNameContainingIgnoreCase(String jobName);
    Optional<ScheduledJob> findByJobClass(String jobClass);
}
