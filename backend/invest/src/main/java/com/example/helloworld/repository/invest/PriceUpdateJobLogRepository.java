package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.PriceUpdateJobLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceUpdateJobLogRepository extends JpaRepository<PriceUpdateJobLog, Long> {
}
