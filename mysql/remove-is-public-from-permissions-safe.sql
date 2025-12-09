-- 移除 permissions 表的 is_public 欄位（安全版本）
-- 因為 URL 的公開訪問應該由 url_permissions 表控制，permissions 表的 is_public 欄位是多餘的

USE church;

-- 使用存儲過程來安全地移除欄位（如果存在）
DELIMITER $$

CREATE PROCEDURE IF NOT EXISTS drop_column_if_exists()
BEGIN
    DECLARE column_exists INT DEFAULT 0;
    
    -- 檢查欄位是否存在
    SELECT COUNT(*) INTO column_exists
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'church'
      AND TABLE_NAME = 'permissions'
      AND COLUMN_NAME = 'is_public';
    
    -- 如果欄位存在，則移除它
    IF column_exists > 0 THEN
        ALTER TABLE permissions DROP COLUMN is_public;
        SELECT '欄位 is_public 已成功移除' AS message;
    ELSE
        SELECT '欄位 is_public 不存在，無需移除' AS message;
    END IF;
END$$

DELIMITER ;

-- 執行存儲過程
CALL drop_column_if_exists();

-- 刪除臨時存儲過程
DROP PROCEDURE IF EXISTS drop_column_if_exists;

-- 驗證欄位已移除
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'church'
  AND TABLE_NAME = 'permissions'
ORDER BY ORDINAL_POSITION;

