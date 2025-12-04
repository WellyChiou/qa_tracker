package com.example.helloworld;

import com.example.helloworld.config.LineBotConfig;
import com.example.helloworld.entity.ScheduledJob;
import com.example.helloworld.repository.ScheduledJobRepository;
import com.example.helloworld.scheduler.DailyExpenseReminderScheduler;
import com.example.helloworld.scheduler.ExchangeRateScheduler;
import com.example.helloworld.service.ScheduledJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
@EnableScheduling
public class HelloWorldApplication implements CommandLineRunner {

    @Autowired
    private ScheduledJobService scheduledJobService;

    @Autowired
    private ScheduledJobRepository scheduledJobRepository;

    @Autowired
    private LineBotConfig lineBotConfig;

    @Autowired
    private ExchangeRateScheduler exchangeRateScheduler;

    @Autowired
    private DailyExpenseReminderScheduler dailyExpenseReminderScheduler;

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

        // 註冊 LINE Bot 每日費用提醒任務執行器
        scheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.DailyExpenseReminderScheduler$SendDailyExpenseReminderJob",
            dailyExpenseReminderScheduler.getSendDailyExpenseReminderJob()
        );

        // 註冊 LINE Bot 每日費用檢查與統計任務執行器
        scheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.DailyExpenseReminderScheduler$CheckAndNotifyDailyExpenseJob",
            dailyExpenseReminderScheduler.getCheckAndNotifyDailyExpenseJob()
        );

        // 初始化所有啟用的 Job
        scheduledJobService.initializeJobs();
    }
}


