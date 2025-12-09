-- 設定前台菜單 API 為公開訪問
-- 在 church 資料庫的 url_permissions 表中設定

USE church;

-- 先刪除可能存在的舊設定（如果有的話）
DELETE FROM url_permissions 
WHERE url_pattern = '/api/church/menus/frontend' AND http_method = 'GET';

-- 插入新的設定（前台菜單 API 為公開）
INSERT INTO url_permissions (
    url_pattern, 
    http_method, 
    is_public, 
    required_role, 
    required_permission, 
    description, 
    is_active, 
    order_index
) VALUES (
    '/api/church/menus/frontend', 
    'GET', 
    1, 
    NULL, 
    NULL, 
    '教會前台菜單 - 公開訪問', 
    1, 
    0
);

-- 顯示設定結果
SELECT 
    id,
    url_pattern,
    http_method,
    is_public,
    description,
    is_active,
    order_index
FROM url_permissions
WHERE url_pattern = '/api/church/menus/frontend'
ORDER BY order_index ASC, id ASC;
