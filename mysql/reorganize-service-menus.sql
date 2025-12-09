-- 重組服事相關菜單
-- 將「服事表管理」、「人員管理」、「崗位管理」收納到「服事管理」母菜單下
-- ============================================

USE church;

-- 設置字符集
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 1. 創建「服事管理」母菜單（如果不存在）
INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active)
VALUES ('ADMIN_SERVICE_MANAGEMENT', '服事管理', '📅', '#', NULL, 2, 'admin', 'SERVICE_SCHEDULE_READ', 1);

-- 2. 獲取「服事管理」母菜單的 ID
SET @parent_menu_id = (SELECT id FROM menu_items WHERE menu_code = 'ADMIN_SERVICE_MANAGEMENT' LIMIT 1);

-- 3. 更新「服事表管理」菜單，設置 parent_id 為「服事管理」
UPDATE menu_items 
SET parent_id = @parent_menu_id,
    order_index = 1
WHERE menu_code = 'ADMIN_SERVICE_SCHEDULE';

-- 4. 更新「人員管理」菜單，設置 parent_id 為「服事管理」
UPDATE menu_items 
SET parent_id = @parent_menu_id,
    order_index = 2
WHERE menu_code = 'ADMIN_PERSONS';

-- 5. 更新「崗位管理」菜單，設置 parent_id 為「服事管理」
UPDATE menu_items 
SET parent_id = @parent_menu_id,
    order_index = 3
WHERE menu_code = 'ADMIN_POSITIONS';

-- 6. 更新其他菜單的 order_index，確保排序正確
-- 「系統設定」保持在最後
UPDATE menu_items 
SET order_index = 99
WHERE menu_code = 'ADMIN_SETTINGS';

SELECT '服事相關菜單重組完成！' AS message;
SELECT 
    menu_code,
    menu_name,
    parent_id,
    (SELECT menu_name FROM menu_items WHERE id = menu_items.parent_id) AS parent_menu_name,
    order_index
FROM menu_items 
WHERE menu_type = 'admin' 
ORDER BY order_index;

