-- 設定 /api/church/positions/{id}/persons 為公開查詢
-- 其中 {id} 是動態的，使用通配符 * 來匹配

USE church;

-- 插入或更新崗位人員查詢 API 為公開
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
    '/api/church/positions/*/persons', 
    'GET', 
    1, 
    NULL, 
    NULL, 
    '教會崗位人員查詢 - 公開訪問（支持動態 ID）', 
    1, 
    0
) ON DUPLICATE KEY UPDATE 
    http_method = VALUES(http_method), 
    is_public = VALUES(is_public), 
    required_role = VALUES(required_role), 
    required_permission = VALUES(required_permission), 
    description = VALUES(description), 
    is_active = VALUES(is_active), 
    order_index = VALUES(order_index);

-- 驗證設定
SELECT 
    id,
    url_pattern,
    http_method,
    is_public,
    description,
    is_active,
    order_index
FROM url_permissions
WHERE url_pattern = '/api/church/positions/*/persons';

-- 說明：
-- 通配符說明：
-- * 表示匹配單層路徑（不含斜線），例如 /api/church/positions/1/persons
-- ** 表示匹配多層路徑（含斜線），例如 /api/church/positions/1/2/persons
-- 對於 /api/church/positions/{id}/persons，使用 * 即可

