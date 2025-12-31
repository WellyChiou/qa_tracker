-- 新增後台管理菜單：代禱事項管理、公告管理
-- 使用方式：在 MySQL 中執行此 SQL 文件
-- 注意：請先確認資料庫名稱（可能是 church 或 church_db）
--
-- 重要說明：
-- 後台菜單的 URL 不包含 '/admin' 前綴，因為後台路由的 base path 是 '/church-admin/'
-- 例如：菜單 URL 設為 '/prayer-requests'，實際訪問路徑為 '/church-admin/prayer-requests'

USE church;

-- ============================================
-- 1. 確保「前台內容管理」父菜單存在
-- ============================================
INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active)
SELECT 'ADMIN_FRONTEND_CONTENT', '前台內容管理', '📝', '#', NULL, 3, 'admin', 'CHURCH_ADMIN', 1
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_FRONTEND_CONTENT');

-- ============================================
-- 2. 插入「代禱事項管理」菜單（作為前台內容管理的子菜單）
-- ============================================
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
SELECT
    'ADMIN_PRAYER_REQUESTS', 
    '代禱事項管理', 
    '🙏', 
    '/prayer-requests', 
    (SELECT id FROM menu_items WHERE menu_code = 'ADMIN_FRONTEND_CONTENT' LIMIT 1), 
    5,  -- 在主日信息管理之後
    'admin', 
    'PRAYER_REQUEST_EDIT', 
    1, 
    '管理代禱事項（新增、編輯、刪除代禱事項）',
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_PRAYER_REQUESTS')
ON DUPLICATE KEY UPDATE 
  menu_name = VALUES(menu_name),
  icon = VALUES(icon),
  url = VALUES(url),
  order_index = VALUES(order_index),
  description = VALUES(description),
  updated_at = NOW();

-- ============================================
-- 3. 插入「公告管理」菜單（作為前台內容管理的子菜單）
-- ============================================
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
SELECT
    'ADMIN_ANNOUNCEMENTS', 
    '公告管理', 
    '📰', 
    '/announcements', 
    (SELECT id FROM menu_items WHERE menu_code = 'ADMIN_FRONTEND_CONTENT' LIMIT 1), 
    6,  -- 在代禱事項管理之後
    'admin', 
    'ANNOUNCEMENT_EDIT', 
    1, 
    '管理公告（新增、編輯、刪除公告，支援置頂、發布日期、到期日期）',
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_ANNOUNCEMENTS')
ON DUPLICATE KEY UPDATE 
  menu_name = VALUES(menu_name),
  icon = VALUES(icon),
  url = VALUES(url),
  order_index = VALUES(order_index),
  description = VALUES(description),
  updated_at = NOW();

-- ============================================
-- 4. 檢查插入結果
-- ============================================
SELECT 
    id,
    menu_code,
    menu_name,
    icon,
    url,
    parent_id,
    (SELECT menu_name FROM menu_items m2 WHERE m2.id = menu_items.parent_id) as parent_name,
    order_index,
    is_active,
    menu_type,
    required_permission
FROM menu_items
WHERE menu_code IN ('ADMIN_PRAYER_REQUESTS', 'ADMIN_ANNOUNCEMENTS', 'ADMIN_FRONTEND_CONTENT')
ORDER BY 
    CASE WHEN parent_id IS NULL THEN order_index ELSE 999 END,
    parent_id,
    order_index;

-- ============================================
-- 5. 查看「前台內容管理」下的所有子菜單
-- ============================================
SELECT 
    id,
    menu_code,
    menu_name,
    icon,
    url,
    order_index,
    required_permission
FROM menu_items
WHERE parent_id = (SELECT id FROM menu_items WHERE menu_code = 'ADMIN_FRONTEND_CONTENT' LIMIT 1)
  AND menu_type = 'admin'
ORDER BY order_index;

