package com.example.helloworld.dto.invest;

import java.time.LocalDate;

public class SystemSchedulerJobLogFailedItemDto {
    private String ticker;
    private LocalDate tradeDate;
    private String reason;

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
