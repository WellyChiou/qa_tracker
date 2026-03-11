package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.ScheduledJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("churchScheduledJobRepository")
public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, Long> {
    List<ScheduledJob> findByEnabledTrue();
    List<ScheduledJob> findByJobNameContainingIgnoreCase(String jobName);
    Optional<ScheduledJob> findByJobClass(String jobClass);
}

