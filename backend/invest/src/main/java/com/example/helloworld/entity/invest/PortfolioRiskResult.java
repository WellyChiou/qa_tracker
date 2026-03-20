package com.example.helloworld.entity.invest;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "portfolio_risk_result",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_portfolio_risk_result_portfolio_trade_date", columnNames = {"portfolio_id", "trade_date"})
    }
)
public class PortfolioRiskResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevelCode riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation", nullable = false, length = 30)
    private RecommendationCode recommendation;

    @Column(name = "summary_text", nullable = false, length = 500)
    private String summaryText;

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

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public RiskLevelCode getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevelCode riskLevel) {
        this.riskLevel = riskLevel;
    }

    public RecommendationCode getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(RecommendationCode recommendation) {
        this.recommendation = recommendation;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
