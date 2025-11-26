package com.example.helloworld.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "firebase_id", length = 128, unique = true)
    private String firebaseId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "member", length = 50, nullable = false)
    private String member;

    @Column(name = "type", length = 20, nullable = false)
    private String type; // 收入、支出

    @Column(name = "main_category", length = 50)
    private String mainCategory;

    @Column(name = "sub_category", length = 100)
    private String subCategory;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 10)
    private String currency = "TWD";

    @Column(name = "exchange_rate", precision = 10, scale = 4)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by_uid", length = 128)
    private String createdByUid;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by_uid", length = 128)
    private String updatedByUid;

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

    public String getFirebaseId() { return firebaseId; }
    public void setFirebaseId(String firebaseId) { this.firebaseId = firebaseId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getMember() { return member; }
    public void setMember(String member) { this.member = member; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMainCategory() { return mainCategory; }
    public void setMainCategory(String mainCategory) { this.mainCategory = mainCategory; }

    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedByUid() { return createdByUid; }
    public void setCreatedByUid(String createdByUid) { this.createdByUid = createdByUid; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getUpdatedByUid() { return updatedByUid; }
    public void setUpdatedByUid(String updatedByUid) { this.updatedByUid = updatedByUid; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

