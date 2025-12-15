package com.example.helloworld.scheduler.personal;

import com.example.helloworld.service.personal.ExchangeRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ExchangeRateScheduler {

    @Autowired
    private ExchangeRateService exchangeRateService;

    /**
     * 自動補足匯率任務執行器
     */
    public static class AutoFillExchangeRatesJob implements Runnable {
        private static final Logger log = LoggerFactory.getLogger(AutoFillExchangeRatesJob.class);
        private final ExchangeRateService exchangeRateService;

        public AutoFillExchangeRatesJob(ExchangeRateService exchangeRateService) {
            this.exchangeRateService = exchangeRateService;
        }

        @Override
        public void run() {
            log.info("🔄 開始執行自動補足匯率任務...");
            try {
                int filledCount = exchangeRateService.checkAndAutoFillMissingRates(7);
                log.info("✅ 自動補足匯率任務完成，補足 {} 個日期", filledCount);
            } catch (Exception e) {
                log.error("❌ 自動補足匯率任務失敗", e);
                // 重新拋出異常，讓外層 Job 執行器捕獲並更新狀態
                throw new RuntimeException("自動補足匯率任務失敗: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 獲取自動補足匯率任務執行器
     */
    public Runnable getAutoFillExchangeRatesJob() {
        return new AutoFillExchangeRatesJob(exchangeRateService);
    }
}
