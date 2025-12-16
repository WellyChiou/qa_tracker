-- 更新 line_group_members 表，新增 is_active 欄位
-- 用於標記成員是否仍在群組中，當人員離開群組時設定為 false，預設為 true

SET @dbname = DATABASE();
SET @tablename = "line_group_members";
SET @columnname = "is_active";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  "ALTER TABLE line_group_members ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否仍在群組中（false 表示已離開）';"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

