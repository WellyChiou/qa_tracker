-- 添加「主日信息」菜單（前台和後台）
USE church_db;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

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

-- 顯示結果
SELECT 
    '主日信息菜單已創建' AS message,
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

