package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.StockPriceDaily;
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
public interface StockPriceDailyRepository extends JpaRepository<StockPriceDaily, Long> {

    @Query("SELECT d FROM StockPriceDaily d JOIN d.stock s WHERE " +
        "(:stockId IS NULL OR s.id = :stockId) AND " +
        "(:ticker IS NULL OR :ticker = '' OR LOWER(s.ticker) LIKE LOWER(CONCAT('%', :ticker, '%'))) AND " +
        "(:tradeDateFrom IS NULL OR d.tradeDate >= :tradeDateFrom) AND " +
        "(:tradeDateTo IS NULL OR d.tradeDate <= :tradeDateTo) " +
        "ORDER BY d.tradeDate DESC, d.id DESC")
    Page<StockPriceDaily> findByFilters(
        @Param("stockId") Long stockId,
        @Param("ticker") String ticker,
        @Param("tradeDateFrom") LocalDate tradeDateFrom,
        @Param("tradeDateTo") LocalDate tradeDateTo,
        Pageable pageable
    );

    @Query("SELECT d FROM StockPriceDaily d JOIN d.stock s WHERE " +
        "(:stockId IS NULL OR s.id = :stockId) AND " +
        "(:ticker IS NULL OR :ticker = '' OR LOWER(s.ticker) LIKE LOWER(CONCAT('%', :ticker, '%'))) AND " +
        "(:tradeDateFrom IS NULL OR d.tradeDate >= :tradeDateFrom) AND " +
        "(:tradeDateTo IS NULL OR d.tradeDate <= :tradeDateTo) " +
        "ORDER BY d.tradeDate DESC, d.id DESC")
    List<StockPriceDaily> findAllByFilters(
        @Param("stockId") Long stockId,
        @Param("ticker") String ticker,
        @Param("tradeDateFrom") LocalDate tradeDateFrom,
        @Param("tradeDateTo") LocalDate tradeDateTo
    );

    @Query(value = """
        SELECT ranked.id,
               ranked.stock_id,
               ranked.trade_date,
               ranked.open_price,
               ranked.high_price,
               ranked.low_price,
               ranked.close_price,
               ranked.volume,
               ranked.change_amount,
               ranked.change_percent,
               ranked.data_source,
               ranked.fetched_at,
               ranked.latency_type,
               ranked.update_batch_id,
               ranked.data_quality,
               ranked.created_at
          FROM (
                SELECT d.*,
                       ROW_NUMBER() OVER (
                           PARTITION BY d.stock_id
                           ORDER BY d.trade_date DESC,
                                    COALESCE(d.fetched_at, d.created_at) DESC,
                                    d.id DESC
                       ) AS rn
                  FROM stock_price_daily d
                  JOIN stock s
                    ON s.id = d.stock_id
                 WHERE (:stockId IS NULL OR d.stock_id = :stockId)
                   AND (:ticker IS NULL OR :ticker = '' OR LOWER(s.ticker) LIKE LOWER(CONCAT('%', :ticker, '%')))
                   AND (:tradeDateFrom IS NULL OR d.trade_date >= :tradeDateFrom)
                   AND (:tradeDateTo IS NULL OR d.trade_date <= :tradeDateTo)
               ) ranked
         WHERE ranked.rn = 1
         ORDER BY ranked.trade_date DESC, ranked.id DESC
        """, nativeQuery = true)
    List<StockPriceDaily> findLatestByFilters(
        @Param("stockId") Long stockId,
        @Param("ticker") String ticker,
        @Param("tradeDateFrom") LocalDate tradeDateFrom,
        @Param("tradeDateTo") LocalDate tradeDateTo
    );

    Optional<StockPriceDaily> findByStockIdAndTradeDate(Long stockId, LocalDate tradeDate);

    Optional<StockPriceDaily> findTopByStockIdOrderByTradeDateDesc(Long stockId);

    List<StockPriceDaily> findTop30ByStockIdOrderByTradeDateDesc(Long stockId);

    List<StockPriceDaily> findTop30ByStockIdAndTradeDateLessThanEqualOrderByTradeDateDesc(Long stockId, LocalDate tradeDate);
}
