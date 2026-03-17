-- 整合所有診斷檢查腳本
-- 使用方式：在 MySQL 中執行此 SQL 文件
-- 注意：某些檢查針對特定資料庫，請根據需要選擇執行

-- ============================================
-- 1. 檢查 LINE Bot 配置（qa_tracker 資料庫）
-- ============================================
-- USE qa_tracker;
-- SELECT '=== LINE Bot 配置檢查 ===' as section;
-- SELECT config_key, config_value 
-- FROM config 
-- WHERE config_key LIKE '%daily%reminder%' 
--    OR config_key LIKE '%line%';
-- 
-- SELECT '=== 用戶 LINE ID 檢查 ===' as section;
-- SELECT display_name, line_user_id 
-- FROM users 
-- WHERE line_user_id IS NOT NULL AND line_user_id != '';

-- ============================================
-- 2. 檢查用戶和費用記錄（qa_tracker 資料庫）
-- ============================================
-- USE qa_tracker;
-- SELECT '=== 用戶信息檢查 ===' as section;
-- SELECT uid, username, display_name, email FROM users;
-- 
-- SELECT '=== 費用記錄 member 分佈 ===' as section;
-- SELECT member, COUNT(*) as count FROM expenses GROUP BY member ORDER BY count DESC;
-- 
-- SELECT '=== 今天的費用記錄 ===' as section;
-- SELECT member, type, main_category, sub_category, amount, date, created_at 
-- FROM expenses 
-- WHERE DATE(date) = CURDATE() 
-- ORDER BY created_at DESC;

-- ============================================
-- 3. 檢查舊的定時任務（qa_tracker 或 church 資料庫）
-- ============================================
-- USE qa_tracker;  -- 或 USE church;
-- SELECT '=== 舊的費用提醒任務 ===' as section;
-- SELECT id, job_name, job_class, cron_expression, enabled, created_at 
-- FROM scheduled_jobs 
-- WHERE job_class LIKE '%CheckAndNotifyDailyExpenseJob%' 
--    OR job_class LIKE '%SendDailyExpenseReminderJob%'
--    OR job_name LIKE '%CheckAndNotify%'
--    OR job_name LIKE '%SendDailyExpense%';
-- 
-- SELECT '=== 當前有效的費用提醒任務 ===' as section;
-- SELECT id, job_name, job_class, cron_expression, enabled, created_at 
-- FROM scheduled_jobs 
-- WHERE job_class LIKE '%DailyExpenseReminderJob%' 
--    OR job_class LIKE '%DailyExpenseReportJob%';

-- ============================================
-- 4. 檢查前台菜單配置（church 資料庫）
-- ============================================
USE church;

SELECT '=== 所有前台菜單 ===' as section;
SELECT 
    id,
    menu_code,
    menu_name,
    icon,
    url,
    parent_id,
    order_index,
    is_active,
    menu_type,
    required_permission,
    description
FROM menu_items
WHERE menu_type = 'frontend'
ORDER BY order_index, id;

SELECT '=== 啟用的前台菜單 ===' as section;
SELECT 
    id,
    menu_code,
    menu_name,
    icon,
    url,
    parent_id,
    order_index
FROM menu_items
WHERE menu_type = 'frontend' 
  AND is_active = 1
  AND parent_id IS NULL
ORDER BY order_index, id;

SELECT '=== 前台菜單統計 ===' as section;
SELECT 
    COUNT(*) as total_frontend_menus,
    SUM(CASE WHEN is_active = 1 THEN 1 ELSE 0 END) as active_menus,
    SUM(CASE WHEN is_active = 0 THEN 1 ELSE 0 END) as inactive_menus,
    SUM(CASE WHEN parent_id IS NULL THEN 1 ELSE 0 END) as root_menus,
    SUM(CASE WHEN parent_id IS NOT NULL THEN 1 ELSE 0 END) as child_menus
FROM menu_items
WHERE menu_type = 'frontend';

-- ============================================
-- 5. 診斷定時任務系統（church 資料庫）
-- ============================================
SELECT '=== 當前資料庫中的任務 ===' as section;
SELECT id, job_name, job_class, enabled FROM scheduled_jobs ORDER BY id;

SELECT '=== 任務執行記錄統計 ===' as section;
SELECT job_id, COUNT(*) as execution_count, 
       MAX(created_at) as last_execution,
       MIN(created_at) as first_execution
FROM job_executions 
GROUP BY job_id 
ORDER BY job_id;

SELECT '=== 孤立的執行記錄檢查 ===' as section;
SELECT je.* FROM job_executions je 
LEFT JOIN scheduled_jobs sj ON je.job_id = sj.id 
WHERE sj.id IS NULL;

SELECT '=== 任務狀態總覽 ===' as section;
SELECT 
  sj.id, 
  sj.job_name, 
  sj.enabled,
  COUNT(je.id) as execution_count,
  MAX(je.created_at) as last_execution
FROM scheduled_jobs sj 
LEFT JOIN job_executions je ON sj.id = je.job_id 
GROUP BY sj.id, sj.job_name, sj.enabled
ORDER BY sj.id;

