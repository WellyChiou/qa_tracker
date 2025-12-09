-- 還原個人網站（qa_tracker）的 menu_items 資料表
-- 此腳本會重新建立 menu_items 表並插入預設菜單資料

USE qa_tracker;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 1. 建立 menu_items 表（如果不存在）
CREATE TABLE IF NOT EXISTS menu_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    menu_code VARCHAR(50) UNIQUE NOT NULL COMMENT '菜單代碼',
    menu_name VARCHAR(100) NOT NULL COMMENT '菜單名稱',
    icon VARCHAR(50) COMMENT '圖標',
    url VARCHAR(255) COMMENT '菜單連結',
    parent_id BIGINT COMMENT '父菜單 ID',
    order_index INT NOT NULL DEFAULT 0 COMMENT '排序順序',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否啟用',
    show_in_dashboard TINYINT(1) DEFAULT 1 COMMENT '是否在儀表板快速訪問中顯示',
    required_permission VARCHAR(100) COMMENT '需要的權限代碼',
    description VARCHAR(255) COMMENT '菜單描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_menu_code (menu_code),
    INDEX idx_parent_id (parent_id),
    INDEX idx_order_index (order_index),
    INDEX idx_is_active (is_active),
    FOREIGN KEY (parent_id) REFERENCES menu_items(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='個人網站菜單項表';

-- 2. 插入預設菜單資料
-- 注意：使用 INSERT IGNORE 避免重複插入

-- 2.1 頂層菜單
INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description) VALUES
('DASHBOARD', '儀表板', '📊', '/', NULL, 1, 1, 1, NULL, '系統儀表板'),
('EXPENSES', '家庭記帳', '💰', '/expenses', NULL, 2, 1, 1, 'EXPENSES_READ', '家庭記帳系統'),
('TRACKER', 'QA Tracker', '📝', '/tracker', NULL, 3, 1, 1, 'TRACKER_READ', 'QA 工作追蹤系統'),
('ASSETS', '資產管理', '💼', '/assets', NULL, 4, 1, 1, 'ASSETS_READ', '資產管理系統'),
('ADMIN_SETTINGS', '系統管理', '⚙️', '#', NULL, 99, 1, 0, 'ADMIN_ACCESS', '系統管理功能');

-- 2.2 系統管理子菜單
-- 先獲取 ADMIN_SETTINGS 的 ID，然後插入子菜單
SET @admin_settings_id = (SELECT id FROM menu_items WHERE menu_code = 'ADMIN_SETTINGS' LIMIT 1);

INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description)
SELECT 
    'ADMIN_USERS', 
    '用戶管理', 
    '👤', 
    '/admin/users', 
    @admin_settings_id, 
    1, 
    1, 
    0, 
    'ADMIN_ACCESS', 
    '管理系統用戶'
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_USERS');

INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description)
SELECT 
    'ADMIN_ROLES', 
    '角色管理', 
    '🔐', 
    '/admin/roles', 
    @admin_settings_id, 
    2, 
    1, 
    0, 
    'ADMIN_ACCESS', 
    '管理系統角色'
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_ROLES');

INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description)
SELECT 
    'ADMIN_PERMISSIONS', 
    '權限管理', 
    '🔑', 
    '/admin/permissions', 
    @admin_settings_id, 
    3, 
    1, 
    0, 
    'ADMIN_ACCESS', 
    '管理系統權限'
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_PERMISSIONS');

INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description)
SELECT 
    'ADMIN_MENUS', 
    '菜單管理', 
    '📑', 
    '/admin/menus', 
    @admin_settings_id, 
    4, 
    1, 
    0, 
    'ADMIN_ACCESS', 
    '管理系統菜單'
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_MENUS');

INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description)
SELECT 
    'ADMIN_URL_PERMISSIONS', 
    'URL 權限管理', 
    '🔗', 
    '/admin/url-permissions', 
    @admin_settings_id, 
    5, 
    1, 
    0, 
    'ADMIN_ACCESS', 
    '管理 URL 權限'
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_URL_PERMISSIONS');

INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description)
SELECT 
    'ADMIN_SCHEDULED_JOBS', 
    '排程管理', 
    '⏰', 
    '/admin/scheduled-jobs', 
    @admin_settings_id, 
    6, 
    1, 
    0, 
    'ADMIN_ACCESS', 
    '管理定時任務排程'
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_SCHEDULED_JOBS');

-- 3. 顯示還原結果
SELECT '✅ menu_items 表已還原' AS message;
SELECT 
    COUNT(*) AS total_menus,
    SUM(CASE WHEN parent_id IS NULL THEN 1 ELSE 0 END) AS root_menus,
    SUM(CASE WHEN parent_id IS NOT NULL THEN 1 ELSE 0 END) AS child_menus,
    SUM(CASE WHEN is_active = 1 THEN 1 ELSE 0 END) AS active_menus
FROM menu_items;

-- 4. 顯示所有菜單項目
SELECT 
    id,
    menu_code,
    menu_name,
    icon,
    url,
    parent_id,
    order_index,
    is_active,
    show_in_dashboard,
    required_permission,
    description
FROM menu_items
ORDER BY 
    COALESCE(parent_id, id),
    order_index;

