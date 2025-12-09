-- 添加教會後台排程管理菜單項目
-- 此腳本會在教會後台的系統管理子菜單中添加排程管理項目

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 查找系統管理父菜單（如果存在）
SET @admin_settings_id = (SELECT id FROM menu_items WHERE menu_code = 'ADMIN_SETTINGS' AND menu_type = 'admin' LIMIT 1);

-- 如果沒有系統管理父菜單，先創建它
INSERT INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active, description)
SELECT 
    'ADMIN_SETTINGS', 
    '系統管理', 
    '⚙️', 
    '#', 
    NULL, 
    99, 
    'admin', 
    'ADMIN_ACCESS', 
    1, 
    '系統管理功能'
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_SETTINGS' AND menu_type = 'admin');

-- 重新獲取系統管理父菜單 ID
SET @admin_settings_id = (SELECT id FROM menu_items WHERE menu_code = 'ADMIN_SETTINGS' AND menu_type = 'admin' LIMIT 1);

-- 添加排程管理菜單項目
INSERT INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active, description)
SELECT 
    'ADMIN_SCHEDULED_JOBS', 
    '排程管理', 
    '⏰', 
    '/admin/scheduled-jobs', 
    @admin_settings_id, 
    6, 
    'admin', 
    'ADMIN_ACCESS', 
    1, 
    '管理定時任務排程'
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_SCHEDULED_JOBS' AND menu_type = 'admin');

-- 顯示結果
SELECT '✅ 教會後台排程管理菜單已添加' AS message;
SELECT 
    id,
    menu_code,
    menu_name,
    icon,
    url,
    parent_id,
    order_index,
    menu_type,
    required_permission,
    is_active
FROM menu_items
WHERE menu_code = 'ADMIN_SCHEDULED_JOBS' AND menu_type = 'admin';

