package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findBySettingKey(String settingKey);

    List<SystemSetting> findByCategory(String category);

    List<SystemSetting> findBySettingKeyIn(List<String> settingKeys);

    List<SystemSetting> findBySettingKeyStartingWithOrderBySettingKeyAsc(String prefix);

    @Query("SELECT max(s.updatedAt) FROM SystemSetting s WHERE s.settingKey IN :keys")
    LocalDateTime findMaxUpdatedAtBySettingKeyIn(@Param("keys") List<String> settingKeys);

    boolean existsBySettingKey(String settingKey);

    @Query("SELECT s FROM SystemSetting s ORDER BY s.category, s.settingKey")
    List<SystemSetting> findAllOrderByCategoryAndKey();
}
