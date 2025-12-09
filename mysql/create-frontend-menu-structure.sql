-- 建立教會前台菜單結構
-- 前台網站的導航菜單是從資料庫動態載入的
-- 直接建立子菜單，不需要母菜單（前台導航欄只顯示這些菜單項目）

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 創建前台菜單（直接建立，parent_id 為 NULL）
INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active, description)
VALUES 
('FRONTEND_HOME', '首頁', '🏠', '/', NULL, 1, 'frontend', NULL, 1, '首頁'),
('FRONTEND_ABOUT', '關於我們', '📖', '/about', NULL, 2, 'frontend', NULL, 1, '關於我們'),
('FRONTEND_ACTIVITIES', '活動', '🎉', '/activities', NULL, 3, 'frontend', NULL, 1, '活動資訊'),
('FRONTEND_CONTACT', '聯絡我們', '📧', '/contact', NULL, 4, 'frontend', NULL, 1, '聯絡我們');

-- 顯示結果
SELECT 
    '前台菜單結構已創建' AS message,
    menu_code,
    menu_name,
    url,
    parent_id,
    order_index,
    menu_type
FROM menu_items
WHERE menu_code IN ('FRONTEND_HOME', 'FRONTEND_ABOUT', 'FRONTEND_ACTIVITIES', 'FRONTEND_CONTACT')
ORDER BY order_index;
