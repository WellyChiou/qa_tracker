package com.example.helloworld.entity.invest;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_risk_reason")
public class PortfolioRiskReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_result_id", nullable = false)
    private PortfolioRiskResult riskResult;

    @Column(name = "rule_code", nullable = false, length = 60)
    private String ruleCode;

    @Column(name = "reason_title", nullable = false, length = 200)
    private String reasonTitle;

    @Column(name = "reason_detail", nullable = false, length = 1000)
    private String reasonDetail;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "score_impact", nullable = false)
    private Integer scoreImpact;

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

    public PortfolioRiskResult getRiskResult() {
        return riskResult;
    }

    public void setRiskResult(PortfolioRiskResult riskResult) {
        this.riskResult = riskResult;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getReasonTitle() {
        return reasonTitle;
    }

    public void setReasonTitle(String reasonTitle) {
        this.reasonTitle = reasonTitle;
    }

    public String getReasonDetail() {
        return reasonDetail;
    }

    public void setReasonDetail(String reasonDetail) {
        this.reasonDetail = reasonDetail;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Integer getScoreImpact() {
        return scoreImpact;
    }

    public void setScoreImpact(Integer scoreImpact) {
        this.scoreImpact = scoreImpact;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
