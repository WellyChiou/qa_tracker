package com.example.helloworld.entity.invest;

public enum RiskLevelCode {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static RiskLevelCode fromScore(int score) {
        if (score >= 75) {
            return CRITICAL;
        }
        if (score >= 50) {
            return HIGH;
        }
        if (score >= 25) {
            return MEDIUM;
        }
        return LOW;
    }
}
