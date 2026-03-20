package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.OpportunitySignal;
import com.example.helloworld.entity.invest.OpportunitySignalStatusCode;
import com.example.helloworld.entity.invest.OpportunitySignalTypeCode;
import com.example.helloworld.entity.invest.StrengthRecommendationCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OpportunitySignalRepository extends JpaRepository<OpportunitySignal, Long> {

    Optional<OpportunitySignal> findByUserIdAndStockIdAndSignalKey(String userId, Long stockId, String signalKey);

    @Query("SELECT o FROM OpportunitySignal o " +
        "JOIN FETCH o.stock st " +
        "LEFT JOIN FETCH o.sourceSnapshot ss " +
        "WHERE o.id = :id AND o.userId = :userId")
    Optional<OpportunitySignal> findByIdAndUserId(@Param("id") Long id, @Param("userId") String userId);

    @Query("SELECT o FROM OpportunitySignal o " +
        "JOIN o.stock st " +
        "WHERE o.userId = :userId " +
        "AND (:tradeDate IS NULL OR o.tradeDate = :tradeDate) " +
        "AND (:status IS NULL OR o.status = :status) " +
        "AND (:recommendation IS NULL OR o.recommendationCode = :recommendation) " +
        "AND (:signalType IS NULL OR o.signalType = :signalType) " +
        "AND (:ticker IS NULL OR :ticker = '' OR LOWER(st.ticker) LIKE LOWER(CONCAT('%', :ticker, '%'))) " +
        "ORDER BY o.tradeDate DESC, o.signalScore DESC, o.updatedAt DESC")
    Page<OpportunitySignal> findPagedByUser(
        @Param("userId") String userId,
        @Param("tradeDate") LocalDate tradeDate,
        @Param("status") OpportunitySignalStatusCode status,
        @Param("recommendation") StrengthRecommendationCode recommendation,
        @Param("signalType") OpportunitySignalTypeCode signalType,
        @Param("ticker") String ticker,
        Pageable pageable
    );

    @Query("SELECT o FROM OpportunitySignal o " +
        "WHERE o.userId = :userId AND o.stock.id = :stockId AND o.status = :status")
    List<OpportunitySignal> findByUserAndStockAndStatus(
        @Param("userId") String userId,
        @Param("stockId") Long stockId,
        @Param("status") OpportunitySignalStatusCode status
    );

    List<OpportunitySignal> findByUserIdAndStatus(String userId, OpportunitySignalStatusCode status);

    @Query("SELECT o FROM OpportunitySignal o " +
        "JOIN FETCH o.stock st " +
        "WHERE o.userId = :userId AND st.id = :stockId " +
        "ORDER BY o.tradeDate DESC, o.updatedAt DESC")
    List<OpportunitySignal> findLatestByUserAndStock(@Param("userId") String userId, @Param("stockId") Long stockId);
}
