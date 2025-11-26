-- 添加 show_in_dashboard 欄位到 menu_items 表
-- 此欄位用於控制菜單項是否在儀表板的快速訪問網格中顯示

USE qa_tracker;

-- 檢查並添加 show_in_dashboard 欄位（如果不存在）
DELIMITER //
CREATE PROCEDURE AddShowInDashboardColumnIfNotExists()
BEGIN
    IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS 
                   WHERE TABLE_SCHEMA = 'qa_tracker' 
                     AND TABLE_NAME = 'menu_items' 
                     AND COLUMN_NAME = 'show_in_dashboard') THEN
        ALTER TABLE menu_items 
        ADD COLUMN show_in_dashboard TINYINT(1) DEFAULT 1 COMMENT '是否在儀表板快速訪問中顯示（1=顯示，0=不顯示）' AFTER is_active;
    END IF;
END //
DELIMITER ;

CALL AddShowInDashboardColumnIfNotExists();
DROP PROCEDURE AddShowInDashboardColumnIfNotExists;

-- 更新現有菜單項的預設值
-- 預設情況下，有實際 URL 的菜單項顯示在快速訪問中，父菜單（url='#'）不顯示
UPDATE menu_items 
SET show_in_dashboard = 1 
WHERE url IS NOT NULL AND url != '#' AND show_in_dashboard IS NULL;

UPDATE menu_items 
SET show_in_dashboard = 0 
WHERE (url IS NULL OR url = '#') AND show_in_dashboard IS NULL;

