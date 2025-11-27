-- 添加定時任務管理的 URL 權限配置
-- 執行此 SQL 以添加定時任務管理相關的 URL 權限

USE qa_tracker;

-- 確保 url_permissions 表存在（如果不存在，請先執行 add-security-tables-simple.sql）

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

