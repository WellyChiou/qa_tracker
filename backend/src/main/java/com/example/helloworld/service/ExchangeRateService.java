package com.example.helloworld.service;

import com.example.helloworld.entity.ExchangeRate;
import com.example.helloworld.repository.ExchangeRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ExchangeRateService {

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    private final RestTemplate restTemplate = new RestTemplate();

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

    /**
     * 從外部 API 取得匯率
     */
    public BigDecimal fetchExchangeRateFromAPI(String currency) {
        try {
            String url = "https://api.exchangerate-api.com/v4/latest/TWD";
            var response = restTemplate.getForObject(url, java.util.Map.class);
            
            if (response != null && response.containsKey("rates")) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> rates = (java.util.Map<String, Object>) response.get("rates");
                
                if (rates.containsKey(currency)) {
                    // API 回傳的是 1 TWD = ? 其他幣別
                    // 我們需要的是 1 其他幣別 = ? TWD
                    double rate = 1.0 / ((Number) rates.get(currency)).doubleValue();
                    return BigDecimal.valueOf(rate).setScale(4, BigDecimal.ROUND_HALF_UP);
                }
            }
            
            // 如果 API 失敗，使用備用匯率
            return getFallbackRate(currency);
        } catch (Exception e) {
            System.err.println("取得 " + currency + " 匯率失敗: " + e.getMessage());
            return getFallbackRate(currency);
        }
    }

    /**
     * 取得備用匯率（當 API 失敗時使用）
     */
    private BigDecimal getFallbackRate(String currency) {
        switch (currency.toUpperCase()) {
            case "USD": return BigDecimal.valueOf(30.5);
            case "EUR": return BigDecimal.valueOf(32.8);
            case "JPY": return BigDecimal.valueOf(0.21);
            case "CNY": return BigDecimal.valueOf(4.3);
            default: return BigDecimal.ONE;
        }
    }

    /**
     * 檢查指定日期的匯率是否存在且完整
     */
    public boolean checkDateRatesExist(LocalDate date) {
        Optional<ExchangeRate> rateOpt = getExchangeRateByDate(date);
        if (rateOpt.isEmpty()) {
            return false;
        }
        
        ExchangeRate rate = rateOpt.get();
        // 檢查是否所有幣別都有匯率
        return rate.getUsdRate() != null && 
               rate.getEurRate() != null && 
               rate.getJpyRate() != null && 
               rate.getCnyRate() != null;
    }

    /**
     * 補足指定日期的所有匯率
     */
    public boolean fillDateRates(LocalDate date) {
        try {
            ExchangeRate exchangeRate = new ExchangeRate();
            exchangeRate.setDate(date);
            exchangeRate.setUsdRate(fetchExchangeRateFromAPI("USD"));
            exchangeRate.setEurRate(fetchExchangeRateFromAPI("EUR"));
            exchangeRate.setJpyRate(fetchExchangeRateFromAPI("JPY"));
            exchangeRate.setCnyRate(fetchExchangeRateFromAPI("CNY"));
            
            saveExchangeRate(exchangeRate);
            System.out.println("✅ " + date + " 所有匯率補足成功");
            return true;
        } catch (Exception e) {
            System.err.println("❌ " + date + " 匯率補足失敗: " + e.getMessage());
            return false;
        }
    }

    /**
     * 檢查並補足最近 N 天缺少的匯率
     */
    public int checkAndAutoFillMissingRates(int days) {
        List<LocalDate> missingDates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        // 檢查最近 N 天（只檢查過去日期，不檢查未來日期）
        for (int i = 0; i < days; i++) {
            LocalDate checkDate = today.minusDays(i);
            if (!checkDateRatesExist(checkDate)) {
                missingDates.add(checkDate);
            }
        }
        
        if (missingDates.isEmpty()) {
            System.out.println("✅ 所有日期匯率資料已存在，無需補足");
            return 0;
        }
        
        System.out.println("⚠️ 發現 " + missingDates.size() + " 個日期缺少匯率資料，開始補足...");
        
        int successCount = 0;
        for (LocalDate date : missingDates) {
            if (fillDateRates(date)) {
                successCount++;
            }
            // 稍微延遲，避免 API 請求過快
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("✅ 已補足 " + successCount + "/" + missingDates.size() + " 個日期的匯率");
        return successCount;
    }
}

