-- 移除 service_schedules 表的 schedule_data 欄位
-- 新的資料結構已經不再使用這個欄位，改用正規化的表結構

USE church;

-- 檢查欄位是否存在
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'schedule_data 欄位存在，將進行移除'
        ELSE 'schedule_data 欄位不存在，無需移除'
    END as status
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'church'
  AND TABLE_NAME = 'service_schedules'
  AND COLUMN_NAME = 'schedule_data';

-- 移除 schedule_data 欄位（如果存在）
SET @sql = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE service_schedules DROP COLUMN schedule_data',
        'SELECT "schedule_data 欄位不存在，無需移除" as message'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'church'
      AND TABLE_NAME = 'service_schedules'
      AND COLUMN_NAME = 'schedule_data'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 驗證移除結果
SELECT 'schedule_data 欄位移除完成！' as message;
DESCRIBE service_schedules;

