-- 清理孤立的執行記錄（如果有的話）
DELETE je FROM job_executions je 
LEFT JOIN scheduled_jobs sj ON je.job_id = sj.id 
WHERE sj.id IS NULL;

-- 重新檢查任務狀態
SELECT 
  sj.id, 
  sj.job_name, 
  COUNT(je.id) as execution_count,
  MAX(je.created_at) as last_execution
FROM scheduled_jobs sj 
LEFT JOIN job_executions je ON sj.id = je.job_id 
GROUP BY sj.id, sj.job_name 
ORDER BY sj.id;
