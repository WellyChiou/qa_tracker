package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.PriceUpdateJobLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PriceUpdateJobLogRepository extends JpaRepository<PriceUpdateJobLog, Long> {
    Page<PriceUpdateJobLog> findByJobNameOrderByStartedAtDesc(String jobName, Pageable pageable);
    Optional<PriceUpdateJobLog> findTopByJobNameOrderByStartedAtDesc(String jobName);
}
