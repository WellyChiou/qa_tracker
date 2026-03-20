package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.AlertTriggerTypeCode;
import com.example.helloworld.entity.invest.PortfolioAlertEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioAlertEventRepository extends JpaRepository<PortfolioAlertEvent, Long> {

    @Query("SELECT e FROM PortfolioAlertEvent e " +
        "JOIN e.portfolio p " +
        "JOIN p.stock s " +
        "WHERE p.userId = :userId AND " +
        "(:portfolioId IS NULL OR p.id = :portfolioId) AND " +
        "(:triggerType IS NULL OR e.triggerType = :triggerType) AND " +
        "(:triggeredFrom IS NULL OR e.triggeredAt >= :triggeredFrom) AND " +
        "(:triggeredTo IS NULL OR e.triggeredAt <= :triggeredTo) " +
        "ORDER BY e.triggeredAt DESC, e.id DESC")
    Page<PortfolioAlertEvent> findByUserAndFilters(
        @Param("userId") String userId,
        @Param("portfolioId") Long portfolioId,
        @Param("triggerType") AlertTriggerTypeCode triggerType,
        @Param("triggeredFrom") LocalDateTime triggeredFrom,
        @Param("triggeredTo") LocalDateTime triggeredTo,
        Pageable pageable
    );

    @Query("SELECT e FROM PortfolioAlertEvent e " +
        "JOIN e.portfolio p " +
        "WHERE p.userId = :userId " +
        "ORDER BY e.triggeredAt DESC, e.id DESC")
    List<PortfolioAlertEvent> findLatestByUserId(@Param("userId") String userId, Pageable pageable);

    boolean existsByPortfolioIdAndTriggerTypeAndTriggeredAtBetween(
        Long portfolioId,
        AlertTriggerTypeCode triggerType,
        LocalDateTime start,
        LocalDateTime end
    );

    Optional<PortfolioAlertEvent> findTopByPortfolioIdAndTriggerTypeOrderByTriggeredAtDesc(
        Long portfolioId,
        AlertTriggerTypeCode triggerType
    );

    @Query("SELECT COUNT(e) FROM PortfolioAlertEvent e JOIN e.portfolio p " +
        "WHERE p.userId = :userId AND e.triggeredAt >= :startAt AND e.triggeredAt < :endAt")
    Long countByUserIdAndTriggeredAtBetween(
        @Param("userId") String userId,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt
    );

    @Query("SELECT COUNT(e) FROM PortfolioAlertEvent e JOIN e.portfolio p " +
        "WHERE p.userId = :userId AND e.triggerType IN :triggerTypes AND e.triggeredAt >= :startAt AND e.triggeredAt < :endAt")
    Long countByUserIdAndTriggerTypesAndTriggeredAtBetween(
        @Param("userId") String userId,
        @Param("triggerTypes") List<AlertTriggerTypeCode> triggerTypes,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt
    );

    @Query("SELECT MAX(e.triggeredAt) FROM PortfolioAlertEvent e JOIN e.portfolio p WHERE p.userId = :userId")
    LocalDateTime findLatestTriggeredAtByUserId(@Param("userId") String userId);
}
