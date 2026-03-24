package com.example.helloworld.entity.invest;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "portfolio_daily_snapshot",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_portfolio_daily_snapshot_portfolio_trade_date", columnNames = {"portfolio_id", "trade_date"})
    }
)
public class PortfolioDailySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "close_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal closePrice;

    @Column(name = "market_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal marketValue;

    @Column(name = "unrealized_profit_loss", nullable = false, precision = 18, scale = 2)
    private BigDecimal unrealizedProfitLoss;

    @Column(name = "unrealized_profit_loss_percent", nullable = false, precision = 9, scale = 2)
    private BigDecimal unrealizedProfitLossPercent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(BigDecimal closePrice) {
        this.closePrice = closePrice;
    }

    public BigDecimal getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(BigDecimal marketValue) {
        this.marketValue = marketValue;
    }

    public BigDecimal getUnrealizedProfitLoss() {
        return unrealizedProfitLoss;
    }

    public void setUnrealizedProfitLoss(BigDecimal unrealizedProfitLoss) {
        this.unrealizedProfitLoss = unrealizedProfitLoss;
    }

    public BigDecimal getUnrealizedProfitLossPercent() {
        return unrealizedProfitLossPercent;
    }

    public void setUnrealizedProfitLossPercent(BigDecimal unrealizedProfitLossPercent) {
        this.unrealizedProfitLossPercent = unrealizedProfitLossPercent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
