USE church;

-- 添加圖片清理定時任務
-- 每週一凌晨 2:00 執行，清理未使用的圖片文件
-- 僅針對「活動（Activities）」和「主日信息（Sunday Messages）」的圖片進行清理
-- 掃描上傳目錄中的圖片文件，與數據庫中活動和主日信息使用的圖片 URL 對比，刪除未使用的圖片
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

