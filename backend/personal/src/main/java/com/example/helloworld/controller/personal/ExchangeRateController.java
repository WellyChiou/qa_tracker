package com.example.helloworld.controller.personal;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.entity.personal.ExchangeRate;
import com.example.helloworld.service.personal.ExchangeRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/personal/exchange-rates")
@CrossOrigin(origins = "*")
public class ExchangeRateController {

    @Autowired
    private ExchangeRateService exchangeRateService;

    @GetMapping("/{date}")
    public ResponseEntity<ApiResponse<ExchangeRate>> getExchangeRateByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return exchangeRateService.getExchangeRateByDate(date)
                .map(e -> ResponseEntity.ok(ApiResponse.ok(e)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("匯率不存在")));
    }

    @GetMapping("/latest/{date}")
    public ResponseEntity<ApiResponse<ExchangeRate>> getLatestExchangeRateByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return exchangeRateService.getLatestExchangeRateByDate(date)
                .map(e -> ResponseEntity.ok(ApiResponse.ok(e)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("匯率不存在")));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExchangeRate>> createExchangeRate(@RequestBody ExchangeRate exchangeRate) {
        if (exchangeRate.getDate() != null) {
            Optional<ExchangeRate> existingOpt = exchangeRateService.getExchangeRateByDate(exchangeRate.getDate());
            if (existingOpt.isPresent()) {
                ExchangeRate existing = existingOpt.get();
                if (exchangeRate.getUsdRate() != null) existing.setUsdRate(exchangeRate.getUsdRate());
                if (exchangeRate.getEurRate() != null) existing.setEurRate(exchangeRate.getEurRate());
                if (exchangeRate.getJpyRate() != null) existing.setJpyRate(exchangeRate.getJpyRate());
                if (exchangeRate.getCnyRate() != null) existing.setCnyRate(exchangeRate.getCnyRate());
                return ResponseEntity.ok(ApiResponse.ok(exchangeRateService.saveExchangeRate(existing)));
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(exchangeRateService.saveExchangeRate(exchangeRate)));
    }

    @PutMapping("/{date}")
    public ResponseEntity<ApiResponse<ExchangeRate>> updateExchangeRate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody ExchangeRate exchangeRate) {
        Optional<ExchangeRate> existingOpt = exchangeRateService.getExchangeRateByDate(date);
        if (existingOpt.isPresent()) {
            ExchangeRate existing = existingOpt.get();
            if (exchangeRate.getUsdRate() != null) existing.setUsdRate(exchangeRate.getUsdRate());
            if (exchangeRate.getEurRate() != null) existing.setEurRate(exchangeRate.getEurRate());
            if (exchangeRate.getJpyRate() != null) existing.setJpyRate(exchangeRate.getJpyRate());
            if (exchangeRate.getCnyRate() != null) existing.setCnyRate(exchangeRate.getCnyRate());
            return ResponseEntity.ok(ApiResponse.ok(exchangeRateService.saveExchangeRate(existing)));
        }
        exchangeRate.setDate(date);
        return ResponseEntity.ok(ApiResponse.ok(exchangeRateService.saveExchangeRate(exchangeRate)));
    }

    @PostMapping("/auto-fill")
    public ResponseEntity<ApiResponse<Map<String, Object>>> autoFillExchangeRates(@RequestParam(defaultValue = "7") int days) {
        int filledCount = exchangeRateService.checkAndAutoFillMissingRates(days);
        Map<String, Object> data = new HashMap<>();
        data.put("message", "已補足 " + filledCount + " 個日期的匯率");
        data.put("filledCount", filledCount);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
