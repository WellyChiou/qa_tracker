package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.PositionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PositionConfigRepository extends JpaRepository<PositionConfig, Long> {
    Optional<PositionConfig> findByConfigName(String configName);
    Optional<PositionConfig> findByIsDefaultTrue();
}

