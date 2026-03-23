package com.example.helloworld.service.invest.systemscheduler;

public enum SystemJobCode {
    PRICE_UPDATE_HOLDINGS,
    DAILY_PORTFOLIO_RISK_REPORT,
    PORTFOLIO_ALERT_POLLING,
    MARKET_ANALYSIS;

    public static SystemJobCode fromPathValue(String value) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException("jobCode 不可為空");
        }
        try {
            return SystemJobCode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("不支援的 jobCode：" + value);
        }
    }
}
