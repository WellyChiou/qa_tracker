package com.example.helloworld;

import com.example.helloworld.scheduler.church.ServiceScheduleNotificationScheduler;
import com.example.helloworld.scheduler.church.ActivityExpirationScheduler;
import com.example.helloworld.scheduler.church.SundayMessageExpirationScheduler;
import com.example.helloworld.scheduler.church.ImageCleanupScheduler;
import com.example.helloworld.scheduler.church.DatabaseBackupScheduler;
import com.example.helloworld.service.church.ChurchScheduledJobService;
import com.example.helloworld.service.church.ConfigurationRefreshService;
import com.example.helloworld.service.church.seed.ChurchAuthorizationSeedRunner;
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
    @Qualifier("churchDatabaseBackupScheduler")
    private DatabaseBackupScheduler databaseBackupScheduler;

    @Autowired
    private ConfigurationRefreshService configurationRefreshService;

    @Autowired
    private ChurchAuthorizationSeedRunner churchAuthorizationSeedRunner;

    public static void main(String[] args) {
        SpringApplication.run(HelloWorldApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("🚀 啟動 Church Backend");

        // 初始化配置（從資料庫讀取）
        try {
            configurationRefreshService.initializeConfig();
            log.info("✅ Church 配置初始化完成");
        } catch (Exception e) {
            log.error("❌ 配置初始化失敗（資料庫可能尚未準備好）", e);
            // 不拋出異常，允許應用繼續啟動（使用預設配置）
        }

        try {
            churchAuthorizationSeedRunner.seedAll();
        } catch (Exception e) {
            log.error("❌ 初始化 Church 權限/菜單資源失敗", e);
        }

        initializeChurchJobs();
    }

    private void initializeChurchJobs() {
        log.info("🔧 初始化 Church 動態排程");

        churchScheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.church.ServiceScheduleNotificationScheduler$WeeklyServiceNotificationJob",
            serviceScheduleNotificationScheduler.getWeeklyServiceNotificationJob()
        );

        churchScheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.church.ActivityExpirationScheduler$ActivityExpirationJob",
            activityExpirationScheduler.getActivityExpirationJob()
        );

        churchScheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.church.SundayMessageExpirationScheduler$SundayMessageExpirationJob",
            sundayMessageExpirationScheduler.getSundayMessageExpirationJob()
        );

        churchScheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.church.ImageCleanupScheduler$ImageCleanupJob",
            imageCleanupScheduler.getImageCleanupJob()
        );

        churchScheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.church.DatabaseBackupScheduler$DatabaseBackupJob",
            databaseBackupScheduler.getDatabaseBackupJob()
        );

        churchScheduledJobService.initializeJobs();
        log.info("✅ Church 動態排程初始化完成");
    }
}
