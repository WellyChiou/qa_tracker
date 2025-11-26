-- 添加 Spring Security 認證和授權相關的表
-- 執行此 SQL 以更新現有數據庫

USE qa_tracker;

-- 1. 擴展 users 表，添加認證相關欄位
-- 注意：如果欄位已存在，執行這些語句會報錯，可以忽略

-- 檢查並添加 username 欄位
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'users' AND COLUMN_NAME = 'username') > 0,
    'SELECT 1',
    CONCAT('ALTER TABLE users ADD COLUMN username VARCHAR(100) UNIQUE COMMENT ''用戶名（用於登入）'' AFTER email')
));
SET @dbname = DATABASE();
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加其他欄位（使用簡化方式，如果已存在會報錯但可忽略）
ALTER TABLE users ADD COLUMN username VARCHAR(100) UNIQUE COMMENT '用戶名（用於登入）' AFTER email;
ALTER TABLE users ADD COLUMN password VARCHAR(255) COMMENT '加密後的密碼' AFTER username;
ALTER TABLE users ADD COLUMN is_enabled TINYINT(1) DEFAULT 1 COMMENT '帳號是否啟用' AFTER provider_id;
ALTER TABLE users ADD COLUMN is_account_non_locked TINYINT(1) DEFAULT 1 COMMENT '帳號是否未鎖定' AFTER is_enabled;

-- 2. 角色表
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    role_name VARCHAR(50) UNIQUE NOT NULL COMMENT '角色名稱',
    description VARCHAR(255) COMMENT '角色描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_role_name (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 3. 權限表
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='權限表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用戶角色關聯表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色權限關聯表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用戶權限關聯表';

-- 7. 菜單項表
CREATE TABLE IF NOT EXISTS menu_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    menu_code VARCHAR(50) UNIQUE NOT NULL COMMENT '菜單代碼',
    menu_name VARCHAR(100) NOT NULL COMMENT '菜單名稱',
    icon VARCHAR(50) COMMENT '圖標',
    url VARCHAR(255) COMMENT '菜單連結',
    parent_id BIGINT COMMENT '父菜單 ID',
    order_index INT NOT NULL DEFAULT 0 COMMENT '排序順序',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否啟用',
    required_permission VARCHAR(100) COMMENT '需要的權限代碼',
    description VARCHAR(255) COMMENT '菜單描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_menu_code (menu_code),
    INDEX idx_parent_id (parent_id),
    INDEX idx_order_index (order_index),
    FOREIGN KEY (parent_id) REFERENCES menu_items(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜單項表';

-- 8. 插入預設角色
INSERT IGNORE INTO roles (role_name, description) VALUES
('ROLE_ADMIN', '系統管理員'),
('ROLE_USER', '一般使用者'),
('ROLE_VIEWER', '唯讀使用者');

-- 9. 插入預設權限
INSERT IGNORE INTO permissions (permission_code, permission_name, resource, action, description) VALUES
('EXPENSES_READ', '查看記帳系統', 'expenses', 'read', '可以查看家庭記帳系統'),
('EXPENSES_WRITE', '編輯記帳系統', 'expenses', 'write', '可以編輯家庭記帳系統'),
('TRACKER_READ', '查看 QA Tracker', 'tracker', 'read', '可以查看 QA Tracker'),
('TRACKER_WRITE', '編輯 QA Tracker', 'tracker', 'write', '可以編輯 QA Tracker'),
('ASSETS_READ', '查看資產', 'assets', 'read', '可以查看資產'),
('ASSETS_WRITE', '編輯資產', 'assets', 'write', '可以編輯資產'),
('ADMIN_ACCESS', '管理員存取', 'admin', 'all', '可以存取管理功能');

-- 10. 將管理員權限分配給 ADMIN 角色
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_ADMIN';

-- 11. 將所有讀取權限分配給 USER 角色
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_USER' 
  AND p.action = 'read';

-- 12. 將所有讀取權限分配給 VIEWER 角色
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_VIEWER' 
  AND p.action = 'read';

-- 13. 插入預設菜單項
INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, required_permission) VALUES
('DASHBOARD', '儀表板', '📊', '/', NULL, 1, NULL),
('EXPENSES', '家庭記帳', '💰', '/expenses.html', NULL, 2, 'EXPENSES_READ'),
('TRACKER', 'QA Tracker', '📋', '/tracker.html', NULL, 3, 'TRACKER_READ'),
('ADMIN', '系統管理', '⚙️', '#', NULL, 99, 'ADMIN_ACCESS'),
('ADMIN_USERS', '用戶管理', '👥', '/admin/users.html', 
    (SELECT id FROM menu_items WHERE menu_code = 'ADMIN'), 1, 'ADMIN_ACCESS'),
('ADMIN_ROLES', '角色管理', '🔐', '/admin/roles.html', 
    (SELECT id FROM menu_items WHERE menu_code = 'ADMIN'), 2, 'ADMIN_ACCESS'),
('ADMIN_PERMISSIONS', '權限管理', '🔑', '/admin/permissions.html', 
    (SELECT id FROM menu_items WHERE menu_code = 'ADMIN'), 3, 'ADMIN_ACCESS'),
('ADMIN_MENUS', '菜單管理', '📑', '/admin/menus.html', 
    (SELECT id FROM menu_items WHERE menu_code = 'ADMIN'), 4, 'ADMIN_ACCESS');

