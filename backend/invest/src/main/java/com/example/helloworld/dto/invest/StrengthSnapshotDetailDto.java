package com.example.helloworld.dto.invest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class StrengthSnapshotDetailDto {
    private Long id;
    private Long stockId;
    private String market;
    private String ticker;
    private String stockName;
    private String universeType;
    private Long watchlistId;
    private String watchlistName;
    private BigDecimal priceClose;
    private LocalDate priceTradeDate;
    private Integer strengthScore;
    private String strengthLevel;
    private String recommendation;
    private String dataQuality;
    private Integer strategyVersion;
    private String summary;
    private LocalDate tradeDate;
    private LocalDateTime computedAt;
    private List<StrengthFactorDto> factors;
    private String disclaimer;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getUniverseType() {
        return universeType;
    }

    public void setUniverseType(String universeType) {
        this.universeType = universeType;
    }

    public Long getWatchlistId() {
        return watchlistId;
    }

    public void setWatchlistId(Long watchlistId) {
        this.watchlistId = watchlistId;
    }

    public String getWatchlistName() {
        return watchlistName;
    }

    public void setWatchlistName(String watchlistName) {
        this.watchlistName = watchlistName;
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

    public String getStrengthLevel() {
        return strengthLevel;
    }

    public void setStrengthLevel(String strengthLevel) {
        this.strengthLevel = strengthLevel;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getDataQuality() {
        return dataQuality;
    }

    public void setDataQuality(String dataQuality) {
        this.dataQuality = dataQuality;
    }

    public Integer getStrategyVersion() {
        return strategyVersion;
    }

    public void setStrategyVersion(Integer strategyVersion) {
        this.strategyVersion = strategyVersion;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    public LocalDateTime getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(LocalDateTime computedAt) {
        this.computedAt = computedAt;
    }

    public List<StrengthFactorDto> getFactors() {
        return factors;
    }

    public void setFactors(List<StrengthFactorDto> factors) {
        this.factors = factors;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
