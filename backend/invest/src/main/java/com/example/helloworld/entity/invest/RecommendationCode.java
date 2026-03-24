package com.example.helloworld.entity.invest;

public enum RecommendationCode {
    HOLD,
    WATCH,
    REDUCE,
    STOP_LOSS_CHECK;

    public static RecommendationCode fromRiskLevel(RiskLevelCode riskLevel) {
        return switch (riskLevel) {
            case LOW -> HOLD;
            case MEDIUM -> WATCH;
            case HIGH -> REDUCE;
            case CRITICAL -> STOP_LOSS_CHECK;
        };
    }
}
