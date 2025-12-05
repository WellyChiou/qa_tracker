-- 修正 service_schedules 表結構
-- 移除舊的欄位，確保表結構符合新的設計

USE church;

-- 檢查並移除舊的欄位（如果存在）
SET @sql = (
    SELECT IF(
        COUNT(*) > 0,
        CONCAT('ALTER TABLE service_schedules DROP COLUMN ', GROUP_CONCAT(COLUMN_NAME SEPARATOR ', DROP COLUMN ')),
        'SELECT "所有舊欄位已移除" as message'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'church'
      AND TABLE_NAME = 'service_schedules'
      AND COLUMN_NAME IN ('schedule_date', 'version', 'start_date', 'end_date', 'schedule_data')
);

-- 執行 SQL（如果有多個欄位需要移除，需要分別執行）
-- 先檢查每個欄位是否存在，然後分別移除

-- 移除 schedule_date 欄位（如果存在）
SET @sql1 = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE service_schedules DROP COLUMN schedule_date',
        'SELECT "schedule_date 欄位不存在" as message'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'church'
      AND TABLE_NAME = 'service_schedules'
      AND COLUMN_NAME = 'schedule_date'
);

PREPARE stmt1 FROM @sql1;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

-- 移除 version 欄位（如果存在）
SET @sql2 = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE service_schedules DROP COLUMN version',
        'SELECT "version 欄位不存在" as message'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'church'
      AND TABLE_NAME = 'service_schedules'
      AND COLUMN_NAME = 'version'
);

PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 移除 start_date 欄位（如果存在）
SET @sql3 = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE service_schedules DROP COLUMN start_date',
        'SELECT "start_date 欄位不存在" as message'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'church'
      AND TABLE_NAME = 'service_schedules'
      AND COLUMN_NAME = 'start_date'
);

PREPARE stmt3 FROM @sql3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

-- 移除 end_date 欄位（如果存在）
SET @sql4 = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE service_schedules DROP COLUMN end_date',
        'SELECT "end_date 欄位不存在" as message'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'church'
      AND TABLE_NAME = 'service_schedules'
      AND COLUMN_NAME = 'end_date'
);

PREPARE stmt4 FROM @sql4;
EXECUTE stmt4;
DEALLOCATE PREPARE stmt4;

-- 移除 schedule_data 欄位（如果存在）
SET @sql5 = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE service_schedules DROP COLUMN schedule_data',
        'SELECT "schedule_data 欄位不存在" as message'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'church'
      AND TABLE_NAME = 'service_schedules'
      AND COLUMN_NAME = 'schedule_data'
);

PREPARE stmt5 FROM @sql5;
EXECUTE stmt5;
DEALLOCATE PREPARE stmt5;

-- 驗證表結構
SELECT 'service_schedules 表結構修正完成！' as message;
DESCRIBE service_schedules;

