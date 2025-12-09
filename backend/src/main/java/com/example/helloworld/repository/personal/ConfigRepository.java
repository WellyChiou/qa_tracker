package com.example.helloworld.repository.personal;

import com.example.helloworld.entity.personal.Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfigRepository extends JpaRepository<Config, String> {
    Optional<Config> findByConfigKey(String configKey);
}
