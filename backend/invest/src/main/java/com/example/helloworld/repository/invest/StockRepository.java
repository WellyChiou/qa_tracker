package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    @Query("SELECT s FROM Stock s WHERE " +
        "(:market IS NULL OR :market = '' OR LOWER(s.market) = LOWER(:market)) AND " +
        "(:ticker IS NULL OR :ticker = '' OR LOWER(s.ticker) LIKE LOWER(CONCAT('%', :ticker, '%'))) AND " +
        "(:name IS NULL OR :name = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
        "(:isActive IS NULL OR s.isActive = :isActive) " +
        "ORDER BY s.updatedAt DESC, s.id DESC")
    Page<Stock> findByFilters(
        @Param("market") String market,
        @Param("ticker") String ticker,
        @Param("name") String name,
        @Param("isActive") Boolean isActive,
        Pageable pageable
    );

    @Query("SELECT s FROM Stock s WHERE (:isActive IS NULL OR s.isActive = :isActive) ORDER BY s.ticker ASC")
    List<Stock> findAllByIsActiveFilter(@Param("isActive") Boolean isActive);

    List<Stock> findByIsActiveTrueOrderByTickerAsc();

    Optional<Stock> findByMarketIgnoreCaseAndTickerIgnoreCase(String market, String ticker);

    boolean existsByMarketIgnoreCaseAndTickerIgnoreCase(String market, String ticker);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Stock s WHERE LOWER(s.market) = LOWER(:market) AND LOWER(s.ticker) = LOWER(:ticker) AND s.id <> :id")
    boolean existsByMarketAndTickerExcludingId(
        @Param("market") String market,
        @Param("ticker") String ticker,
        @Param("id") Long id
    );
}
