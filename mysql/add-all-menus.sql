-- ============================================
-- 整合所有菜單配置
-- ============================================
-- 此腳本整合了所有菜單項的配置
-- 包括：主日信息、排程管理、系統維護
-- ============================================

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- 第一部分：主日信息菜單
-- ============================================

-- 1. 插入前台菜單項
INSERT INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active, created_at, updated_at)
VALUES ('SUNDAY_MESSAGES', '主日信息', '📖', '/sunday-messages', NULL, 4, 'frontend', NULL, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
  menu_name = VALUES(menu_name),
  icon = VALUES(icon),
  url = VALUES(url),
  order_index = VALUES(order_index),
  updated_at = NOW();

-- 2. 獲取「前台內容管理」母菜單的 ID
SET @parent_menu_id = (SELECT id FROM menu_items WHERE menu_code = 'ADMIN_FRONTEND_CONTENT' LIMIT 1);

-- 3. 插入後台管理菜單項（作為「前台內容管理」的子菜單）
INSERT INTO menu_items (
    menu_code, 
    menu_name, 
    icon, 
    url, 
    parent_id, 
    order_index, 
    menu_type, 
    required_permission, 
    is_active, 
    description,
    created_at,
    updated_at
)
VALUES (
    'ADMIN_SUNDAY_MESSAGES', 
    '主日信息管理', 
    '📖', 
    '/admin/sunday-messages', 
    @parent_menu_id, 
    4,  -- 在活動管理之後
    'admin', 
    'SUNDAY_MESSAGE_EDIT', 
    1, 
    '管理主日信息（DM圖片、講題、經文、講員等）',
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE 
  menu_name = VALUES(menu_name),
  icon = VALUES(icon),
  url = VALUES(url),
  order_index = VALUES(order_index),
  description = VALUES(description),
  updated_at = NOW();

-- ============================================
-- 第二部分：排程管理菜單
-- ============================================

-- 查找系統設定父菜單（如果存在）
SET @admin_settings_id = (SELECT id FROM menu_items WHERE menu_code = 'ADMIN_SETTINGS' AND menu_type = 'admin' LIMIT 1);

-- 如果沒有系統設定父菜單，先創建它
INSERT INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active, description)
SELECT 
    'ADMIN_SETTINGS', 
    '系統設定', 
    '⚙️', 
    '#', 
    NULL, 
    99, 
    'admin', 
    'CHURCH_ADMIN', 
    1, 
    '系統設定功能'
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_SETTINGS' AND menu_type = 'admin');

-- 重新獲取系統設定父菜單 ID
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
    'CHURCH_ADMIN', 
    1, 
    '管理定時任務排程'
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_SCHEDULED_JOBS' AND menu_type = 'admin');

-- ============================================
-- 第三部分：系統維護菜單
-- ============================================

-- 添加系統維護菜單項目
INSERT INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active, description)
SELECT 
    'ADMIN_MAINTENANCE', 
    '系統維護', 
    '🔧', 
    '/admin/maintenance', 
    @admin_settings_id, 
    5, 
    'admin', 
    'CHURCH_ADMIN', 
    1, 
    '系統參數設定與備份管理'
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_MAINTENANCE' AND menu_type = 'admin');

-- ============================================
-- 第四部分：顯示執行結果
-- ============================================

SELECT '✅ 所有菜單配置已添加' AS message;

-- 顯示主日信息菜單
SELECT 
    '主日信息菜單' AS section,
    menu_code,
    menu_name,
    url,
    menu_type,
    (SELECT menu_name FROM menu_items WHERE id = menu_items.parent_id) AS parent_menu_name,
    order_index,
    is_active
FROM menu_items
WHERE menu_code IN ('SUNDAY_MESSAGES', 'ADMIN_SUNDAY_MESSAGES')
ORDER BY menu_type, order_index;

-- 顯示排程管理菜單
SELECT 
    '排程管理菜單' AS section,
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

-- 顯示系統維護菜單
SELECT 
    '系統維護菜單' AS section,
    id,
    menu_code,
    menu_name,
    icon,
    url,
    parent_id,
    order_index,
    menu_type,
    required_permission,
    is_active,
    description
FROM menu_items
WHERE menu_code = 'ADMIN_MAINTENANCE' AND menu_type = 'admin';

