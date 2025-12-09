package com.example.helloworld.entity.personal;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_rates")
public class ExchangeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date", nullable = false, unique = true)
    private LocalDate date;

    @Column(name = "usd_rate", precision = 10, scale = 4)
    private BigDecimal usdRate;

    @Column(name = "eur_rate", precision = 10, scale = 4)
    private BigDecimal eurRate;

    @Column(name = "jpy_rate", precision = 10, scale = 4)
    private BigDecimal jpyRate;

    @Column(name = "cny_rate", precision = 10, scale = 4)
    private BigDecimal cnyRate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public BigDecimal getUsdRate() { return usdRate; }
    public void setUsdRate(BigDecimal usdRate) { this.usdRate = usdRate; }

    public BigDecimal getEurRate() { return eurRate; }
    public void setEurRate(BigDecimal eurRate) { this.eurRate = eurRate; }

    public BigDecimal getJpyRate() { return jpyRate; }
    public void setJpyRate(BigDecimal jpyRate) { this.jpyRate = jpyRate; }

    public BigDecimal getCnyRate() { return cnyRate; }
    public void setCnyRate(BigDecimal cnyRate) { this.cnyRate = cnyRate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
