package com.example.helloworld;

import com.example.helloworld.config.ChurchLineBotConfig;
import com.example.helloworld.entity.personal.ScheduledJob;
import com.example.helloworld.repository.personal.ScheduledJobRepository;
import com.example.helloworld.scheduler.personal.DailyExpenseReminderScheduler;
import com.example.helloworld.scheduler.personal.ExchangeRateScheduler;
import com.example.helloworld.scheduler.church.ServiceScheduleNotificationScheduler;
import com.example.helloworld.scheduler.church.ActivityExpirationScheduler;
import com.example.helloworld.scheduler.church.SundayMessageExpirationScheduler;
import com.example.helloworld.scheduler.church.ImageCleanupScheduler;
import com.example.helloworld.scheduler.church.DatabaseBackupScheduler;
import com.example.helloworld.service.personal.ScheduledJobService;
import com.example.helloworld.service.church.ChurchScheduledJobService;
import com.example.helloworld.service.church.ConfigurationRefreshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private ScheduledJobRepository scheduledJobRepository;

    @Autowired
    private ChurchLineBotConfig lineBotConfig;

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

    @Autowired
<<<<<<< HEAD
    @org.springframework.beans.factory.annotation.Qualifier("personalDatabaseBackupScheduler")
    private com.example.helloworld.scheduler.personal.DatabaseBackupScheduler personalDatabaseBackupScheduler;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("churchDatabaseBackupScheduler")
=======
    @Qualifier("churchDatabaseBackupScheduler")
>>>>>>> 45b7fd36d7e04bf5e2b8c79b7542d7cec8adf2d1
    private DatabaseBackupScheduler databaseBackupScheduler;

    @Autowired
    @Qualifier("personalDatabaseBackupScheduler")
    private com.example.helloworld.scheduler.personal.DatabaseBackupScheduler personalDatabaseBackupScheduler;

    @Autowired
    private ConfigurationRefreshService configurationRefreshService;

    public static void main(String[] args) {
        SpringApplication.run(HelloWorldApplication.class, args);
    }

    @Override
    public void run(String... args) {
        // 初始化配置（從資料庫讀取）
        try {
            configurationRefreshService.initializeConfig();
            log.info("✅ 配置初始化完成");
        } catch (Exception e) {
            log.error("❌ 配置初始化失敗（資料庫可能尚未準備好）", e);
            // 不拋出異常，允許應用繼續啟動（使用預設配置）
        }

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

        // 註冊 Personal 系統資料庫備份任務執行器
        scheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.personal.DatabaseBackupScheduler$DatabaseBackupJob",
            personalDatabaseBackupScheduler.getDatabaseBackupJob()
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

<<<<<<< HEAD
        // 註冊資料庫備份任務執行器（教會網站）
=======
        // 註冊 Church 系統資料庫備份任務執行器
>>>>>>> 45b7fd36d7e04bf5e2b8c79b7542d7cec8adf2d1
        churchScheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.church.DatabaseBackupScheduler$DatabaseBackupJob",
            databaseBackupScheduler.getDatabaseBackupJob()
        );

        // 註冊資料庫備份任務執行器（個人網站）
        // 使用獨立的 Scheduler，只備份 qa_tracker
        scheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.personal.DatabaseBackupScheduler$DatabaseBackupJob",
            personalDatabaseBackupScheduler.getDatabaseBackupJob()
        );
        
        // 為了相容舊的 Job Class 名稱（如果有指向 church 的）
        scheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.church.DatabaseBackupScheduler$DatabaseBackupJob",
            personalDatabaseBackupScheduler.getDatabaseBackupJob()
        );

        // 初始化所有啟用的教會 Job
        churchScheduledJobService.initializeJobs();
    }
}


