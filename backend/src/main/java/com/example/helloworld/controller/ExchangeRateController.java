package com.example.helloworld.controller;

import com.example.helloworld.entity.ExchangeRate;
import com.example.helloworld.service.ExchangeRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/exchange-rates")
@CrossOrigin(origins = "*")
public class ExchangeRateController {

    @Autowired
    private ExchangeRateService exchangeRateService;

    @GetMapping("/{date}")
    public ResponseEntity<ExchangeRate> getExchangeRateByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return exchangeRateService.getExchangeRateByDate(date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/latest/{date}")
    public ResponseEntity<ExchangeRate> getLatestExchangeRateByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return exchangeRateService.getLatestExchangeRateByDate(date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ExchangeRate> createExchangeRate(@RequestBody ExchangeRate exchangeRate) {
        // 檢查該日期的匯率是否已存在
        if (exchangeRate.getDate() != null) {
            Optional<ExchangeRate> existingOpt = exchangeRateService.getExchangeRateByDate(exchangeRate.getDate());
            if (existingOpt.isPresent()) {
                // 如果已存在，執行更新而不是創建
                ExchangeRate existing = existingOpt.get();
                if (exchangeRate.getUsdRate() != null) existing.setUsdRate(exchangeRate.getUsdRate());
                if (exchangeRate.getEurRate() != null) existing.setEurRate(exchangeRate.getEurRate());
                if (exchangeRate.getJpyRate() != null) existing.setJpyRate(exchangeRate.getJpyRate());
                if (exchangeRate.getCnyRate() != null) existing.setCnyRate(exchangeRate.getCnyRate());
                ExchangeRate saved = exchangeRateService.saveExchangeRate(existing);
                return ResponseEntity.ok(saved);
            }
        }
        // 如果不存在，創建新記錄
        ExchangeRate saved = exchangeRateService.saveExchangeRate(exchangeRate);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{date}")
    public ResponseEntity<ExchangeRate> updateExchangeRate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody ExchangeRate exchangeRate) {
        Optional<ExchangeRate> existingOpt = exchangeRateService.getExchangeRateByDate(date);
        if (existingOpt.isPresent()) {
            ExchangeRate existing = existingOpt.get();
            if (exchangeRate.getUsdRate() != null) existing.setUsdRate(exchangeRate.getUsdRate());
            if (exchangeRate.getEurRate() != null) existing.setEurRate(exchangeRate.getEurRate());
            if (exchangeRate.getJpyRate() != null) existing.setJpyRate(exchangeRate.getJpyRate());
            if (exchangeRate.getCnyRate() != null) existing.setCnyRate(exchangeRate.getCnyRate());
            return ResponseEntity.ok(exchangeRateService.saveExchangeRate(existing));
        } else {
            exchangeRate.setDate(date);
            return ResponseEntity.ok(exchangeRateService.saveExchangeRate(exchangeRate));
        }
    }

    /**
     * 手動觸發補足匯率（用於測試或手動執行）
     * @param days 要檢查的天數，預設為 7 天
     */
    @PostMapping("/auto-fill")
    public ResponseEntity<Map<String, Object>> autoFillExchangeRates(@RequestParam(defaultValue = "7") int days) {
        int filledCount = exchangeRateService.checkAndAutoFillMissingRates(days);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "已補足 " + filledCount + " 個日期的匯率");
        response.put("filledCount", filledCount);
        return ResponseEntity.ok(response);
    }
}

