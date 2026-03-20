package com.example.helloworld.entity.invest;

public enum MarketAnalysisScopeCode {
    HOLDINGS_ONLY,
    HOLDINGS_AND_WATCHLIST;

    public static MarketAnalysisScopeCode fromNullable(String value) {
        if (value == null || value.trim().isEmpty()) {
            return HOLDINGS_AND_WATCHLIST;
        }
        try {
            return MarketAnalysisScopeCode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("scope 無效，請使用 HOLDINGS_ONLY 或 HOLDINGS_AND_WATCHLIST");
        }
    }
}
