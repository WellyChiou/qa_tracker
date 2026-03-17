-- ============================================
-- 個人系統權限配置（整合版）
-- 資料庫：qa_tracker
-- ============================================
-- 此檔案整合了以下檔案：
-- - grant-permissions.sql（資料庫權限授予）
-- - add-scheduled-jobs-url-permissions.sql（定時任務 URL 權限）
-- - add-personal-maintenance-url-permissions.sql（系統維護 URL 權限）
-- ============================================

-- ============================================
-- 1. 資料庫權限授予
-- ============================================
-- 授予 appuser 對 qa_tracker 和 church 資料庫的權限

-- 授予所有權限
GRANT ALL PRIVILEGES ON qa_tracker.* TO 'appuser'@'%';
GRANT ALL PRIVILEGES ON church.* TO 'appuser'@'%';

-- 重新載入權限
FLUSH PRIVILEGES;

-- 驗證權限
SHOW GRANTS FOR 'appuser'@'%';

-- ============================================
-- 2. 定時任務管理的 URL 權限配置
-- ============================================
USE qa_tracker;

-- 確保 url_permissions 表存在（如果不存在，請先執行相關 schema 檔案）

-- 插入定時任務管理的 URL 權限配置
INSERT IGNORE INTO url_permissions (url_pattern, http_method, required_role, required_permission, is_public, is_active, order_index, description) VALUES
-- 查詢所有任務
('/api/scheduled-jobs', 'GET', 'ROLE_ADMIN', NULL, 0, 1, 100, '查詢所有定時任務'),
-- 查詢單一任務
('/api/scheduled-jobs/*', 'GET', 'ROLE_ADMIN', NULL, 0, 1, 101, '查詢單一定時任務'),
-- 創建任務
('/api/scheduled-jobs', 'POST', 'ROLE_ADMIN', NULL, 0, 1, 102, '創建定時任務'),
-- 更新任務
('/api/scheduled-jobs/*', 'PUT', 'ROLE_ADMIN', NULL, 0, 1, 103, '更新定時任務'),
-- 刪除任務
('/api/scheduled-jobs/*', 'DELETE', 'ROLE_ADMIN', NULL, 0, 1, 104, '刪除定時任務'),
-- 立即執行任務
('/api/scheduled-jobs/*/execute', 'POST', 'ROLE_ADMIN', NULL, 0, 1, 105, '立即執行定時任務'),
-- 啟用/停用任務
('/api/scheduled-jobs/*/toggle', 'PUT', 'ROLE_ADMIN', NULL, 0, 1, 106, '啟用/停用定時任務');

-- ============================================
-- 3. Personal 系統維護相關的 URL 權限配置
-- ============================================
-- 系統參數管理和備份管理 API

-- 系統參數管理 API
INSERT IGNORE INTO url_permissions (
    url_pattern, 
    http_method, 
    required_role, 
    required_permission, 
    is_public, 
    is_active, 
    order_index, 
    description
) VALUES 
('/api/personal/admin/system-settings', 'GET', 'ROLE_ADMIN', NULL, 0, 1, 200, '獲取所有系統參數（需管理員權限）'),
('/api/personal/admin/system-settings', 'POST', 'ROLE_ADMIN', NULL, 0, 1, 201, '創建系統參數（需管理員權限）'),
('/api/personal/admin/system-settings/category/*', 'GET', 'ROLE_ADMIN', NULL, 0, 1, 202, '根據分類獲取系統參數（需管理員權限）'),
('/api/personal/admin/system-settings/*', 'GET', 'ROLE_ADMIN', NULL, 0, 1, 203, '根據鍵獲取系統參數（需管理員權限）'),
('/api/personal/admin/system-settings/*', 'PUT', 'ROLE_ADMIN', NULL, 0, 1, 204, '更新系統參數（需管理員權限）'),
('/api/personal/admin/system-settings/*', 'DELETE', 'ROLE_ADMIN', NULL, 0, 1, 205, '刪除系統參數（需管理員權限）'),
('/api/personal/admin/system-settings/refresh', 'POST', 'ROLE_ADMIN', NULL, 0, 1, 206, '刷新系統配置緩存（需管理員權限）'),

-- 備份管理 API
('/api/personal/admin/backups', 'GET', 'ROLE_ADMIN', NULL, 0, 1, 210, '獲取備份列表（需管理員權限）'),
('/api/personal/admin/backups/create', 'POST', 'ROLE_ADMIN', NULL, 0, 1, 211, '創建備份（需管理員權限）'),
('/api/personal/admin/backups/download', 'GET', 'ROLE_ADMIN', NULL, 0, 1, 212, '下載備份檔案（需管理員權限）'),
('/api/personal/admin/backups/delete', 'DELETE', 'ROLE_ADMIN', NULL, 0, 1, 213, '刪除備份檔案（需管理員權限）');
