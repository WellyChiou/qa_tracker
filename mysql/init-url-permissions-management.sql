-- 初始化 URL 權限管理 API 的權限配置
-- 這些配置確保 URL 權限管理功能本身可以正常運作

USE qa_tracker;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 個人網站 URL 權限管理 API
-- 需要 ADMIN 角色才能管理 URL 權限
INSERT IGNORE INTO url_permissions (
    url_pattern, 
    http_method, 
    is_public, 
    required_role, 
    required_permission, 
    is_active, 
    order_index, 
    description
) VALUES 
-- 查詢所有 URL 權限配置
('/api/url-permissions', 'GET', 0, 'ROLE_ADMIN', NULL, 1, 10, '查詢所有 URL 權限配置（需管理員）'),
-- 查詢單一 URL 權限配置
('/api/url-permissions/*', 'GET', 0, 'ROLE_ADMIN', NULL, 1, 11, '查詢單一 URL 權限配置（需管理員）'),
-- 新增 URL 權限配置
('/api/url-permissions', 'POST', 0, 'ROLE_ADMIN', NULL, 1, 12, '新增 URL 權限配置（需管理員）'),
-- 更新 URL 權限配置
('/api/url-permissions/*', 'PUT', 0, 'ROLE_ADMIN', NULL, 1, 13, '更新 URL 權限配置（需管理員）'),
-- 刪除 URL 權限配置
('/api/url-permissions/*', 'DELETE', 0, 'ROLE_ADMIN', NULL, 1, 14, '刪除 URL 權限配置（需管理員）'),
-- 查詢啟用的 URL 權限配置（公開 API，用於動態配置）
('/api/url-permissions/active', 'GET', 1, NULL, NULL, 1, 0, '查詢啟用的 URL 權限配置（公開）');

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 教會 URL 權限管理 API
-- 需要 PERM_CHURCH_ADMIN 權限才能管理 URL 權限
INSERT IGNORE INTO url_permissions (
    url_pattern, 
    http_method, 
    is_public, 
    required_role, 
    required_permission, 
    is_active, 
    order_index, 
    description
) VALUES 
-- 查詢所有 URL 權限配置
('/api/church/admin/url-permissions', 'GET', 0, NULL, 'PERM_CHURCH_ADMIN', 1, 10, '查詢所有 URL 權限配置（需教會管理員權限）'),
-- 查詢單一 URL 權限配置
('/api/church/admin/url-permissions/*', 'GET', 0, NULL, 'PERM_CHURCH_ADMIN', 1, 11, '查詢單一 URL 權限配置（需教會管理員權限）'),
-- 新增 URL 權限配置
('/api/church/admin/url-permissions', 'POST', 0, NULL, 'PERM_CHURCH_ADMIN', 1, 12, '新增 URL 權限配置（需教會管理員權限）'),
-- 更新 URL 權限配置
('/api/church/admin/url-permissions/*', 'PUT', 0, NULL, 'PERM_CHURCH_ADMIN', 1, 13, '更新 URL 權限配置（需教會管理員權限）'),
-- 刪除 URL 權限配置
('/api/church/admin/url-permissions/*', 'DELETE', 0, NULL, 'PERM_CHURCH_ADMIN', 1, 14, '刪除 URL 權限配置（需教會管理員權限）');

-- 顯示設定結果
SELECT 
    '個人網站 URL 權限管理 API 配置' AS message,
    url_pattern,
    http_method,
    is_public,
    required_role,
    required_permission,
    description
FROM qa_tracker.url_permissions
WHERE url_pattern LIKE '/api/url-permissions%'
ORDER BY order_index, id;

SELECT 
    '教會 URL 權限管理 API 配置' AS message,
    url_pattern,
    http_method,
    is_public,
    required_role,
    required_permission,
    description
FROM church.url_permissions
WHERE url_pattern LIKE '/api/church/admin/url-permissions%'
ORDER BY order_index, id;

