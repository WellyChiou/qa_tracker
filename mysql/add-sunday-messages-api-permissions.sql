-- 添加主日信息管理 API 的 URL 權限配置
-- 需要 CHURCH_ADMIN 權限才能訪問
USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 插入主日信息 API 的 URL 權限配置
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
-- 公開 API（前台訪問）
('/api/church/public/sunday-messages', 'GET', 1, NULL, NULL, 1, 103, '獲取主日信息（公開）'),
-- 後台管理 API（需主日信息編輯權限）
('/api/church/admin/sunday-messages', 'GET', 0, NULL, 'SUNDAY_MESSAGE_READ', 1, 235, '獲取所有主日信息（需查看主日信息權限）'),
('/api/church/admin/sunday-messages', 'POST', 0, NULL, 'SUNDAY_MESSAGE_EDIT', 1, 236, '創建主日信息（需編輯主日信息權限）'),
('/api/church/admin/sunday-messages/*', 'GET', 0, NULL, 'SUNDAY_MESSAGE_READ', 1, 237, '根據 ID 獲取主日信息（需查看主日信息權限）'),
('/api/church/admin/sunday-messages/*', 'PUT', 0, NULL, 'SUNDAY_MESSAGE_EDIT', 1, 238, '更新主日信息（需編輯主日信息權限）'),
('/api/church/admin/sunday-messages/*', 'DELETE', 0, NULL, 'SUNDAY_MESSAGE_EDIT', 1, 239, '刪除主日信息（需編輯主日信息權限）');

-- 顯示設定結果
SELECT 
    '主日信息 API 權限配置' AS message,
    url_pattern,
    http_method,
    is_public,
    required_role,
    required_permission,
    description
FROM url_permissions
WHERE url_pattern LIKE '/api/church/%/sunday-messages%'
ORDER BY order_index, id;

