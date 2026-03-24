package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.PortfolioDailySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PortfolioDailySnapshotRepository extends JpaRepository<PortfolioDailySnapshot, Long> {
    Optional<PortfolioDailySnapshot> findByPortfolioIdAndTradeDate(Long portfolioId, LocalDate tradeDate);
}
