-- 為 groups 表新增三個欄位：聚會頻率、區分、聚會地點
-- 使用方式：在 MySQL 中執行此 SQL 文件
-- 注意：請先確認資料庫名稱（可能是 church 或 church_db）

USE church;

-- 檢查並新增 meeting_frequency 欄位
SET @dbname = DATABASE();
SET @tablename = "groups";
SET @columnname = "meeting_frequency";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE `", @tablename, "` ADD COLUMN `", @columnname, "` VARCHAR(100) COMMENT '聚會頻率（例如：一週一次、兩週一次、三週一次、四周一次）'")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 檢查並新增 category 欄位
SET @columnname = "category";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE `", @tablename, "` ADD COLUMN `", @columnname, "` VARCHAR(100) COMMENT '區分（例如：學生(國高)、學生(大專)、社青、家萱區、遲靜區）'")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 檢查並新增 meeting_location 欄位
SET @columnname = "meeting_location";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE `", @tablename, "` ADD COLUMN `", @columnname, "` VARCHAR(200) COMMENT '聚會地點（例如：極光基地、榮耀堂）'")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 檢查結果
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    CHARACTER_MAXIMUM_LENGTH,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @dbname
  AND TABLE_NAME = @tablename
  AND COLUMN_NAME IN ('meeting_frequency', 'category', 'meeting_location')
ORDER BY COLUMN_NAME;

