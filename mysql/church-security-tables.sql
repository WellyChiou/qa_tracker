-- 教會系統權限相關表
-- 在 church 資料庫中建立獨立的權限系統

USE church;

-- 1. 使用者表（教會系統專用）
CREATE TABLE IF NOT EXISTS users (
    uid VARCHAR(128) PRIMARY KEY COMMENT '用戶 UID',
    email VARCHAR(255) UNIQUE COMMENT '電子郵件',
    username VARCHAR(100) UNIQUE COMMENT '用戶名（用於登入）',
    password VARCHAR(255) COMMENT '加密後的密碼',
    display_name VARCHAR(255) COMMENT '顯示名稱',
    photo_url TEXT COMMENT '頭像 URL',
    provider_id VARCHAR(50) DEFAULT 'local' COMMENT '登入提供者（如 local, firebase, google 等）',
    is_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '帳號是否啟用',
    is_account_non_locked TINYINT(1) NOT NULL DEFAULT 1 COMMENT '帳號是否未鎖定',
    last_login_at DATETIME COMMENT '最後登入時間',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_email (email),
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教會系統使用者表';

-- 2. 角色表（教會系統專用）
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    role_name VARCHAR(50) UNIQUE NOT NULL COMMENT '角色名稱',
    description VARCHAR(255) COMMENT '角色描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_role_name (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教會系統角色表';

-- 3. 權限表（教會系統專用）
CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    permission_code VARCHAR(100) UNIQUE NOT NULL COMMENT '權限代碼',
    permission_name VARCHAR(100) NOT NULL COMMENT '權限名稱',
    resource VARCHAR(100) COMMENT '資源名稱',
    action VARCHAR(50) COMMENT '操作類型',
    description VARCHAR(255) COMMENT '權限描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_permission_code (permission_code),
    INDEX idx_resource (resource)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教會系統權限表';

-- 4. 用戶角色關聯表
CREATE TABLE IF NOT EXISTS user_roles (
    user_uid VARCHAR(128) NOT NULL COMMENT '用戶 UID',
    role_id BIGINT NOT NULL COMMENT '角色 ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    PRIMARY KEY (user_uid, role_id),
    FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    INDEX idx_user_uid (user_uid),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教會系統用戶角色關聯表';

-- 5. 角色權限關聯表
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL COMMENT '角色 ID',
    permission_id BIGINT NOT NULL COMMENT '權限 ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教會系統角色權限關聯表';

-- 6. 用戶權限關聯表（直接分配給用戶的權限）
CREATE TABLE IF NOT EXISTS user_permissions (
    user_uid VARCHAR(128) NOT NULL COMMENT '用戶 UID',
    permission_id BIGINT NOT NULL COMMENT '權限 ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    PRIMARY KEY (user_uid, permission_id),
    FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    INDEX idx_user_uid (user_uid),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教會系統用戶權限關聯表';

-- 7. 菜單項表（教會系統專用，包含前台和後台菜單）
CREATE TABLE IF NOT EXISTS menu_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    menu_code VARCHAR(50) UNIQUE NOT NULL COMMENT '菜單代碼',
    menu_name VARCHAR(100) NOT NULL COMMENT '菜單名稱',
    icon VARCHAR(50) COMMENT '圖標',
    url VARCHAR(255) COMMENT '菜單連結',
    parent_id BIGINT COMMENT '父菜單 ID',
    order_index INT NOT NULL DEFAULT 0 COMMENT '排序順序',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否啟用',
    menu_type VARCHAR(20) NOT NULL DEFAULT 'frontend' COMMENT '菜單類型：frontend=前台, admin=後台',
    required_permission VARCHAR(100) COMMENT '需要的權限代碼',
    description VARCHAR(255) COMMENT '菜單描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_menu_code (menu_code),
    INDEX idx_parent_id (parent_id),
    INDEX idx_order_index (order_index),
    INDEX idx_menu_type (menu_type),
    FOREIGN KEY (parent_id) REFERENCES menu_items(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教會系統菜單項表';

-- 8. URL 權限表（教會系統專用）
CREATE TABLE IF NOT EXISTS url_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    url_pattern VARCHAR(255) NOT NULL COMMENT 'URL 模式',
    http_method VARCHAR(10) COMMENT 'HTTP 方法（NULL 表示所有方法）',
    is_public TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否公開（無需認證）',
    required_role VARCHAR(50) COMMENT '需要的角色',
    required_permission VARCHAR(100) COMMENT '需要的權限代碼',
    description VARCHAR(255) COMMENT '描述',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否啟用',
    order_index INT NOT NULL DEFAULT 0 COMMENT '排序順序（數字越小優先級越高）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_url_pattern (url_pattern),
    INDEX idx_is_active (is_active),
    INDEX idx_order_index (order_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教會系統 URL 權限表';

-- 9. 插入預設角色
INSERT IGNORE INTO roles (role_name, description) VALUES
('ROLE_CHURCH_ADMIN', '教會系統管理員'),
('ROLE_CHURCH_EDITOR', '教會系統編輯者'),
('ROLE_CHURCH_VIEWER', '教會系統查看者');

-- 10. 插入預設權限
INSERT IGNORE INTO permissions (permission_code, permission_name, resource, action, description) VALUES
('SERVICE_SCHEDULE_READ', '查看服事表', 'service_schedule', 'read', '可以查看服事表'),
('SERVICE_SCHEDULE_EDIT', '編輯服事表', 'service_schedule', 'edit', '可以新增、修改、刪除服事表'),
('PERSON_READ', '查看人員', 'person', 'read', '可以查看人員列表'),
('PERSON_EDIT', '編輯人員', 'person', 'edit', '可以新增、修改、刪除人員'),
('POSITION_READ', '查看崗位', 'position', 'read', '可以查看崗位列表'),
('POSITION_EDIT', '編輯崗位', 'position', 'edit', '可以新增、修改、刪除崗位'),
('CHURCH_ADMIN', '教會管理', 'church', 'admin', '可以存取所有教會管理功能');

-- 11. 將所有權限分配給 ADMIN 角色
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_CHURCH_ADMIN';

-- 12. 將讀取權限分配給 EDITOR 角色
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_CHURCH_EDITOR' 
  AND (p.action = 'read' OR p.action = 'edit');

-- 13. 將讀取權限分配給 VIEWER 角色
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_CHURCH_VIEWER' 
  AND p.action = 'read';

-- 14. 插入前台菜單（公開訪問）
INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active) VALUES
('HOME', '首頁', '🏠', '/', NULL, 1, 'frontend', NULL, 1),
('ABOUT', '關於我們', 'ℹ️', '/about', NULL, 2, 'frontend', NULL, 1),
('ACTIVITIES', '活動', '📅', '/activities', NULL, 3, 'frontend', NULL, 1),
('SERVICE_SCHEDULE', '服事安排', '📋', '/service-schedule', NULL, 4, 'frontend', NULL, 1),
('CONTACT', '聯絡我們', '📧', '/contact', NULL, 5, 'frontend', NULL, 1);

-- 15. 插入後台菜單（需要登入）
INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active) VALUES
('ADMIN_DASHBOARD', '儀表板', '📊', '/admin', NULL, 1, 'admin', NULL, 1),
('ADMIN_SERVICE_SCHEDULE', '服事表管理', '📋', '/admin/service-schedule', NULL, 2, 'admin', 'SERVICE_SCHEDULE_READ', 1),
('ADMIN_PERSONS', '人員管理', '👥', '/admin/persons', NULL, 3, 'admin', 'PERSON_READ', 1),
('ADMIN_POSITIONS', '崗位管理', '🎯', '/admin/positions', NULL, 4, 'admin', 'POSITION_READ', 1),
('ADMIN_SETTINGS', '系統設定', '⚙️', '#', NULL, 99, 'admin', 'CHURCH_ADMIN', 1);

-- 插入設定子菜單
INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active)
SELECT 'ADMIN_USERS', '用戶管理', '👤', '/admin/users', 
    (SELECT id FROM menu_items WHERE menu_code = 'ADMIN_SETTINGS' LIMIT 1), 1, 'admin', 'CHURCH_ADMIN', 1
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_USERS');

INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active)
SELECT 'ADMIN_ROLES', '角色管理', '🔐', '/admin/roles', 
    (SELECT id FROM menu_items WHERE menu_code = 'ADMIN_SETTINGS' LIMIT 1), 2, 'admin', 'CHURCH_ADMIN', 1
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_ROLES');

INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active)
SELECT 'ADMIN_PERMISSIONS', '權限管理', '🔑', '/admin/permissions', 
    (SELECT id FROM menu_items WHERE menu_code = 'ADMIN_SETTINGS' LIMIT 1), 3, 'admin', 'CHURCH_ADMIN', 1
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_PERMISSIONS');

INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active)
SELECT 'ADMIN_MENUS', '菜單管理', '📑', '/admin/menus', 
    (SELECT id FROM menu_items WHERE menu_code = 'ADMIN_SETTINGS' LIMIT 1), 4, 'admin', 'CHURCH_ADMIN', 1
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_MENUS');

INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active)
SELECT 'ADMIN_MAINTENANCE', '系統維護', '🔧', '/admin/maintenance', 
    (SELECT id FROM menu_items WHERE menu_code = 'ADMIN_SETTINGS' LIMIT 1), 5, 'admin', 'CHURCH_ADMIN', 1
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_MAINTENANCE');

-- 16. 插入 URL 權限配置（教會 API 公開訪問）
INSERT IGNORE INTO url_permissions (
    url_pattern, http_method, is_public, required_role, required_permission, 
    description, is_active, order_index
) VALUES
('/api/church/service-schedules', 'GET', 1, NULL, NULL, '教會服事表查詢 - 公開訪問', 1, 0),
('/api/church/persons', 'GET', 1, NULL, NULL, '教會人員查詢 - 公開訪問', 1, 0),
('/api/church/positions', 'GET', 1, NULL, NULL, '教會崗位查詢 - 公開訪問', 1, 0),
('/api/church/menus/frontend', 'GET', 1, NULL, NULL, '教會前台菜單 - 公開訪問', 1, 0),
('/api/church/auth/**', NULL, 1, NULL, NULL, '教會認證 API - 公開訪問', 1, 0);

-- 17. 插入 URL 權限配置（後台管理需要權限）
INSERT IGNORE INTO url_permissions (
    url_pattern, http_method, is_public, required_role, required_permission, 
    description, is_active, order_index
) VALUES
('/api/church/service-schedules', 'POST', 0, NULL, 'SERVICE_SCHEDULE_EDIT', '教會服事表新增 - 需要編輯權限', 1, 10),
('/api/church/service-schedules/**', 'PUT', 0, NULL, 'SERVICE_SCHEDULE_EDIT', '教會服事表修改 - 需要編輯權限', 1, 10),
('/api/church/service-schedules/**', 'DELETE', 0, NULL, 'SERVICE_SCHEDULE_EDIT', '教會服事表刪除 - 需要編輯權限', 1, 10),
('/api/church/persons', 'POST', 0, NULL, 'PERSON_EDIT', '教會人員新增 - 需要編輯權限', 1, 10),
('/api/church/persons/**', 'PUT', 0, NULL, 'PERSON_EDIT', '教會人員修改 - 需要編輯權限', 1, 10),
('/api/church/persons/**', 'DELETE', 0, NULL, 'PERSON_EDIT', '教會人員刪除 - 需要編輯權限', 1, 10),
('/api/church/positions', 'POST', 0, NULL, 'POSITION_EDIT', '教會崗位新增 - 需要編輯權限', 1, 10),
('/api/church/positions/**', 'PUT', 0, NULL, 'POSITION_EDIT', '教會崗位修改 - 需要編輯權限', 1, 10),
('/api/church/positions/**', 'DELETE', 0, NULL, 'POSITION_EDIT', '教會崗位刪除 - 需要編輯權限', 1, 10),
('/api/church/menus/admin', 'GET', 0, NULL, NULL, '教會後台菜單 - 需要登入', 1, 10),
('/api/church/admin/**', NULL, 0, NULL, 'CHURCH_ADMIN', '教會管理 API - 需要管理權限', 1, 10);

-- 顯示建立結果
SELECT '權限表建立完成' AS message;
SELECT COUNT(*) AS user_count FROM users;
SELECT COUNT(*) AS role_count FROM roles;
SELECT COUNT(*) AS permission_count FROM permissions;
SELECT COUNT(*) AS menu_count FROM menu_items;
SELECT COUNT(*) AS url_permission_count FROM url_permissions;

