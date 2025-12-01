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

@SpringBootApplication
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
    private DailyExpenseReminderScheduler expenseReminderScheduler;

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

        scheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.DailyExpenseReminderScheduler$DailyExpenseReminderJob",
            expenseReminderScheduler.getDailyExpenseReminderJob()
        );

        scheduledJobService.registerJobExecutor(
            "com.example.helloworld.scheduler.DailyExpenseReminderScheduler$DailyExpenseReportJob",
            expenseReminderScheduler.getDailyExpenseReportJob()
        );

        // 創建預設的費用提醒任務（如果不存在）
        createDefaultExpenseReminderJobs();

        // 初始化所有啟用的 Job
        scheduledJobService.initializeJobs();
    }

    /**
     * 創建預設的費用提醒任務
     */
    private void createDefaultExpenseReminderJobs() {
        // 創建每日費用提醒任務
        String reminderJobClass = "com.example.helloworld.scheduler.DailyExpenseReminderScheduler$DailyExpenseReminderJob";
        if (scheduledJobRepository.findByJobClass(reminderJobClass).isEmpty()) {
            ScheduledJob reminderJob = new ScheduledJob();
            reminderJob.setJobName("每日費用記錄提醒");
            reminderJob.setJobClass(reminderJobClass);
            reminderJob.setCronExpression(lineBotConfig.getDailyReminderCron());
            reminderJob.setDescription("每天提醒用戶記錄當日費用");
            reminderJob.setEnabled(lineBotConfig.isDailyReminderEnabled());
            scheduledJobRepository.save(reminderJob);
            System.out.println("✅ 已創建預設費用提醒任務: " + reminderJob.getJobName());
        }

        // 創建每日費用統計報告任務
        String reportJobClass = "com.example.helloworld.scheduler.DailyExpenseReminderScheduler$DailyExpenseReportJob";
        if (scheduledJobRepository.findByJobClass(reportJobClass).isEmpty()) {
            ScheduledJob reportJob = new ScheduledJob();
            reportJob.setJobName("每日費用統計報告");
            reportJob.setJobClass(reportJobClass);
            reportJob.setCronExpression("0 0 21 * * ?"); // 每天晚上 9 點
            reportJob.setDescription("每天發送當日費用統計報告給用戶");
            reportJob.setEnabled(lineBotConfig.isDailyReminderEnabled());
            scheduledJobRepository.save(reportJob);
            System.out.println("✅ 已創建預設費用統計任務: " + reportJob.getJobName());
        }
    }
}


