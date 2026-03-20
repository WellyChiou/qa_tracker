package com.example.helloworld.entity.invest;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "opportunity_signal")
public class OpportunitySignal {

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

    @Column(name = "signal_key", nullable = false, length = 120)
    private String signalKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false, length = 50)
    private OpportunitySignalTypeCode signalType;

    @Column(name = "signal_score", nullable = false)
    private Integer signalScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_code", nullable = false, length = 60)
    private StrengthRecommendationCode recommendationCode;

    @Column(name = "condition_text", nullable = false, length = 1000)
    private String conditionText;

    @Column(name = "summary_text", nullable = false, length = 1000)
    private String summaryText;

    @Column(name = "disclaimer_text", nullable = false, length = 1000)
    private String disclaimerText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OpportunitySignalStatusCode status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_snapshot_id")
    private StrengthSnapshot sourceSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public String getSignalKey() {
        return signalKey;
    }

    public void setSignalKey(String signalKey) {
        this.signalKey = signalKey;
    }

    public OpportunitySignalTypeCode getSignalType() {
        return signalType;
    }

    public void setSignalType(OpportunitySignalTypeCode signalType) {
        this.signalType = signalType;
    }

    public Integer getSignalScore() {
        return signalScore;
    }

    public void setSignalScore(Integer signalScore) {
        this.signalScore = signalScore;
    }

    public StrengthRecommendationCode getRecommendationCode() {
        return recommendationCode;
    }

    public void setRecommendationCode(StrengthRecommendationCode recommendationCode) {
        this.recommendationCode = recommendationCode;
    }

    public String getConditionText() {
        return conditionText;
    }

    public void setConditionText(String conditionText) {
        this.conditionText = conditionText;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public String getDisclaimerText() {
        return disclaimerText;
    }

    public void setDisclaimerText(String disclaimerText) {
        this.disclaimerText = disclaimerText;
    }

    public OpportunitySignalStatusCode getStatus() {
        return status;
    }

    public void setStatus(OpportunitySignalStatusCode status) {
        this.status = status;
    }

    public StrengthSnapshot getSourceSnapshot() {
        return sourceSnapshot;
    }

    public void setSourceSnapshot(StrengthSnapshot sourceSnapshot) {
        this.sourceSnapshot = sourceSnapshot;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
