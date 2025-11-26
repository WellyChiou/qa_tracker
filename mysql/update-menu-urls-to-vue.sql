-- 更新菜單 URL 從 HTML 路徑改為 Vue Router 路徑
-- 執行此腳本後，菜單將指向新的 Vue 3 路由

USE qa_tracker;

-- 更新記帳系統菜單
UPDATE menu_items 
SET url = '/expenses' 
WHERE menu_code = 'EXPENSES' AND url = '/expenses.html';

-- 更新 QA Tracker 菜單
UPDATE menu_items 
SET url = '/tracker' 
WHERE menu_code = 'TRACKER' AND url = '/tracker.html';

-- 更新用戶管理菜單
UPDATE menu_items 
SET url = '/admin/users' 
WHERE menu_code = 'ADMIN_USERS' AND url = '/admin/users.html';

-- 更新角色管理菜單
UPDATE menu_items 
SET url = '/admin/roles' 
WHERE menu_code = 'ADMIN_ROLES' AND url = '/admin/roles.html';

-- 更新權限管理菜單
UPDATE menu_items 
SET url = '/admin/permissions' 
WHERE menu_code = 'ADMIN_PERMISSIONS' AND url = '/admin/permissions.html';

-- 更新菜單管理菜單
UPDATE menu_items 
SET url = '/admin/menus' 
WHERE menu_code = 'ADMIN_MENUS' AND url = '/admin/menus.html';

-- 更新 URL 權限管理菜單
UPDATE menu_items 
SET url = '/admin/url-permissions' 
WHERE menu_code = 'ADMIN_URL_PERMISSIONS' AND url = '/admin/url-permissions.html';

-- 通用更新：移除所有 .html 後綴（如果還有其他菜單）
UPDATE menu_items 
SET url = REPLACE(url, '.html', '') 
WHERE url LIKE '%.html';

-- 顯示更新結果
SELECT 
    menu_code,
    menu_name,
    url,
    CASE 
        WHEN url LIKE '%.html' THEN '⚠️ 仍需更新'
        ELSE '✅ 已更新'
    END AS status
FROM menu_items
WHERE url IS NOT NULL AND url != '#'
ORDER BY order_index;

