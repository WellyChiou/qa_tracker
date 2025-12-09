-- 添加教會排程任務：週二早上10點查詢本周六日服事人員，發送 LINE 通知
-- 執行此腳本前，請先執行 church-scheduled-jobs-schema.sql 創建表結構

USE church;

-- 插入新的排程任務
INSERT INTO scheduled_jobs (job_name, job_class, cron_expression, description, enabled) 
VALUES (
    '週二服事人員通知',
    'com.example.helloworld.scheduler.church.ServiceScheduleNotificationScheduler$WeeklyServiceNotificationJob',
    '0 0 10 ? * TUE',
    '每週二早上 10:00 查詢本周六日服事人員，發送 LINE 通知',
    TRUE
)
ON DUPLICATE KEY UPDATE
    job_name = VALUES(job_name),
    cron_expression = VALUES(cron_expression),
    description = VALUES(description);

-- 顯示插入結果
SELECT '✅ 教會排程任務已添加' AS message;
SELECT * FROM scheduled_jobs WHERE job_name = '週二服事人員通知';

