package com.example.helloworld.entity.invest;

public enum StrengthLevelCode {
    STRONG,
    GOOD,
    NEUTRAL,
    WEAK;

    public static StrengthLevelCode fromScore(int score) {
        if (score >= 80) {
            return STRONG;
        }
        if (score >= 65) {
            return GOOD;
        }
        if (score >= 45) {
            return NEUTRAL;
        }
        return WEAK;
    }
}
