package com.example.helloworld.entity.invest;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "strength_snapshot_factor")
public class StrengthSnapshotFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private StrengthSnapshot snapshot;

    @Column(name = "factor_code", nullable = false, length = 50)
    private String factorCode;

    @Column(name = "factor_name", nullable = false, length = 100)
    private String factorName;

    @Column(name = "raw_value_text", length = 255)
    private String rawValueText;

    @Column(name = "threshold_text", length = 255)
    private String thresholdText;

    @Column(name = "score_contribution", nullable = false)
    private Integer scoreContribution;

    @Column(name = "explanation_text", nullable = false, length = 1000)
    private String explanationText;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

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

    public StrengthSnapshot getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(StrengthSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public String getFactorCode() {
        return factorCode;
    }

    public void setFactorCode(String factorCode) {
        this.factorCode = factorCode;
    }

    public String getFactorName() {
        return factorName;
    }

    public void setFactorName(String factorName) {
        this.factorName = factorName;
    }

    public String getRawValueText() {
        return rawValueText;
    }

    public void setRawValueText(String rawValueText) {
        this.rawValueText = rawValueText;
    }

    public String getThresholdText() {
        return thresholdText;
    }

    public void setThresholdText(String thresholdText) {
        this.thresholdText = thresholdText;
    }

    public Integer getScoreContribution() {
        return scoreContribution;
    }

    public void setScoreContribution(Integer scoreContribution) {
        this.scoreContribution = scoreContribution;
    }

    public String getExplanationText() {
        return explanationText;
    }

    public void setExplanationText(String explanationText) {
        this.explanationText = explanationText;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
