-- 為教會網站 API 添加公開權限配置
-- 確保 /api/church/** 路徑可以公開訪問

USE qa_tracker;

-- 如果不存在，則插入
INSERT INTO url_permissions (
    url_pattern,
    http_method,
    is_public,
    required_role,
    required_permission,
    description,
    is_active,
    order_index,
    created_at,
    updated_at
)
SELECT 
    '/api/church/**' AS url_pattern,
    NULL AS http_method,  -- 允許所有 HTTP 方法
    1 AS is_public,  -- 公開訪問
    NULL AS required_role,
    NULL AS required_permission,
    '教會網站 API - 公開訪問' AS description,
    1 AS is_active,
    0 AS order_index,  -- 設置為最高優先級（數字越小優先級越高）
    NOW() AS created_at,
    NOW() AS updated_at
WHERE NOT EXISTS (
    SELECT 1 FROM url_permissions WHERE url_pattern = '/api/church/**'
);

-- 顯示插入結果
SELECT 
    id,
    url_pattern,
    http_method,
    is_public,
    required_role,
    is_active,
    order_index,
    description
FROM url_permissions 
WHERE url_pattern = '/api/church/**';

