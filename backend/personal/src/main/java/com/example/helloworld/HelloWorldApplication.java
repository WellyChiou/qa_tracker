package com.example.helloworld;

import com.example.helloworld.scheduler.personal.DailyExpenseReminderScheduler;
import com.example.helloworld.scheduler.personal.ExchangeRateScheduler;
import com.example.helloworld.service.personal.ScheduledJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(HelloWorldApplication.class);

    @Autowired
    private ScheduledJobService scheduledJobService;

    @Autowired
    private ExchangeRateScheduler exchangeRateScheduler;

    @Autowired
    private DailyExpenseReminderScheduler dailyExpenseReminderScheduler;

    @Autowired
    private com.example.helloworld.scheduler.personal.DatabaseBackupScheduler personalDatabaseBackupScheduler;

    public static void main(String[] args) {
        SpringApplication.run(HelloWorldApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("🚀 啟動 Personal Backend");
        initializePersonalJobs();
    }

    private void initializePersonalJobs() {
        log.info("🔧 初始化 Personal 動態排程");

        scheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.personal.ExchangeRateScheduler$AutoFillExchangeRatesJob",
            exchangeRateScheduler.getAutoFillExchangeRatesJob()
        );

        scheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.personal.DailyExpenseReminderScheduler$SendDailyExpenseReminderJob",
            dailyExpenseReminderScheduler.getSendDailyExpenseReminderJob()
        );

        scheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.personal.DailyExpenseReminderScheduler$CheckAndNotifyDailyExpenseJob",
            dailyExpenseReminderScheduler.getCheckAndNotifyDailyExpenseJob()
        );

        scheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.personal.DatabaseBackupScheduler$DatabaseBackupJob",
            personalDatabaseBackupScheduler.getDatabaseBackupJob()
        );

        scheduledJobService.initializeJobs();
        log.info("✅ Personal 動態排程初始化完成");
    }
}
