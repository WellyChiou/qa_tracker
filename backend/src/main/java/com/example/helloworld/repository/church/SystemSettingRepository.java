package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {
    Optional<SystemSetting> findBySettingKey(String settingKey);
    
    List<SystemSetting> findByCategory(String category);
    
    @Query("SELECT s FROM SystemSetting s ORDER BY s.category, s.settingKey")
    List<SystemSetting> findAllOrderByCategoryAndKey();
    
    boolean existsBySettingKey(String settingKey);
}

