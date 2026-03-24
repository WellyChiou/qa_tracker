package com.example.helloworld.dto.invest;

public class WatchlistItemUpsertRequestDto {
    private Long stockId;
    private String note;

    public Long getStockId() {
        return stockId;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
