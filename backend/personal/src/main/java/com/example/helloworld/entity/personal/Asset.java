package com.example.helloworld.entity.personal;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "assets")
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "firebase_id", length = 128, unique = true)
    private String firebaseId;

    @Column(name = "stock_code", length = 50)
    private String stockCode;

    @Column(name = "asset_type", length = 50)
    private String assetType;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "currency", length = 10)
    private String currency = "TWD";

    @Column(name = "quantity", precision = 15, scale = 4)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "cost", precision = 15, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(name = "unit_price", precision = 15, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "current_price", precision = 15, scale = 4)
    private BigDecimal currentPrice = BigDecimal.ZERO;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "member", length = 50)
    private String member;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "order_index")
    private Integer orderIndex = 0;

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

    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }

    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

    public String getMember() { return member; }
    public void setMember(String member) { this.member = member; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public String getCreatedByUid() { return createdByUid; }
    public void setCreatedByUid(String createdByUid) { this.createdByUid = createdByUid; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getUpdatedByUid() { return updatedByUid; }
    public void setUpdatedByUid(String updatedByUid) { this.updatedByUid = updatedByUid; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
