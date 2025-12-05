-- 移除 service_schedules 表中的 position_config 欄位
-- 因為現在使用即時的崗位配置，不需要保存歷史快照

USE church;

-- 移除 position_config 欄位（如果存在）
-- MySQL 不支援 IF EXISTS，所以使用存儲過程來安全地移除
SET @dbname = DATABASE();
SET @tablename = 'service_schedules';
SET @columnname = 'position_config';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  CONCAT('ALTER TABLE ', @tablename, ' DROP COLUMN ', @columnname),
  'SELECT 1'
));
PREPARE alterIfExists FROM @preparedStatement;
EXECUTE alterIfExists;
DEALLOCATE PREPARE alterIfExists;

