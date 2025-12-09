-- 在個人網站後台菜單中添加 URL 權限管理
-- 在 qa_tracker 資料庫的 menu_items 表中添加

USE qa_tracker;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 插入 URL 權限管理菜單項
INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active)
SELECT 
    'ADMIN_URL_PERMISSIONS', 
    'URL 權限管理', 
    '🔗', 
    '/admin/url-permissions', 
    4, 
    COALESCE((SELECT MAX(order_index) + 1 FROM menu_items WHERE parent_id = 4), 5), 
    '', 
    'ADMIN_ACESS', 
    1
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_URL_PERMISSIONS');

-- 顯示設定結果
SELECT 
    'URL 權限管理菜單已添加' AS message,
    menu_code,
    menu_name,
    url,
    parent_id,
    order_index,
    menu_type
FROM menu_items
WHERE menu_code = 'ADMIN_URL_PERMISSIONS';

