package com.example.helloworld.entity.invest;

public enum StrengthRecommendationCode {
    OBSERVE,
    WAIT_PULLBACK,
    REEVALUATE_WHEN_CONDITION_MET,
    NOT_SUITABLE_CHASE;

    public static StrengthRecommendationCode fromStrengthLevel(StrengthLevelCode level) {
        return switch (level) {
            case STRONG -> OBSERVE;
            case GOOD -> WAIT_PULLBACK;
            case NEUTRAL -> REEVALUATE_WHEN_CONDITION_MET;
            case WEAK -> NOT_SUITABLE_CHASE;
        };
    }
}
