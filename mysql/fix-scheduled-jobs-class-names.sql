-- 修正 scheduled_jobs 表中的 Job class 名稱
-- 將舊的類名更新為新的類名（已移動到 scheduler.personal 包）

USE qa_tracker;

-- 更新 DailyExpenseReminderJob 的類名（移動到 personal 包）
UPDATE scheduled_jobs 
SET job_class = 'com.example.helloworld.scheduler.personal.DailyExpenseReminderScheduler$SendDailyExpenseReminderJob'
WHERE job_class = 'com.example.helloworld.scheduler.DailyExpenseReminderScheduler$SendDailyExpenseReminderJob'
   OR job_class = 'com.example.helloworld.scheduler.DailyExpenseReminderScheduler$DailyExpenseReminderJob'
   OR job_class LIKE '%DailyExpenseReminderJob%';

-- 更新 DailyExpenseReportJob 的類名（移動到 personal 包）
UPDATE scheduled_jobs 
SET job_class = 'com.example.helloworld.scheduler.personal.DailyExpenseReminderScheduler$CheckAndNotifyDailyExpenseJob'
WHERE job_class = 'com.example.helloworld.scheduler.DailyExpenseReminderScheduler$CheckAndNotifyDailyExpenseJob'
   OR job_class = 'com.example.helloworld.scheduler.DailyExpenseReminderScheduler$DailyExpenseReportJob'
   OR job_class LIKE '%DailyExpenseReportJob%'
   OR job_class LIKE '%CheckAndNotifyDailyExpenseJob%';

-- 更新 ExchangeRateScheduler 的類名（移動到 personal 包）
UPDATE scheduled_jobs 
SET job_class = 'com.example.helloworld.scheduler.personal.ExchangeRateScheduler$AutoFillExchangeRatesJob'
WHERE job_class = 'com.example.helloworld.scheduler.ExchangeRateScheduler$AutoFillExchangeRatesJob'
   OR job_class LIKE '%ExchangeRateScheduler%';

-- 驗證更新結果
SELECT id, job_name, job_class, enabled 
FROM scheduled_jobs 
WHERE job_class LIKE '%scheduler%'
ORDER BY id;

