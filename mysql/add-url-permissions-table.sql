-- 添加 URL 權限配置表
-- 用於動態管理 URL 訪問權限

USE qa_tracker;

-- URL 權限配置表
CREATE TABLE IF NOT EXISTS url_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    url_pattern VARCHAR(255) NOT NULL COMMENT 'URL 模式（支持通配符 *）',
    http_method VARCHAR(10) COMMENT 'HTTP 方法（GET, POST, PUT, DELETE, 空表示全部）',
    required_role VARCHAR(50) COMMENT '需要的角色（ROLE_ADMIN, ROLE_USER 等，NULL 表示只需認證）',
    required_permission VARCHAR(100) COMMENT '需要的權限代碼（NULL 表示不需要特定權限）',
    is_public TINYINT(1) DEFAULT 0 COMMENT '是否公開（1=公開，0=需要認證）',
    order_index INT NOT NULL DEFAULT 0 COMMENT '匹配順序（數字越小越優先）',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否啟用',
    description VARCHAR(255) COMMENT '描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_url_pattern (url_pattern),
    INDEX idx_order_index (order_index),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='URL 權限配置表';

-- 插入預設的 URL 權限配置
-- 順序很重要：更具體的規則應該有較小的 order_index

-- 1. 公開端點（order_index: 1-10）
INSERT IGNORE INTO url_permissions (url_pattern, http_method, is_public, order_index, description) VALUES
('/api/auth/**', NULL, 1, 1, '認證相關 API，公開訪問'),
('/api/public/**', NULL, 1, 2, '公開 API'),
('/api/hello', 'GET', 1, 3, '健康檢查'),
('/api/utils/**', NULL, 1, 4, '工具 API'),
('/login.html', NULL, 1, 5, '登入頁面'),
('/*.css', NULL, 1, 6, 'CSS 文件'),
('/*.js', NULL, 1, 7, 'JavaScript 文件'),
('/api.js', NULL, 1, 8, 'API 服務文件'),
('/style.css', NULL, 1, 9, '樣式文件'),
('/', NULL, 1, 10, '首頁'),
('/index.html', NULL, 1, 11, '首頁'),
('/expenses.html', NULL, 1, 12, '記帳系統頁面'),
('/tracker.html', NULL, 1, 13, 'Tracker 頁面');

-- 2. 管理端點（order_index: 20-30，僅限 ADMIN）
INSERT IGNORE INTO url_permissions (url_pattern, http_method, required_role, order_index, description) VALUES
('/api/users/**', NULL, 'ROLE_ADMIN', 20, '用戶管理 API'),
('/api/roles/**', NULL, 'ROLE_ADMIN', 21, '角色管理 API'),
('/api/permissions/**', NULL, 'ROLE_ADMIN', 22, '權限管理 API'),
('/api/admin/**', NULL, 'ROLE_ADMIN', 23, '管理功能 API');

-- 3. 需要認證的 API 端點（order_index: 40-50）
INSERT IGNORE INTO url_permissions (url_pattern, http_method, required_role, order_index, description) VALUES
('/api/menus/**', NULL, NULL, 40, '菜單 API，需要認證'),
('/api/records/**', NULL, NULL, 41, '記錄 API，需要認證'),
('/api/expenses/**', NULL, NULL, 42, '記帳 API，需要認證'),
('/api/assets/**', NULL, NULL, 43, '資產 API，需要認證'),
('/api/exchange-rates/**', NULL, NULL, 44, '匯率 API，需要認證'),
('/api/config/**', NULL, NULL, 45, '配置 API，需要認證');

-- 4. 其他 API（order_index: 100）
INSERT IGNORE INTO url_permissions (url_pattern, http_method, required_role, order_index, description) VALUES
('/api/**', NULL, NULL, 100, '其他所有 API，需要認證');

-- 5. HTML 頁面（order_index: 200）
INSERT IGNORE INTO url_permissions (url_pattern, http_method, required_role, order_index, description) VALUES
('/*.html', NULL, NULL, 200, '其他 HTML 頁面，需要認證');

-- 6. 其他請求（order_index: 999）
INSERT IGNORE INTO url_permissions (url_pattern, http_method, required_role, order_index, description) VALUES
('/**', NULL, NULL, 999, '所有其他請求，需要認證');

