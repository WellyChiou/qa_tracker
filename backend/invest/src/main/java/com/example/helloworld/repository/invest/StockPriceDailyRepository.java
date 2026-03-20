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

    Optional<StockPriceDaily> findByStockIdAndTradeDate(Long stockId, LocalDate tradeDate);

    Optional<StockPriceDaily> findTopByStockIdOrderByTradeDateDesc(Long stockId);

    List<StockPriceDaily> findTop30ByStockIdOrderByTradeDateDesc(Long stockId);

    List<StockPriceDaily> findTop30ByStockIdAndTradeDateLessThanEqualOrderByTradeDateDesc(Long stockId, LocalDate tradeDate);
}
