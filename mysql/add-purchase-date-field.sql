-- 添加購買日期欄位到 assets 表（安全版本）
-- 執行此 SQL 以更新現有數據庫
-- 此版本會先檢查欄位是否存在，避免重複添加

USE qa_tracker;

-- 檢查欄位是否存在，如果不存在則添加
SET @dbname = DATABASE();
SET @tablename = 'assets';
SET @columnname = 'purchase_date';
SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
        WHERE
            (TABLE_SCHEMA = @dbname)
            AND (TABLE_NAME = @tablename)
            AND (COLUMN_NAME = @columnname)
    ) > 0,
    'SELECT 1', -- 欄位已存在，不執行任何操作
    CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' DATE COMMENT ''購買日期'' AFTER current_price')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;
