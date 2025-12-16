-- 更新 line_groups 表，新增 group_code 欄位
-- 用於區分群組類型：PERSONAL（個人）或 CHURCH_TECH_CONTROL（教會技術控制）

SET @dbname = DATABASE();
SET @tablename = "line_groups";
SET @columnname = "group_code";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  "ALTER TABLE line_groups ADD COLUMN group_code VARCHAR(50) NULL COMMENT '群組代碼：PERSONAL（個人）或 CHURCH_TECH_CONTROL（教會技術控制）';"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;
