package com.example.helloworld.scheduler.personal;

import com.example.helloworld.service.personal.ExchangeRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateScheduler {

    @Autowired
    private ExchangeRateService exchangeRateService;

    /**
     * 自動補足匯率任務執行器
     */
    public static class AutoFillExchangeRatesJob implements Runnable {
        private final ExchangeRateService exchangeRateService;

        public AutoFillExchangeRatesJob(ExchangeRateService exchangeRateService) {
            this.exchangeRateService = exchangeRateService;
        }

        @Override
        public void run() {
            System.out.println("🔄 開始執行自動補足匯率任務...");
            int filledCount = exchangeRateService.checkAndAutoFillMissingRates(7);
            System.out.println("✅ 自動補足匯率任務完成，補足 " + filledCount + " 個日期");
        }
    }

    /**
     * 獲取自動補足匯率任務執行器
     */
    public Runnable getAutoFillExchangeRatesJob() {
        return new AutoFillExchangeRatesJob(exchangeRateService);
    }
}
