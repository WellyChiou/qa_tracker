-- 移除 permissions 表的 is_public 欄位
-- 因為 URL 的公開訪問應該由 url_permissions 表控制，permissions 表的 is_public 欄位是多餘的

USE church;

-- 移除 is_public 欄位
-- 注意：如果欄位不存在，會產生錯誤 "Unknown column 'is_public'"，可以忽略
ALTER TABLE permissions DROP COLUMN is_public;

-- 驗證欄位已移除
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'church'
  AND TABLE_NAME = 'permissions'
ORDER BY ORDINAL_POSITION;

