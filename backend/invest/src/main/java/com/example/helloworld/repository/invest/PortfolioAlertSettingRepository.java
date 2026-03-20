package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.PortfolioAlertSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioAlertSettingRepository extends JpaRepository<PortfolioAlertSetting, Long> {

    Optional<PortfolioAlertSetting> findByPortfolioId(Long portfolioId);

    Optional<PortfolioAlertSetting> findByPortfolioIdAndPortfolioUserId(Long portfolioId, String userId);

    @Query("SELECT s FROM PortfolioAlertSetting s " +
        "JOIN FETCH s.portfolio p " +
        "JOIN FETCH p.stock st " +
        "WHERE s.enabled = true AND p.isActive = true AND p.userId = :userId " +
        "ORDER BY p.id ASC")
    List<PortfolioAlertSetting> findEnabledByUserId(@Param("userId") String userId);

    @Query("SELECT s FROM PortfolioAlertSetting s " +
        "JOIN FETCH s.portfolio p " +
        "JOIN FETCH p.stock st " +
        "WHERE s.enabled = true AND p.isActive = true " +
        "ORDER BY p.userId ASC, p.id ASC")
    List<PortfolioAlertSetting> findAllEnabledForPolling();
}
