-- 設定教會系統的公開 API
-- 在 church 資料庫的 url_permissions 表中設定所有需要公開訪問的 API

USE church;

-- 先刪除可能存在的舊設定
DELETE FROM url_permissions 
WHERE url_pattern IN (
    '/api/church/menus/frontend',
    '/api/church/service-schedules',
    '/api/church/persons',
    '/api/church/positions',
    '/api/church/positions/config/full',
    '/api/church/positions/active'
) AND http_method = 'GET';

-- 插入公開訪問的 API 配置
INSERT INTO url_permissions (
    url_pattern, 
    http_method, 
    is_public, 
    required_role, 
    required_permission, 
    description, 
    is_active, 
    order_index
) VALUES
-- 前台菜單（必須公開）
('/api/church/menus/frontend', 'GET', 1, NULL, NULL, '教會前台菜單 - 公開訪問', 1, 0),
-- 服事表查詢（公開）
('/api/church/service-schedules', 'GET', 1, NULL, NULL, '教會服事表查詢 - 公開訪問', 1, 0),
-- 人員查詢（公開）
('/api/church/persons', 'GET', 1, NULL, NULL, '教會人員查詢 - 公開訪問', 1, 0),
-- 崗位查詢（公開，包含所有子路徑）
('/api/church/positions', 'GET', 1, NULL, NULL, '教會崗位查詢 - 公開訪問', 1, 0),
('/api/church/positions/active', 'GET', 1, NULL, NULL, '教會啟用崗位查詢 - 公開訪問', 1, 0),
('/api/church/positions/config/full', 'GET', 1, NULL, NULL, '教會崗位完整配置 - 公開訪問', 1, 0),
('/api/church/positions/config/**', 'GET', 1, NULL, NULL, '教會崗位配置 - 公開訪問', 1, 0);

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
WHERE is_public = 1 AND is_active = 1
ORDER BY order_index ASC, id ASC;

-- 顯示前台菜單的設定
SELECT 
    '前台菜單設定' AS info,
    url_pattern,
    http_method,
    is_public,
    is_active
FROM url_permissions
WHERE url_pattern = '/api/church/menus/frontend';

