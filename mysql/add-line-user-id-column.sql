-- 添加 LINE Bot 相關字段到 users 表
-- 用於 LINE Bot 用戶綁定和通知功能

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

-- 說明：此腳本安全地添加 LINE Bot 功能所需的資料庫欄位
-- line_user_id: 用於存儲從 LINE 平台獲取的用戶唯一識別碼
-- 當用戶在網頁應用中綁定 LINE 帳號時，此字段會被設置
-- LINE Bot 可以通過此字段識別用戶並發送個人化通知
