-- 檢查教會前台菜單配置
-- 使用方式：在 MySQL 中執行此 SQL 文件

USE church_db;

-- 1. 檢查所有前台菜單（包括啟用和未啟用的）
SELECT 
    id,
    menu_code,
    menu_name,
    icon,
    url,
    parent_id,
    order_index,
    is_active,
    menu_type,
    required_permission,
    description,
    created_at,
    updated_at
FROM menu_items
WHERE menu_type = 'frontend'
ORDER BY order_index, id;

-- 2. 只檢查啟用的前台菜單（這應該是 API 返回的數據）
SELECT 
    id,
    menu_code,
    menu_name,
    icon,
    url,
    parent_id,
    order_index,
    is_active,
    menu_type,
    required_permission,
    description
FROM menu_items
WHERE menu_type = 'frontend' 
  AND is_active = 1
  AND parent_id IS NULL  -- 只查詢根菜單（後端會自動載入子菜單）
ORDER BY order_index, id;

-- 3. 檢查是否有前台菜單但被停用了
SELECT 
    COUNT(*) as inactive_count,
    GROUP_CONCAT(menu_name) as inactive_menus
FROM menu_items
WHERE menu_type = 'frontend' 
  AND is_active = 0;

-- 4. 檢查前台菜單的權限設定（應該為 NULL 或空字符串，才能公開訪問）
SELECT 
    id,
    menu_code,
    menu_name,
    required_permission,
    CASE 
        WHEN required_permission IS NULL OR required_permission = '' THEN '✅ 公開訪問（正確）'
        ELSE '❌ 需要權限（前台菜單應該為 NULL）'
    END as permission_status
FROM menu_items
WHERE menu_type = 'frontend';

-- 5. 檢查前台菜單的 URL 設定
SELECT 
    id,
    menu_code,
    menu_name,
    url,
    CASE 
        WHEN url IS NULL OR url = '' THEN '❌ URL 為空'
        WHEN url = '#' THEN '⚠️ URL 為 #（可能不正確）'
        ELSE '✅ URL 已設定'
    END as url_status
FROM menu_items
WHERE menu_type = 'frontend';

-- 6. 統計前台菜單數量
SELECT 
    COUNT(*) as total_frontend_menus,
    SUM(CASE WHEN is_active = 1 THEN 1 ELSE 0 END) as active_menus,
    SUM(CASE WHEN is_active = 0 THEN 1 ELSE 0 END) as inactive_menus,
    SUM(CASE WHEN parent_id IS NULL THEN 1 ELSE 0 END) as root_menus,
    SUM(CASE WHEN parent_id IS NOT NULL THEN 1 ELSE 0 END) as child_menus
FROM menu_items
WHERE menu_type = 'frontend';

-- 7. 如果沒有前台菜單，可以使用以下 SQL 插入預設前台菜單
-- 注意：這會插入預設菜單，如果已經有菜單請不要執行
/*
INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active) VALUES
('HOME', '首頁', '🏠', '/', NULL, 1, 'frontend', NULL, 1),
('ABOUT', '關於我們', 'ℹ️', '/about', NULL, 2, 'frontend', NULL, 1),
('ACTIVITIES', '活動', '📅', '/activities', NULL, 3, 'frontend', NULL, 1),
('SERVICE_SCHEDULE', '服事安排', '📋', '/service-schedule', NULL, 4, 'frontend', NULL, 1),
('CONTACT', '聯絡我們', '📧', '/contact', NULL, 5, 'frontend', NULL, 1);
*/

-- 8. 修復常見問題：如果前台菜單的 required_permission 有設定，將其設為 NULL
-- 注意：執行前請先備份資料庫
/*
UPDATE menu_items 
SET required_permission = NULL 
WHERE menu_type = 'frontend' 
  AND required_permission IS NOT NULL 
  AND required_permission != '';
*/

-- 9. 修復常見問題：如果前台菜單被停用，將其啟用
-- 注意：執行前請先備份資料庫
/*
UPDATE menu_items 
SET is_active = 1 
WHERE menu_type = 'frontend' 
  AND is_active = 0;
*/


