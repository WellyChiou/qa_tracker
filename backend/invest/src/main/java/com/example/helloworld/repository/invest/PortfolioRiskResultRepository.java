package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.PortfolioRiskResult;
import com.example.helloworld.entity.invest.RecommendationCode;
import com.example.helloworld.entity.invest.RiskLevelCode;
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
public interface PortfolioRiskResultRepository extends JpaRepository<PortfolioRiskResult, Long> {

    Optional<PortfolioRiskResult> findByPortfolioIdAndTradeDate(Long portfolioId, LocalDate tradeDate);

    Optional<PortfolioRiskResult> findByIdAndPortfolioUserId(Long id, String userId);

    Optional<PortfolioRiskResult> findTopByPortfolioIdOrderByTradeDateDescCreatedAtDesc(Long portfolioId);

    Optional<PortfolioRiskResult> findTopByPortfolioIdAndPortfolioUserIdOrderByTradeDateDescCreatedAtDesc(Long portfolioId, String userId);

    @Query("SELECT r FROM PortfolioRiskResult r JOIN r.portfolio p JOIN p.stock s WHERE " +
        "p.userId = :userId AND " +
        "(:tradeDate IS NULL OR r.tradeDate = :tradeDate) AND " +
        "(:riskLevel IS NULL OR r.riskLevel = :riskLevel) AND " +
        "(:recommendation IS NULL OR r.recommendation = :recommendation) " +
        "ORDER BY r.tradeDate DESC, r.id DESC")
    Page<PortfolioRiskResult> findByUserAndFilters(
        @Param("userId") String userId,
        @Param("tradeDate") LocalDate tradeDate,
        @Param("riskLevel") RiskLevelCode riskLevel,
        @Param("recommendation") RecommendationCode recommendation,
        Pageable pageable
    );

    @Query("SELECT r FROM PortfolioRiskResult r WHERE " +
        "r.portfolio.id IN :portfolioIds AND " +
        "r.tradeDate = (" +
        "   SELECT MAX(r2.tradeDate) FROM PortfolioRiskResult r2 WHERE r2.portfolio.id = r.portfolio.id" +
        ")")
    List<PortfolioRiskResult> findLatestByPortfolioIds(@Param("portfolioIds") List<Long> portfolioIds);
}
