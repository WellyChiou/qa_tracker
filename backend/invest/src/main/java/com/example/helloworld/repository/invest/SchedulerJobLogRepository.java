package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.SchedulerJobLog;
import com.example.helloworld.entity.invest.SchedulerJobStatusCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface SchedulerJobLogRepository extends JpaRepository<SchedulerJobLog, Long> {

    @Query("SELECT l FROM SchedulerJobLog l WHERE " +
        "(:jobName IS NULL OR :jobName = '' OR l.jobName = :jobName) AND " +
        "(:runDateFrom IS NULL OR l.runDate >= :runDateFrom) AND " +
        "(:runDateTo IS NULL OR l.runDate <= :runDateTo) AND " +
        "(:status IS NULL OR l.status = :status) " +
        "ORDER BY l.startedAt DESC, l.id DESC")
    Page<SchedulerJobLog> findByFilters(
        @Param("jobName") String jobName,
        @Param("runDateFrom") LocalDate runDateFrom,
        @Param("runDateTo") LocalDate runDateTo,
        @Param("status") SchedulerJobStatusCode status,
        Pageable pageable
    );
}
