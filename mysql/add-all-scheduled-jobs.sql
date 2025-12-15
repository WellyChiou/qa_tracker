-- ============================================
-- 整合所有排程任務配置
-- ============================================
-- 此腳本整合了所有排程任務的配置
-- 包括：週一服事人員通知、活動過期檢查、主日信息過期檢查、圖片清理
-- ============================================

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- 第一部分：週一服事人員通知任務
-- ============================================

INSERT INTO scheduled_jobs (job_name, job_class, cron_expression, description, enabled) 
VALUES (
    '週一服事人員通知',
    'com.example.helloworld.scheduler.church.ServiceScheduleNotificationScheduler$WeeklyServiceNotificationJob',
    '0 0 10 ? * TUE',
    '每週一早上 10:00 查詢本周六日服事人員，發送 LINE 通知',
    TRUE
)
ON DUPLICATE KEY UPDATE
    job_name = VALUES(job_name),
    cron_expression = VALUES(cron_expression),
    description = VALUES(description);

-- ============================================
-- 第二部分：活動過期檢查任務
-- ============================================

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

-- ============================================
-- 第三部分：主日信息過期檢查任務
-- ============================================

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

-- ============================================
-- 第四部分：圖片清理任務
-- ============================================

INSERT INTO scheduled_jobs (
    job_name, 
    job_class, 
    cron_expression, 
    description, 
    enabled, 
    created_at, 
    updated_at
) VALUES (
    '圖片清理任務',
    'com.example.helloworld.scheduler.church.ImageCleanupScheduler$ImageCleanupJob',
    '0 0 2 ? * MON',  -- 每週一凌晨 2:00
    '清理未使用的圖片文件，避免圖片檔案過多。僅針對「活動（Activities）」和「主日信息（Sunday Messages）」的圖片進行清理。掃描上傳目錄中的圖片文件，與數據庫中活動和主日信息使用的圖片 URL 對比，刪除未使用的圖片。',
    1,
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    cron_expression = '0 0 2 ? * MON',
    description = '清理未使用的圖片文件，避免圖片檔案過多。僅針對「活動（Activities）」和「主日信息（Sunday Messages）」的圖片進行清理。掃描上傳目錄中的圖片文件，與數據庫中活動和主日信息使用的圖片 URL 對比，刪除未使用的圖片。',
    updated_at = NOW();

-- ============================================
-- 第五部分：資料庫自動備份任務
-- ============================================
-- 注意：Cron 表達式預設為每天 02:00，可通過系統設定 backup.schedule_time 修改
-- 啟用狀態可通過系統設定 backup.enabled 控制

INSERT INTO scheduled_jobs (
    job_name,
    job_class,
    cron_expression,
    description,
    enabled,
    created_at,
    updated_at
) VALUES (
    'Church 資料庫自動備份',
    'com.example.helloworld.scheduler.church.DatabaseBackupScheduler$DatabaseBackupJob',
    '0 0 2 * * ?',  -- 每天凌晨 2:00（預設值，可通過系統設定修改）
    '自動備份 church 資料庫（Church 系統）。備份時間和啟用狀態可通過系統維護頁面的系統參數設定進行配置。',
    1,
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    cron_expression = '0 0 2 * * ?',
    description = '自動備份 church 資料庫（Church 系統）。備份時間和啟用狀態可通過系統維護頁面的系統參數設定進行配置。',
    enabled = 1,
    updated_at = NOW();

-- ============================================
-- 第六部分：顯示執行結果
-- ============================================

SELECT '✅ 所有排程任務已添加' AS message;

SELECT 
    id,
    job_name,
    job_class,
    cron_expression,
    description,
    enabled,
    created_at,
    updated_at
FROM scheduled_jobs
WHERE job_class IN (
    'com.example.helloworld.scheduler.church.ServiceScheduleNotificationScheduler$WeeklyServiceNotificationJob',
    'com.example.helloworld.scheduler.church.ActivityExpirationScheduler$ActivityExpirationJob',
    'com.example.helloworld.scheduler.church.SundayMessageExpirationScheduler$SundayMessageExpirationJob',
    'com.example.helloworld.scheduler.church.ImageCleanupScheduler$ImageCleanupJob',
    'com.example.helloworld.scheduler.church.DatabaseBackupScheduler$DatabaseBackupJob'
)
ORDER BY id;

