-- 更新 line_groups 表，新增 member_count 欄位
-- MySQL 5.7+ 支援 ADD COLUMN IF NOT EXISTS 語法可能受限，這裡改用存儲過程或直接添加（如果報錯說明已存在）
-- 嘗試直接添加，如果已存在會報錯但無害（或者可以先檢查）

-- 這裡提供一個更兼容的寫法：
-- 如果 member_count 不存在則添加
SET @dbname = DATABASE();
SET @tablename = "line_groups";
SET @columnname = "member_count";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  "ALTER TABLE line_groups ADD COLUMN member_count INT DEFAULT 0 COMMENT '群組人數';"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 建立 line_group_members 表
CREATE TABLE IF NOT EXISTS line_group_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    group_id VARCHAR(100) NOT NULL COMMENT '群組 ID',
    user_id VARCHAR(100) NOT NULL COMMENT 'LINE User ID',
    display_name VARCHAR(255) COMMENT '成員顯示名稱',
    is_admin BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否為管理員',
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入時間',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    
    INDEX idx_group_id (group_id),
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_group_user (group_id, user_id),
    FOREIGN KEY (group_id) REFERENCES line_groups(group_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LINE 群組成員表';

