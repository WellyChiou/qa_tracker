package com.example.helloworld.dto.invest;

import java.time.LocalDateTime;

public class StrategySettingsDto {

    private Integer strategyVersion;
    private LocalDateTime lastUpdatedAt;
    private StrengthThresholds strength;
    private DataQualityThresholds dataQuality;
    private OpportunityThresholds opportunity;

    public Integer getStrategyVersion() {
        return strategyVersion;
    }

    public void setStrategyVersion(Integer strategyVersion) {
        this.strategyVersion = strategyVersion;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public StrengthThresholds getStrength() {
        return strength;
    }

    public void setStrength(StrengthThresholds strength) {
        this.strength = strength;
    }

    public DataQualityThresholds getDataQuality() {
        return dataQuality;
    }

    public void setDataQuality(DataQualityThresholds dataQuality) {
        this.dataQuality = dataQuality;
    }

    public OpportunityThresholds getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(OpportunityThresholds opportunity) {
        this.opportunity = opportunity;
    }

    public static class StrengthThresholds {
        private Integer strongMin;
        private Integer goodMin;
        private Integer weakMax;

        public Integer getStrongMin() {
            return strongMin;
        }

        public void setStrongMin(Integer strongMin) {
            this.strongMin = strongMin;
        }

        public Integer getGoodMin() {
            return goodMin;
        }

        public void setGoodMin(Integer goodMin) {
            this.goodMin = goodMin;
        }

        public Integer getWeakMax() {
            return weakMax;
        }

        public void setWeakMax(Integer weakMax) {
            this.weakMax = weakMax;
        }
    }

    public static class DataQualityThresholds {
        private Integer minHistoryDays;
        private Integer staleDays;

        public Integer getMinHistoryDays() {
            return minHistoryDays;
        }

        public void setMinHistoryDays(Integer minHistoryDays) {
            this.minHistoryDays = minHistoryDays;
        }

        public Integer getStaleDays() {
            return staleDays;
        }

        public void setStaleDays(Integer staleDays) {
            this.staleDays = staleDays;
        }
    }

    public static class OpportunityThresholds {
        private Integer observeMinScore;
        private Integer waitPullbackMinScore;
        private Integer reevaluateMinScore;

        public Integer getObserveMinScore() {
            return observeMinScore;
        }

        public void setObserveMinScore(Integer observeMinScore) {
            this.observeMinScore = observeMinScore;
        }

        public Integer getWaitPullbackMinScore() {
            return waitPullbackMinScore;
        }

        public void setWaitPullbackMinScore(Integer waitPullbackMinScore) {
            this.waitPullbackMinScore = waitPullbackMinScore;
        }

        public Integer getReevaluateMinScore() {
            return reevaluateMinScore;
        }

        public void setReevaluateMinScore(Integer reevaluateMinScore) {
            this.reevaluateMinScore = reevaluateMinScore;
        }
    }
}
