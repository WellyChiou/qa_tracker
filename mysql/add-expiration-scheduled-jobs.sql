-- ============================================
-- 添加活動和主日信息過期檢查排程任務
-- ============================================
-- 這兩個任務會自動檢查並停用過期的活動和主日信息
-- ============================================

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 1. 活動過期檢查任務（每天晚上 23:00）
INSERT INTO scheduled_jobs (
    job_name,
    job_class,
    cron_expression,
    description,
    enabled,
    created_at,
    updated_at
) VALUES (
    '活動過期檢查',
    'com.example.helloworld.scheduler.church.ActivityExpirationScheduler$ActivityExpirationJob',
    '0 0 23 * * ?',
    '每天晚上 23:00 自動檢查並停用過期的活動（活動日期小於今天的活動會被設為不啟用）',
    1,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    cron_expression = VALUES(cron_expression),
    description = VALUES(description),
    enabled = VALUES(enabled),
    updated_at = NOW();

-- 2. 主日信息過期檢查任務（每週一晚上 01:00）
INSERT INTO scheduled_jobs (
    job_name,
    job_class,
    cron_expression,
    description,
    enabled,
    created_at,
    updated_at
) VALUES (
    '主日信息過期檢查',
    'com.example.helloworld.scheduler.church.SundayMessageExpirationScheduler$SundayMessageExpirationJob',
    '0 0 1 ? * MON',
    '每週一晚上 01:00 自動檢查並停用過期的主日信息（服務日期小於今天的主日信息會被設為不啟用）',
    1,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    cron_expression = VALUES(cron_expression),
    description = VALUES(description),
    enabled = VALUES(enabled),
    updated_at = NOW();

-- 顯示插入結果
SELECT 
    '排程任務已創建' AS message,
    id,
    job_name,
    job_class,
    cron_expression,
    description,
    enabled
FROM scheduled_jobs
WHERE job_class IN (
    'com.example.helloworld.scheduler.church.ActivityExpirationScheduler$ActivityExpirationJob',
    'com.example.helloworld.scheduler.church.SundayMessageExpirationScheduler$SundayMessageExpirationJob'
)
ORDER BY id;

