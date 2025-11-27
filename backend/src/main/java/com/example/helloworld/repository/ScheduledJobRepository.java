package com.example.helloworld.repository;

import com.example.helloworld.entity.ScheduledJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, Long> {
    List<ScheduledJob> findByEnabledTrue();
    List<ScheduledJob> findByJobNameContainingIgnoreCase(String jobName);
}

