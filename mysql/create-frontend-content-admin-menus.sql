-- 建立前台內容管理的後台菜單
-- 這些菜單用於後台管理系統，讓管理員可以編輯前台顯示的內容
-- 包括：教會資訊、關於我們、活動、聯絡表單

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 1. 創建「前台內容管理」母菜單
INSERT IGNORE INTO menu_items (
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
)
VALUES (
    'ADMIN_FRONTEND_CONTENT', 
    '前台內容管理', 
    '🌐', 
    '#', 
    NULL, 
    3, 
    'admin', 
    'PERM_CHURCH_ADMIN', 
    1, 
    '前台網站內容管理（教會資訊、關於我們、活動、聯絡表單）'
);

-- 2. 獲取「前台內容管理」母菜單的 ID
SET @parent_menu_id = (SELECT id FROM menu_items WHERE menu_code = 'ADMIN_FRONTEND_CONTENT' LIMIT 1);

-- 3. 創建子菜單
INSERT IGNORE INTO menu_items (
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
)
VALUES 
-- 教會資訊管理
('ADMIN_CHURCH_INFO', '教會資訊', '🏛️', '/admin/church-info', @parent_menu_id, 1, 'admin', 'PERM_CHURCH_ADMIN', 1, '管理教會基本資訊（地址、電話、郵件、服務時間等）'),
-- 關於我們管理
('ADMIN_ABOUT_INFO', '關於我們', '📖', '/admin/about-info', @parent_menu_id, 2, 'admin', 'PERM_CHURCH_ADMIN', 1, '管理關於我們頁面內容（使命、願景、價值等）'),
-- 活動管理
('ADMIN_ACTIVITIES', '活動管理', '🎉', '/admin/activities', @parent_menu_id, 3, 'admin', 'PERM_CHURCH_ADMIN', 1, '管理活動資訊'),
-- 聯絡表單管理
('ADMIN_CONTACT_SUBMISSIONS', '聯絡表單', '📧', '/admin/contact-submissions', @parent_menu_id, 4, 'admin', 'PERM_CHURCH_ADMIN', 1, '查看和管理聯絡表單提交記錄');

-- 顯示結果
SELECT 
    '前台內容管理菜單已創建' AS message,
    menu_code,
    menu_name,
    url,
    parent_id,
    (SELECT menu_name FROM menu_items WHERE id = menu_items.parent_id) AS parent_menu_name,
    order_index,
    menu_type,
    required_permission
FROM menu_items
WHERE menu_code IN ('ADMIN_FRONTEND_CONTENT', 'ADMIN_CHURCH_INFO', 'ADMIN_ABOUT_INFO', 'ADMIN_ACTIVITIES', 'ADMIN_CONTACT_SUBMISSIONS')
ORDER BY parent_id IS NULL DESC, order_index;

