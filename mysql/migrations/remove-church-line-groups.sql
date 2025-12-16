-- 移除教會資料庫中的 church_line_groups 表
-- 統一使用個人網站資料庫（qa_tracker）的 line_groups 表管理
-- 教會群組透過 group_code = 'CHURCH_TECH_CONTROL' 識別

USE church;

-- 檢查表是否存在，如果存在則刪除
SET @dbname = DATABASE();
SET @tablename = "church_line_groups";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
  ) > 0,
  "DROP TABLE IF EXISTS church_line_groups;",
  "SELECT 1"
));
PREPARE dropIfExists FROM @preparedStatement;
EXECUTE dropIfExists;
DEALLOCATE PREPARE dropIfExists;
