-- 代禱事項權限配置
-- 教會系統專用

USE church;

-- 1. 在 permissions 表新增權限
INSERT IGNORE INTO permissions (permission_code, permission_name, resource, action, description) VALUES
('PRAYER_REQUEST_READ', '查看代禱事項', 'prayer_request', 'read', '可以查看代禱事項'),
('PRAYER_REQUEST_EDIT', '編輯代禱事項', 'prayer_request', 'edit', '可以新增、修改、刪除代禱事項');

-- 2. 將權限分配給 ADMIN 角色
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_CHURCH_ADMIN'
  AND p.permission_code IN ('PRAYER_REQUEST_READ', 'PRAYER_REQUEST_EDIT');

-- 3. 將讀取權限分配給 EDITOR 角色
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_CHURCH_EDITOR'
  AND p.permission_code IN ('PRAYER_REQUEST_READ', 'PRAYER_REQUEST_EDIT');

-- 4. 將讀取權限分配給 VIEWER 角色
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_CHURCH_VIEWER'
  AND p.permission_code = 'PRAYER_REQUEST_READ';

-- 5. 在 url_permissions 表新增 URL 權限配置
-- 公開 API
INSERT IGNORE INTO url_permissions (
    url_pattern, http_method, is_public, required_role, required_permission, 
    description, is_active, order_index
) VALUES
('/api/church/public/prayer-requests', 'GET', 1, NULL, NULL, '獲取代禱事項列表（公開訪問）', 1, 300);

-- 管理 API - 列表和新增
INSERT IGNORE INTO url_permissions (
    url_pattern, http_method, is_public, required_role, required_permission, 
    description, is_active, order_index
) VALUES
('/api/church/admin/prayer-requests', 'GET', 0, NULL, 'PRAYER_REQUEST_READ', '獲取代禱事項列表（需查看權限）', 1, 301),
('/api/church/admin/prayer-requests', 'POST', 0, NULL, 'PRAYER_REQUEST_EDIT', '創建代禱事項（需編輯權限）', 1, 302);

-- 管理 API - 單一資源操作
INSERT IGNORE INTO url_permissions (
    url_pattern, http_method, is_public, required_role, required_permission, 
    description, is_active, order_index
) VALUES
('/api/church/admin/prayer-requests/*', 'GET', 0, NULL, 'PRAYER_REQUEST_READ', '獲取單一代禱事項（需查看權限）', 1, 303),
('/api/church/admin/prayer-requests/*', 'PUT', 0, NULL, 'PRAYER_REQUEST_EDIT', '更新代禱事項（需編輯權限）', 1, 304),
('/api/church/admin/prayer-requests/*', 'DELETE', 0, NULL, 'PRAYER_REQUEST_EDIT', '刪除代禱事項（需編輯權限）', 1, 305);

