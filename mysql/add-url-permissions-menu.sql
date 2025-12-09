-- 在後台菜單中添加 URL 權限管理
-- 在 church 資料庫的 menu_items 表中添加

USE church;

-- 插入 URL 權限管理菜單項（作為系統設定的子菜單）
INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active)
SELECT 
    'ADMIN_URL_PERMISSIONS', 
    'URL 權限管理', 
    '🔗', 
    '/admin/url-permissions', 
    (SELECT id FROM menu_items WHERE menu_code = 'ADMIN_SETTINGS' LIMIT 1), 
    5, 
    'admin', 
    'CHURCH_ADMIN', 
    1
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_URL_PERMISSIONS');

-- 顯示設定結果
SELECT 
    'URL 權限管理菜單已添加' AS message,
    menu_code,
    menu_name,
    url,
    order_index
FROM menu_items
WHERE menu_code = 'ADMIN_URL_PERMISSIONS';

