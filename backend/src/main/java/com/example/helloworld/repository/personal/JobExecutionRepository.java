package com.example.helloworld.repository.personal;

import com.example.helloworld.entity.personal.JobExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("personalJobExecutionRepository")
public interface JobExecutionRepository extends JpaRepository<JobExecution, Long> {
    List<JobExecution> findByJobIdOrderByCreatedAtDesc(Long jobId);
    Optional<JobExecution> findFirstByJobIdOrderByCreatedAtDesc(Long jobId);
    List<JobExecution> findByJobIdAndStatus(Long jobId, String status);
}
