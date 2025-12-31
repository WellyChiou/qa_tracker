-- 新增前台菜單：小組介紹、最新消息、代禱事項
-- 使用方式：在 MySQL 中執行此 SQL 文件
-- 注意：請先確認資料庫名稱（可能是 church 或 church_db）

USE church;

-- ============================================
-- 1. 插入「小組介紹」菜單（直接顯示，重要頁面）
-- ============================================
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

-- ============================================
-- 2. 插入「資訊服務」父菜單（用於分組）
-- ============================================
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

-- ============================================
-- 3. 插入「最新消息」子菜單（屬於「資訊服務」）
-- ============================================
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

-- ============================================
-- 4. 插入「代禱事項」子菜單（屬於「資訊服務」）
-- ============================================
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
-- 5. 檢查插入結果
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
    menu_type
FROM menu_items
WHERE menu_code IN ('GROUPS', 'INFO_SERVICES', 'ANNOUNCEMENTS', 'PRAYER_REQUESTS')
ORDER BY 
    CASE WHEN parent_id IS NULL THEN order_index ELSE 999 END,
    parent_id,
    order_index;

-- ============================================
-- 6. 查看所有前台菜單（按排序）
-- ============================================
SELECT 
    id,
    menu_code,
    menu_name,
    icon,
    url,
    parent_id,
    order_index,
    is_active
FROM menu_items
WHERE menu_type = 'frontend' 
  AND is_active = 1
ORDER BY 
    CASE WHEN parent_id IS NULL THEN order_index ELSE 999 END,
    parent_id,
    order_index;

