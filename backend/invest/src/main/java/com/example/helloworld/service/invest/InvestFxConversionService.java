package com.example.helloworld.service.invest;

import com.example.helloworld.service.invest.system.InvestSystemSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Set;

@Service
public class InvestFxConversionService {

    private static final Logger log = LoggerFactory.getLogger(InvestFxConversionService.class);
    private static final int MONEY_SCALE = 2;
    private static final String SETTING_KEY_USD_TO_TWD = "invest.analysis.fx.usd_to_twd";
    private static final BigDecimal DEFAULT_USD_TO_TWD = BigDecimal.valueOf(32);
    private static final Set<String> USD_MARKETS = Set.of("US", "NYSE", "NASDAQ", "AMEX", "USA");
    private static final String BASE_CURRENCY = "TWD";

    private final InvestSystemSettingService investSystemSettingService;

    public InvestFxConversionService(InvestSystemSettingService investSystemSettingService) {
        this.investSystemSettingService = investSystemSettingService;
    }

    public String resolveAssetCurrencyByMarket(String market) {
        if (market == null || market.isBlank()) {
            return BASE_CURRENCY;
        }
        String normalized = market.trim().toUpperCase(Locale.ROOT);
        return USD_MARKETS.contains(normalized) ? "USD" : BASE_CURRENCY;
    }

    public BigDecimal convertToBaseCurrency(BigDecimal amount, String assetCurrency) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        String currency = normalizeCurrency(assetCurrency);

        if (BASE_CURRENCY.equals(currency)) {
            return safeAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        if ("USD".equals(currency)) {
            return safeAmount.multiply(getUsdToTwdRate()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        // 目前只支援 TWD/USD，其他幣別先保守回原值，並保留告警方便追查。
        log.warn("尚未支援的幣別換算，先回傳原值。assetCurrency={}", currency);
        return safeAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal convertByMarketToBaseCurrency(BigDecimal amount, String market) {
        return convertToBaseCurrency(amount, resolveAssetCurrencyByMarket(market));
    }

    public BigDecimal getUsdToTwdRate() {
        String raw = investSystemSettingService.getSettingValue(SETTING_KEY_USD_TO_TWD, DEFAULT_USD_TO_TWD.toPlainString());
        try {
            BigDecimal parsed = new BigDecimal(raw.trim());
            if (parsed.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("USD/TWD 匯率設定非正值，改用預設值。key={}, value={}", SETTING_KEY_USD_TO_TWD, raw);
                return DEFAULT_USD_TO_TWD;
            }
            return parsed;
        } catch (Exception e) {
            log.warn("USD/TWD 匯率設定無法解析，改用預設值。key={}, value={}", SETTING_KEY_USD_TO_TWD, raw);
            return DEFAULT_USD_TO_TWD;
        }
    }

    public String getBaseCurrency() {
        return BASE_CURRENCY;
    }

    private String normalizeCurrency(String assetCurrency) {
        if (assetCurrency == null || assetCurrency.isBlank()) {
            return BASE_CURRENCY;
        }
        return assetCurrency.trim().toUpperCase(Locale.ROOT);
    }
}

