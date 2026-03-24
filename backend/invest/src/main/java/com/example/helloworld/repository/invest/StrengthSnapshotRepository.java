package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.MarketUniverseTypeCode;
import com.example.helloworld.entity.invest.StrengthLevelCode;
import com.example.helloworld.entity.invest.StrengthSnapshot;
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
public interface StrengthSnapshotRepository extends JpaRepository<StrengthSnapshot, Long> {

    Optional<StrengthSnapshot> findByUserIdAndStockIdAndTradeDateAndUniverseTypeAndWatchScopeId(
        String userId,
        Long stockId,
        LocalDate tradeDate,
        MarketUniverseTypeCode universeType,
        Long watchScopeId
    );

    @Query("SELECT s FROM StrengthSnapshot s " +
        "JOIN FETCH s.stock st " +
        "LEFT JOIN FETCH s.watchlist wl " +
        "WHERE s.id = :id AND s.userId = :userId")
    Optional<StrengthSnapshot> findByIdAndUserId(@Param("id") Long id, @Param("userId") String userId);

    @Query("SELECT s FROM StrengthSnapshot s " +
        "JOIN s.stock st " +
        "LEFT JOIN s.watchlist wl " +
        "WHERE s.userId = :userId " +
        "AND (:tradeDate IS NULL OR s.tradeDate = :tradeDate) " +
        "AND (:level IS NULL OR s.strengthLevel = :level) " +
        "AND (:universeType IS NULL OR s.universeType = :universeType) " +
        "AND (:watchlistId IS NULL OR wl.id = :watchlistId) " +
        "AND (:ticker IS NULL OR :ticker = '' OR LOWER(st.ticker) LIKE LOWER(CONCAT('%', :ticker, '%'))) " +
        "ORDER BY s.tradeDate DESC, s.strengthScore DESC, s.id DESC")
    Page<StrengthSnapshot> findPagedByUser(
        @Param("userId") String userId,
        @Param("tradeDate") LocalDate tradeDate,
        @Param("level") StrengthLevelCode level,
        @Param("universeType") MarketUniverseTypeCode universeType,
        @Param("watchlistId") Long watchlistId,
        @Param("ticker") String ticker,
        Pageable pageable
    );

    @Query("SELECT s FROM StrengthSnapshot s " +
        "JOIN FETCH s.stock st " +
        "LEFT JOIN FETCH s.watchlist wl " +
        "WHERE s.userId = :userId AND st.id = :stockId " +
        "AND (:universeType IS NULL OR s.universeType = :universeType) " +
        "ORDER BY s.tradeDate DESC, s.computedAt DESC")
    List<StrengthSnapshot> findLatestByUserAndStock(
        @Param("userId") String userId,
        @Param("stockId") Long stockId,
        @Param("universeType") MarketUniverseTypeCode universeType
    );
}
