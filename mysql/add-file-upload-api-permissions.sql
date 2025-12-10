-- 添加文件上傳 API 的 URL 權限配置
USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 插入文件上傳 API 的 URL 權限配置
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
-- 活動圖片上傳（需活動編輯權限）
('/api/church/admin/upload/image', 'POST', 0, NULL, 'ACTIVITY_EDIT', 1, 240, '上傳活動圖片（需活動編輯權限）'),
-- 主日信息圖片上傳（需主日信息編輯權限）
('/api/church/admin/upload/image', 'POST', 0, NULL, 'SUNDAY_MESSAGE_EDIT', 1, 241, '上傳主日信息圖片（需主日信息編輯權限）');

-- 顯示設定結果
SELECT 
    '文件上傳 API 權限配置' AS message,
    url_pattern,
    http_method,
    is_public,
    required_role,
    required_permission,
    description
FROM url_permissions
WHERE url_pattern LIKE '/api/church/admin/upload%'
ORDER BY order_index, id;

