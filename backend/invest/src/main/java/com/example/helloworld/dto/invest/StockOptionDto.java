package com.example.helloworld.dto.invest;

public class StockOptionDto {
    private Long id;
    private String ticker;
    private String name;

    public StockOptionDto() {
    }

    public StockOptionDto(Long id, String ticker, String name) {
        this.id = id;
        this.ticker = ticker;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
