-- 添加 show_in_dashboard 欄位到 menu_items 表
-- 用於控制哪些菜單項顯示在儀表板快速操作中
-- ============================================

USE church;

-- 設置字符集
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 檢查欄位是否存在，如果不存在則添加
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'church' 
    AND TABLE_NAME = 'menu_items' 
    AND COLUMN_NAME = 'show_in_dashboard'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE menu_items ADD COLUMN show_in_dashboard TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''是否在儀表板快速訪問中顯示''',
    'SELECT ''欄位 show_in_dashboard 已存在'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 設置現有菜單的默認值（將服事表管理、人員管理、崗位管理設為顯示）
UPDATE menu_items 
SET show_in_dashboard = 1
WHERE menu_code IN ('ADMIN_SERVICE_SCHEDULE', 'ADMIN_PERSONS', 'ADMIN_POSITIONS')
AND show_in_dashboard IS NULL;

-- 將其他菜單設為不顯示（除非是子菜單，子菜單不應該顯示在儀表板）
UPDATE menu_items 
SET show_in_dashboard = 0
WHERE parent_id IS NOT NULL
AND show_in_dashboard IS NULL;

SELECT 'show_in_dashboard 欄位添加完成！' AS message;

