package com.example.helloworld;

import com.example.helloworld.config.LineBotConfig;
import com.example.helloworld.entity.personal.ScheduledJob;
import com.example.helloworld.repository.personal.ScheduledJobRepository;
import com.example.helloworld.scheduler.personal.DailyExpenseReminderScheduler;
import com.example.helloworld.scheduler.personal.ExchangeRateScheduler;
import com.example.helloworld.scheduler.church.ServiceScheduleNotificationScheduler;
import com.example.helloworld.scheduler.church.ActivityExpirationScheduler;
import com.example.helloworld.scheduler.church.SundayMessageExpirationScheduler;
import com.example.helloworld.scheduler.church.ImageCleanupScheduler;
import com.example.helloworld.service.personal.ScheduledJobService;
import com.example.helloworld.service.church.ChurchScheduledJobService;
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

    @Autowired
    private ChurchScheduledJobService churchScheduledJobService;

    @Autowired
    private ServiceScheduleNotificationScheduler serviceScheduleNotificationScheduler;

    @Autowired
    private ActivityExpirationScheduler activityExpirationScheduler;

    @Autowired
    private SundayMessageExpirationScheduler sundayMessageExpirationScheduler;

    @Autowired
    private ImageCleanupScheduler imageCleanupScheduler;

    public static void main(String[] args) {
        SpringApplication.run(HelloWorldApplication.class, args);
    }

    @Override
    public void run(String... args) {
        // 註冊 Job 執行器
        scheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.personal.ExchangeRateScheduler$AutoFillExchangeRatesJob",
            exchangeRateScheduler.getAutoFillExchangeRatesJob()
        );

        // 註冊 LINE Bot 每日費用提醒任務執行器
        scheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.personal.DailyExpenseReminderScheduler$SendDailyExpenseReminderJob",
            dailyExpenseReminderScheduler.getSendDailyExpenseReminderJob()
        );

        // 註冊 LINE Bot 每日費用檢查與統計任務執行器
        scheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.personal.DailyExpenseReminderScheduler$CheckAndNotifyDailyExpenseJob",
            dailyExpenseReminderScheduler.getCheckAndNotifyDailyExpenseJob()
        );

        // 初始化所有啟用的 Job
        scheduledJobService.initializeJobs();

        // 註冊教會排程任務執行器
        churchScheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.church.ServiceScheduleNotificationScheduler$WeeklyServiceNotificationJob",
            serviceScheduleNotificationScheduler.getWeeklyServiceNotificationJob()
        );

        // 註冊活動過期檢查任務執行器
        churchScheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.church.ActivityExpirationScheduler$ActivityExpirationJob",
            activityExpirationScheduler.getActivityExpirationJob()
        );

        // 註冊主日信息過期檢查任務執行器
        churchScheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.church.SundayMessageExpirationScheduler$SundayMessageExpirationJob",
            sundayMessageExpirationScheduler.getSundayMessageExpirationJob()
        );

        // 註冊圖片清理任務執行器
        churchScheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.church.ImageCleanupScheduler$ImageCleanupJob",
            imageCleanupScheduler.getImageCleanupJob()
        );

        // 初始化所有啟用的教會 Job
        churchScheduledJobService.initializeJobs();
    }
}


