-- 檢查當前資料庫中的任務
SELECT id, job_name, job_class, enabled FROM scheduled_jobs ORDER BY id;

-- 檢查任務執行記錄
SELECT job_id, COUNT(*) as execution_count, 
       MAX(created_at) as last_execution,
       MIN(created_at) as first_execution
FROM job_executions 
GROUP BY job_id 
ORDER BY job_id;

-- 檢查是否存在孤立的執行記錄（任務已被刪除但記錄還在）
SELECT je.* FROM job_executions je 
LEFT JOIN scheduled_jobs sj ON je.job_id = sj.id 
WHERE sj.id IS NULL;
