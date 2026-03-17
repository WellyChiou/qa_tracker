-- ============================================
-- 教會系統菜單配置（整合版）
-- 資料庫：church
-- ============================================
-- 此檔案整合了以下檔案：
-- - add-frontend-menus.sql（前台菜單）
-- - add-admin-menus.sql（後台管理菜單）
-- ============================================
-- 重要說明：
-- 後台菜單的 URL 不包含 '/admin' 前綴，因為後台路由的 base path 是 '/church-admin/'
-- 例如：菜單 URL 設為 '/prayer-requests'，實際訪問路徑為 '/church-admin/prayer-requests'
-- ============================================

USE church;

-- ============================================
-- 第一部分：前台菜單
-- ============================================

-- 1. 插入「小組介紹」菜單（直接顯示，重要頁面）
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
) VALUES (
    'GROUPS', 
    '小組介紹', 
    '👥', 
    '/groups', 
    NULL,  -- 直接顯示，無父菜單
    6,     -- 排序：在「主日信息」之後
    'frontend', 
    NULL,  -- 公開訪問
    1,
    '查看各小組資訊'
);

-- 2. 插入「資訊服務」父菜單（用於分組）
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
) VALUES (
    'INFO_SERVICES', 
    '資訊服務', 
    '📢', 
    '#',  -- 父菜單不需要實際 URL
    NULL, -- 根菜單
    7,    -- 排序：在「小組介紹」之後
    'frontend', 
    NULL, -- 公開訪問
    1,
    '資訊服務分組（包含最新消息和代禱事項）'
);

-- 3. 插入「最新消息」子菜單（屬於「資訊服務」）
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
SELECT 
    'ANNOUNCEMENTS', 
    '最新消息', 
    '📰', 
    '/announcements', 
    (SELECT id FROM menu_items WHERE menu_code = 'INFO_SERVICES' LIMIT 1),  -- 父菜單 ID
    1,     -- 在父菜單下的排序
    'frontend', 
    NULL,  -- 公開訪問
    1,
    '查看教會最新公告和重要消息'
WHERE NOT EXISTS (
    SELECT 1 FROM menu_items WHERE menu_code = 'ANNOUNCEMENTS'
);

-- 4. 插入「代禱事項」子菜單（屬於「資訊服務」）
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
SELECT 
    'PRAYER_REQUESTS', 
    '代禱事項', 
    '🙏', 
    '/prayer-requests', 
    (SELECT id FROM menu_items WHERE menu_code = 'INFO_SERVICES' LIMIT 1),  -- 父菜單 ID
    2,     -- 在父菜單下的排序
    'frontend', 
    NULL,  -- 公開訪問
    1,
    '查看代禱事項'
WHERE NOT EXISTS (
    SELECT 1 FROM menu_items WHERE menu_code = 'PRAYER_REQUESTS'
);

-- ============================================
-- 第二部分：後台管理菜單
-- ============================================

-- 1. 確保「前台內容管理」父菜單存在
INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active)
SELECT 'ADMIN_FRONTEND_CONTENT', '前台內容管理', '📝', '#', NULL, 3, 'admin', 'CHURCH_ADMIN', 1
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_FRONTEND_CONTENT');

-- 2. 插入「代禱事項管理」菜單（作為前台內容管理的子菜單）
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

-- 3. 插入「公告管理」菜單（作為前台內容管理的子菜單）
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
