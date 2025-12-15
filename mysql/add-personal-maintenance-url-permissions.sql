-- ============================================
-- 添加 Personal 系統維護相關的 URL 權限配置
-- ============================================
-- 此腳本用於添加 Personal 系統維護頁面（系統參數和備份管理）的 URL 權限
-- ============================================

USE qa_tracker;

-- 確保 url_permissions 表存在（如果不存在，請先執行相關的 security tables SQL）

-- 插入 Personal 系統維護相關的 URL 權限配置
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
-- 系統參數管理 API
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

-- 顯示插入結果
SELECT 
    'Personal 系統維護權限配置完成' AS status,
    COUNT(*) AS total_permissions
FROM url_permissions
WHERE url_pattern LIKE '/api/personal/admin/%'
AND is_active = 1;

