package com.example.helloworld.service;

import com.example.helloworld.entity.ExchangeRate;
import com.example.helloworld.repository.ExchangeRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class ExchangeRateService {

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    public Optional<ExchangeRate> getExchangeRateByDate(LocalDate date) {
        return exchangeRateRepository.findByDate(date);
    }

    public Optional<ExchangeRate> getLatestExchangeRateByDate(LocalDate date) {
        return exchangeRateRepository.findLatestByDate(date);
    }

    public ExchangeRate saveExchangeRate(ExchangeRate exchangeRate) {
        return exchangeRateRepository.save(exchangeRate);
    }

    public BigDecimal getRateForCurrency(String currency, LocalDate date) {
        Optional<ExchangeRate> rateOpt = getLatestExchangeRateByDate(date);
        if (rateOpt.isEmpty()) {
            return BigDecimal.ONE; // 預設為 1（TWD）
        }
        
        ExchangeRate rate = rateOpt.get();
        switch (currency.toUpperCase()) {
            case "USD": return rate.getUsdRate() != null ? rate.getUsdRate() : BigDecimal.ONE;
            case "EUR": return rate.getEurRate() != null ? rate.getEurRate() : BigDecimal.ONE;
            case "JPY": return rate.getJpyRate() != null ? rate.getJpyRate() : BigDecimal.ONE;
            case "CNY": return rate.getCnyRate() != null ? rate.getCnyRate() : BigDecimal.ONE;
            default: return BigDecimal.ONE;
        }
    }
}

