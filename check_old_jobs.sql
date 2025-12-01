-- 檢查舊的費用提醒任務
SELECT id, job_name, job_class, cron_expression, enabled, created_at 
FROM scheduled_jobs 
WHERE job_class LIKE '%CheckAndNotifyDailyExpenseJob%' 
   OR job_class LIKE '%SendDailyExpenseReminderJob%'
   OR job_name LIKE '%CheckAndNotify%'
   OR job_name LIKE '%SendDailyExpense%';

-- 查看當前有效的費用提醒任務
SELECT id, job_name, job_class, cron_expression, enabled, created_at 
FROM scheduled_jobs 
WHERE job_class LIKE '%DailyExpenseReminderJob%' 
   OR job_class LIKE '%DailyExpenseReportJob%';
