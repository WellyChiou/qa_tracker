-- ============================================
-- 整合所有資料庫結構變更
-- ============================================
-- 此腳本整合了所有資料庫結構變更（Migration）
-- 包括：LINE 用戶 ID 欄位、服事表年份欄位
-- ============================================

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- 第一部分：添加 LINE Bot 相關字段到 users 表
-- ============================================

-- 檢查並添加 line_user_id 字段（如果不存在）
SET @dbname = DATABASE();
SET @tablename = "users";
SET @columnname = "line_user_id";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " VARCHAR(50) UNIQUE COMMENT 'LINE 用戶 ID，用於 LINE Bot 綁定'")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加索引（如果不存在）
SET @indexname = "idx_line_user_id";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (index_name = @indexname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD INDEX ", @indexname, " (line_user_id)")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- ============================================
-- 第二部分：為 service_schedules 表添加 year 欄位和唯一約束
-- ============================================

-- 1. 添加 year 欄位（允許 NULL，以便為現有資料填充）
-- 檢查欄位是否已存在
SET @columnname = "year";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = 'service_schedules')
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  "ALTER TABLE service_schedules ADD COLUMN year INT COMMENT '年度（例如：2024）' AFTER name"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 2. 為現有資料填充年度
-- 從 dates 關聯表中提取最早的日期作為年度依據
-- 如果沒有 dates，則使用 created_at 的年份
UPDATE service_schedules ss
LEFT JOIN (
    SELECT service_schedule_id, MIN(date) as min_date
    FROM service_schedule_dates
    GROUP BY service_schedule_id
) ssd ON ssd.service_schedule_id = ss.id
SET ss.year = COALESCE(
    YEAR(ssd.min_date),  -- 從日期明細表中提取最早日期的年份
    YEAR(ss.created_at)  -- 如果沒有日期明細，使用建立時間的年份
)
WHERE ss.year IS NULL;

-- 如果還有 NULL 值（沒有 dates 也沒有 created_at），使用當前年份
UPDATE service_schedules 
SET year = YEAR(CURDATE())
WHERE year IS NULL;

-- 3. 設置 year 欄位為 NOT NULL（如果欄位存在且允許 NULL）
SET @preparedStatement = (SELECT IF(
  (
    SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = 'service_schedules')
      AND (table_schema = @dbname)
      AND (column_name = 'year')
  ) = 'YES',
  "ALTER TABLE service_schedules MODIFY COLUMN year INT NOT NULL COMMENT '年度（例如：2024）'",
  "SELECT 1"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 4. 添加唯一約束，確保每個年度只有一個版本（如果不存在）
SET @constraintname = "uk_year";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE
      (table_name = 'service_schedules')
      AND (table_schema = @dbname)
      AND (constraint_name = @constraintname)
  ) > 0,
  "SELECT 1",
  "ALTER TABLE service_schedules ADD UNIQUE KEY uk_year (year)"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 5. 添加索引以提升查詢效能（如果不存在）
SET @indexname = "idx_year";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (table_name = 'service_schedules')
      AND (table_schema = @dbname)
      AND (index_name = @indexname)
  ) > 0,
  "SELECT 1",
  "ALTER TABLE service_schedules ADD INDEX idx_year (year)"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- ============================================
-- 第三部分：顯示執行結果
-- ============================================

SELECT '✅ 所有資料庫結構變更已完成' AS message;

-- 顯示 users 表的 line_user_id 欄位
SELECT 
    'users 表結構' AS section,
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'users'
  AND COLUMN_NAME = 'line_user_id';

-- 顯示 service_schedules 表的 year 欄位
SELECT 
    'service_schedules 表結構' AS section,
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'service_schedules'
  AND COLUMN_NAME = 'year';

-- 顯示 service_schedules 表的統計資訊
SELECT 
    'service_schedules 統計' AS section,
    COUNT(*) AS total_schedules, 
    COUNT(DISTINCT year) AS distinct_years,
    GROUP_CONCAT(DISTINCT year ORDER BY year) AS years
FROM service_schedules;

