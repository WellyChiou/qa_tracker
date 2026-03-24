package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.SystemScheduledJobConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemScheduledJobConfigRepository extends JpaRepository<SystemScheduledJobConfig, Long> {
    Optional<SystemScheduledJobConfig> findByJobCode(String jobCode);

    List<SystemScheduledJobConfig> findAllByOrderByUpdatedAtDesc();
}
