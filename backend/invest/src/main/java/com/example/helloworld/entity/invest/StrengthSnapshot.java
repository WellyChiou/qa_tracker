package com.example.helloworld.entity.invest;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "strength_snapshot")
public class StrengthSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "universe_type", nullable = false, length = 20)
    private MarketUniverseTypeCode universeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watchlist_id")
    private Watchlist watchlist;

    @Column(name = "watch_scope_id", nullable = false)
    private Long watchScopeId = 0L;

    @Column(name = "price_close", nullable = false, precision = 18, scale = 4)
    private BigDecimal priceClose;

    @Column(name = "price_trade_date", nullable = false)
    private LocalDate priceTradeDate;

    @Column(name = "strength_score")
    private Integer strengthScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "strength_level", length = 20)
    private StrengthLevelCode strengthLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_code", length = 60)
    private StrengthRecommendationCode recommendationCode;

    @Column(name = "summary_text", length = 1000)
    private String summaryText;

    @Column(name = "data_quality", nullable = false, length = 20)
    private String dataQuality;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    public MarketUniverseTypeCode getUniverseType() {
        return universeType;
    }

    public void setUniverseType(MarketUniverseTypeCode universeType) {
        this.universeType = universeType;
    }

    public Watchlist getWatchlist() {
        return watchlist;
    }

    public void setWatchlist(Watchlist watchlist) {
        this.watchlist = watchlist;
    }

    public Long getWatchScopeId() {
        return watchScopeId;
    }

    public void setWatchScopeId(Long watchScopeId) {
        this.watchScopeId = watchScopeId;
    }

    public BigDecimal getPriceClose() {
        return priceClose;
    }

    public void setPriceClose(BigDecimal priceClose) {
        this.priceClose = priceClose;
    }

    public LocalDate getPriceTradeDate() {
        return priceTradeDate;
    }

    public void setPriceTradeDate(LocalDate priceTradeDate) {
        this.priceTradeDate = priceTradeDate;
    }

    public Integer getStrengthScore() {
        return strengthScore;
    }

    public void setStrengthScore(Integer strengthScore) {
        this.strengthScore = strengthScore;
    }

    public StrengthLevelCode getStrengthLevel() {
        return strengthLevel;
    }

    public void setStrengthLevel(StrengthLevelCode strengthLevel) {
        this.strengthLevel = strengthLevel;
    }

    public StrengthRecommendationCode getRecommendationCode() {
        return recommendationCode;
    }

    public void setRecommendationCode(StrengthRecommendationCode recommendationCode) {
        this.recommendationCode = recommendationCode;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public String getDataQuality() {
        return dataQuality;
    }

    public void setDataQuality(String dataQuality) {
        this.dataQuality = dataQuality;
    }

    public LocalDateTime getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(LocalDateTime computedAt) {
        this.computedAt = computedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
