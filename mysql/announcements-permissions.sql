-- 公告權限配置
-- 教會系統專用

USE church;

-- 1. 在 permissions 表新增權限
INSERT IGNORE INTO permissions (permission_code, permission_name, resource, action, description) VALUES
('ANNOUNCEMENT_READ', '查看公告', 'announcement', 'read', '可以查看公告'),
('ANNOUNCEMENT_EDIT', '編輯公告', 'announcement', 'edit', '可以新增、修改、刪除公告');

-- 2. 將權限分配給 ADMIN 角色
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_CHURCH_ADMIN'
  AND p.permission_code IN ('ANNOUNCEMENT_READ', 'ANNOUNCEMENT_EDIT');

-- 3. 將讀取權限分配給 EDITOR 角色
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_CHURCH_EDITOR'
  AND p.permission_code IN ('ANNOUNCEMENT_READ', 'ANNOUNCEMENT_EDIT');

-- 4. 將讀取權限分配給 VIEWER 角色
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_CHURCH_VIEWER'
  AND p.permission_code = 'ANNOUNCEMENT_READ';

-- 5. 在 url_permissions 表新增 URL 權限配置
-- 公開 API
INSERT IGNORE INTO url_permissions (
    url_pattern, http_method, is_public, required_role, required_permission, 
    description, is_active, order_index
) VALUES
('/api/church/public/announcements', 'GET', 1, NULL, NULL, '獲取公告列表（公開訪問）', 1, 310);

-- 管理 API - 列表和新增
INSERT IGNORE INTO url_permissions (
    url_pattern, http_method, is_public, required_role, required_permission, 
    description, is_active, order_index
) VALUES
('/api/church/admin/announcements', 'GET', 0, NULL, 'ANNOUNCEMENT_READ', '獲取公告列表（需查看權限）', 1, 311),
('/api/church/admin/announcements', 'POST', 0, NULL, 'ANNOUNCEMENT_EDIT', '創建公告（需編輯權限）', 1, 312);

-- 管理 API - 單一資源操作
INSERT IGNORE INTO url_permissions (
    url_pattern, http_method, is_public, required_role, required_permission, 
    description, is_active, order_index
) VALUES
('/api/church/admin/announcements/*', 'GET', 0, NULL, 'ANNOUNCEMENT_READ', '獲取單一公告（需查看權限）', 1, 313),
('/api/church/admin/announcements/*', 'PUT', 0, NULL, 'ANNOUNCEMENT_EDIT', '更新公告（需編輯權限）', 1, 314),
('/api/church/admin/announcements/*', 'DELETE', 0, NULL, 'ANNOUNCEMENT_EDIT', '刪除公告（需編輯權限）', 1, 315);

