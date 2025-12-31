-- 為 group_persons 表新增角色欄位
-- 使用方式：在 MySQL 中執行此 SQL 文件
-- 注意：請先確認資料庫名稱（可能是 church 或 church_db）

USE church;

-- 檢查並新增 role 欄位
SET @dbname = DATABASE();
SET @tablename = "group_persons";
SET @columnname = "role";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE `", @tablename, "` ADD COLUMN `", @columnname, "` ENUM('MEMBER', 'LEADER', 'ASSISTANT_LEADER') NOT NULL DEFAULT 'MEMBER' COMMENT '角色：MEMBER=一般成員，LEADER=小組長，ASSISTANT_LEADER=實習小組長'")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 為現有記錄設定預設值（如果欄位已存在但有些記錄為 NULL）
UPDATE `group_persons` 
SET `role` = 'MEMBER' 
WHERE `role` IS NULL OR `role` = '';

-- 檢查結果
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @dbname
  AND TABLE_NAME = @tablename
  AND COLUMN_NAME = 'role';

-- 顯示角色統計
SELECT 
    `role`,
    COUNT(*) as count
FROM `group_persons`
GROUP BY `role`;

