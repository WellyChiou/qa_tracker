-- 刪除舊的重複任務
DELETE FROM scheduled_jobs 
WHERE job_class LIKE '%CheckAndNotifyDailyExpenseJob%' 
   OR job_class LIKE '%SendDailyExpenseReminderJob%';

-- 確認刪除後的任務列表
SELECT id, job_name, job_class, cron_expression, enabled, created_at 
FROM scheduled_jobs 
ORDER BY id;
