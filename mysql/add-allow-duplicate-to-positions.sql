-- 為 positions 表新增 allow_duplicate 欄位
-- 用於標記崗位是否允許與其他崗位重複（同一天同一人可以擔任多個崗位）

USE church;

-- 檢查欄位是否已存在，如果不存在則新增
SET @dbname = DATABASE();
SET @tablename = 'positions';
SET @columnname = 'allow_duplicate';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT 1', -- 欄位已存在，不執行任何操作
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' BOOLEAN NOT NULL DEFAULT FALSE COMMENT ''是否允許與其他崗位重複（同一天同一人可以擔任多個崗位）''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 更新現有崗位的 allow_duplicate 值
-- 預設：電腦、混音、燈光不允許重複（false），直播允許重複（true）
UPDATE positions SET allow_duplicate = TRUE WHERE position_code = 'live';
UPDATE positions SET allow_duplicate = FALSE WHERE position_code IN ('computer', 'sound', 'light');

