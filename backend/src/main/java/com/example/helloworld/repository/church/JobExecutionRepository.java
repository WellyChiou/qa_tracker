package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.JobExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("churchJobExecutionRepository")
public interface JobExecutionRepository extends JpaRepository<JobExecution, Long> {
    List<JobExecution> findByJobIdOrderByCreatedAtDesc(Long jobId);
    
    @Query("SELECT e FROM JobExecution e WHERE e.jobId = :jobId ORDER BY e.createdAt DESC")
    Optional<JobExecution> findLatestByJobId(@Param("jobId") Long jobId);
    
    Optional<JobExecution> findFirstByJobIdOrderByCreatedAtDesc(Long jobId);
}

