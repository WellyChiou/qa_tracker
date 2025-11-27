package com.example.helloworld;

import com.example.helloworld.scheduler.ExchangeRateScheduler;
import com.example.helloworld.service.ScheduledJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HelloWorldApplication implements CommandLineRunner {

    @Autowired
    private ScheduledJobService scheduledJobService;

    @Autowired
    private ExchangeRateScheduler exchangeRateScheduler;

    public static void main(String[] args) {
        SpringApplication.run(HelloWorldApplication.class, args);
    }

    @Override
    public void run(String... args) {
        // 註冊 Job 執行器
        scheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.ExchangeRateScheduler$AutoFillExchangeRatesJob",
            exchangeRateScheduler.getAutoFillExchangeRatesJob()
        );

        // 初始化所有啟用的 Job
        scheduledJobService.initializeJobs();
    }
}


