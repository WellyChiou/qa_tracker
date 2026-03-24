package com.example.helloworld.dto.invest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PortfolioResponseDto {
    private Long id;
    private String userId;
    private Long stockId;
    private String market;
    private String ticker;
    private String stockName;
    private BigDecimal avgCost;
    private BigDecimal quantity;
    private BigDecimal totalCost;
    private String note;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal currentPrice;
    private String assetCurrency;
    private String baseCurrency;
    private BigDecimal fxRateToBase;
    private LocalDate currentPriceTradeDate;
    private String priceSource;
    private String currentPriceDataQuality;
    private BigDecimal marketValue;
    private BigDecimal unrealizedProfitLoss;
    private BigDecimal unrealizedProfitLossPercent;
    private Integer latestRiskScore;
    private String latestRiskLevel;
    private String latestRecommendation;
    private List<StockPriceDailyResponseDto> priceHistory;

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

    public Long getStockId() {
        return stockId;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public BigDecimal getAvgCost() {
        return avgCost;
    }

    public void setAvgCost(BigDecimal avgCost) {
        this.avgCost = avgCost;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getAssetCurrency() {
        return assetCurrency;
    }

    public void setAssetCurrency(String assetCurrency) {
        this.assetCurrency = assetCurrency;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public BigDecimal getFxRateToBase() {
        return fxRateToBase;
    }

    public void setFxRateToBase(BigDecimal fxRateToBase) {
        this.fxRateToBase = fxRateToBase;
    }

    public LocalDate getCurrentPriceTradeDate() {
        return currentPriceTradeDate;
    }

    public void setCurrentPriceTradeDate(LocalDate currentPriceTradeDate) {
        this.currentPriceTradeDate = currentPriceTradeDate;
    }

    public String getPriceSource() {
        return priceSource;
    }

    public void setPriceSource(String priceSource) {
        this.priceSource = priceSource;
    }

    public String getCurrentPriceDataQuality() {
        return currentPriceDataQuality;
    }

    public void setCurrentPriceDataQuality(String currentPriceDataQuality) {
        this.currentPriceDataQuality = currentPriceDataQuality;
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

    public Integer getLatestRiskScore() {
        return latestRiskScore;
    }

    public void setLatestRiskScore(Integer latestRiskScore) {
        this.latestRiskScore = latestRiskScore;
    }

    public String getLatestRiskLevel() {
        return latestRiskLevel;
    }

    public void setLatestRiskLevel(String latestRiskLevel) {
        this.latestRiskLevel = latestRiskLevel;
    }

    public String getLatestRecommendation() {
        return latestRecommendation;
    }

    public void setLatestRecommendation(String latestRecommendation) {
        this.latestRecommendation = latestRecommendation;
    }

    public List<StockPriceDailyResponseDto> getPriceHistory() {
        return priceHistory;
    }

    public void setPriceHistory(List<StockPriceDailyResponseDto> priceHistory) {
        this.priceHistory = priceHistory;
    }
}
