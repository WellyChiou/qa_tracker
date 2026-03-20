package com.example.helloworld.service.invest.price;

import com.example.helloworld.entity.invest.Stock;

import java.util.Optional;

public interface MarketPriceProvider {
    boolean supportsMarket(String market);

    Optional<PriceQuoteSnapshot> fetchLatestQuote(Stock stock);
}
