-- 檢查和更新 users 表結構
-- 確保包含所有必要的欄位以支援 LINE Bot 功能

USE qa_tracker;

-- 檢查現有表結構
DESCRIBE users;

-- 添加缺失的欄位（帶條件檢查）

-- 1. 添加 line_user_id 欄位（LINE Bot 功能必需）
SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND COLUMN_NAME = 'line_user_id'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE users ADD COLUMN line_user_id VARCHAR(50) UNIQUE COMMENT "LINE 用戶 ID，用於 LINE Bot 綁定"',
    'SELECT "line_user_id column already exists" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 檢查並添加必要的索引
SET @index_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND INDEX_NAME = 'idx_line_user_id'
);

SET @sql = IF(@index_exists = 0,
    'ALTER TABLE users ADD INDEX idx_line_user_id (line_user_id)',
    'SELECT "idx_line_user_id index already exists" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 驗證最終表結構
DESCRIBE users;

-- 4. 顯示一些示例數據（如果有的話）
SELECT
    uid,
    email,
    username,
    display_name,
    line_user_id,
    created_at
FROM users
LIMIT 5;

SELECT 'Users table structure updated successfully for LINE Bot integration' as status;
