-- 建立教會前台菜單結構
-- 母菜單：主要導航
-- 子菜單：首頁、關於我們、活動、聯絡我們

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 創建母菜單（前台網站導航 - 僅用於組織子菜單，不會顯示在導航欄）
INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active, description)
VALUES ('FRONTEND_MAIN', '前台網站導航', '🏠', '#', NULL, 1, 'frontend', NULL, 1, '前台網站導航菜單（組織用）');

SET @parent_menu_id = (SELECT id FROM menu_items WHERE menu_code = 'FRONTEND_MAIN' LIMIT 1);

-- 創建子菜單
INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active, description)
VALUES 
('FRONTEND_HOME', '首頁', '🏠', '/', @parent_menu_id, 1, 'frontend', NULL, 1, '首頁'),
('FRONTEND_ABOUT', '關於我們', '📖', '/about', @parent_menu_id, 2, 'frontend', NULL, 1, '關於我們'),
('FRONTEND_ACTIVITIES', '活動', '🎉', '/activities', @parent_menu_id, 3, 'frontend', NULL, 1, '活動資訊'),
('FRONTEND_CONTACT', '聯絡我們', '📧', '/contact', @parent_menu_id, 4, 'frontend', NULL, 1, '聯絡我們');

-- 顯示結果
SELECT 
    '菜單結構已創建' AS message,
    menu_code,
    menu_name,
    url,
    parent_id,
    order_index,
    menu_type
FROM menu_items
WHERE menu_code IN ('FRONTEND_MAIN', 'FRONTEND_HOME', 'FRONTEND_ABOUT', 'FRONTEND_ACTIVITIES', 'FRONTEND_CONTACT')
ORDER BY parent_id IS NULL DESC, order_index;

