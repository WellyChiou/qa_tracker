-- 小組公開 API 權限配置
-- 教會系統專用

USE church;

-- 在 url_permissions 表新增小組公開 API 的 URL 權限配置
INSERT IGNORE INTO url_permissions (
    url_pattern, http_method, is_public, required_role, required_permission, 
    description, is_active, order_index
) VALUES
('/api/church/public/groups', 'GET', 1, NULL, NULL, '獲取小組列表（公開訪問）', 1, 290),
('/api/church/public/groups/*', 'GET', 1, NULL, NULL, '獲取單一小組詳情（公開訪問）', 1, 291);

