package com.example.helloworld.dto.invest;

import java.math.BigDecimal;

public class PortfolioAlertSettingUpsertRequestDto {
    private BigDecimal stopLossPrice;
    private BigDecimal alertDropPercent;
    private Boolean enabled;

    public BigDecimal getStopLossPrice() {
        return stopLossPrice;
    }

    public void setStopLossPrice(BigDecimal stopLossPrice) {
        this.stopLossPrice = stopLossPrice;
    }

    public BigDecimal getAlertDropPercent() {
        return alertDropPercent;
    }

    public void setAlertDropPercent(BigDecimal alertDropPercent) {
        this.alertDropPercent = alertDropPercent;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
