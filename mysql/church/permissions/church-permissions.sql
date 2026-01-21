-- ============================================
-- 教會系統權限配置（整合版）
-- 資料庫：church
-- ============================================
-- 此檔案整合了以下檔案：
-- - prayer-requests-permissions.sql（代禱事項權限）
-- - announcements-permissions.sql（公告權限）
-- - groups-public-permissions.sql（小組公開 API 權限）
-- ============================================

USE church;

-- ============================================
-- 1. 代禱事項權限配置
-- ============================================

-- 1.1 在 permissions 表新增權限
INSERT IGNORE INTO permissions (permission_code, permission_name, resource, action, description) VALUES
('PRAYER_REQUEST_READ', '查看代禱事項', 'prayer_request', 'read', '可以查看代禱事項'),
('PRAYER_REQUEST_EDIT', '編輯代禱事項', 'prayer_request', 'edit', '可以新增、修改、刪除代禱事項');

-- 1.2 將權限分配給 ADMIN 角色
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_CHURCH_ADMIN'
  AND p.permission_code IN ('PRAYER_REQUEST_READ', 'PRAYER_REQUEST_EDIT');

-- 1.3 將讀取權限分配給 EDITOR 角色
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_CHURCH_EDITOR'
  AND p.permission_code IN ('PRAYER_REQUEST_READ', 'PRAYER_REQUEST_EDIT');

-- 1.4 將讀取權限分配給 VIEWER 角色
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_CHURCH_VIEWER'
  AND p.permission_code = 'PRAYER_REQUEST_READ';

-- 1.5 在 url_permissions 表新增 URL 權限配置
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

-- ============================================
-- 2. 公告權限配置
-- ============================================

-- 2.1 在 permissions 表新增權限
INSERT IGNORE INTO permissions (permission_code, permission_name, resource, action, description) VALUES
('ANNOUNCEMENT_READ', '查看公告', 'announcement', 'read', '可以查看公告'),
('ANNOUNCEMENT_EDIT', '編輯公告', 'announcement', 'edit', '可以新增、修改、刪除公告');

-- 2.2 將權限分配給 ADMIN 角色
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_CHURCH_ADMIN'
  AND p.permission_code IN ('ANNOUNCEMENT_READ', 'ANNOUNCEMENT_EDIT');

-- 2.3 將讀取權限分配給 EDITOR 角色
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_CHURCH_EDITOR'
  AND p.permission_code IN ('ANNOUNCEMENT_READ', 'ANNOUNCEMENT_EDIT');

-- 2.4 將讀取權限分配給 VIEWER 角色
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_CHURCH_VIEWER'
  AND p.permission_code = 'ANNOUNCEMENT_READ';

-- 2.5 在 url_permissions 表新增 URL 權限配置
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

-- ============================================
-- 3. 小組公開 API 權限配置
-- ============================================

-- 在 url_permissions 表新增小組公開 API 的 URL 權限配置
INSERT IGNORE INTO url_permissions (
    url_pattern, http_method, is_public, required_role, required_permission, 
    description, is_active, order_index
) VALUES
('/api/church/public/groups', 'GET', 1, NULL, NULL, '獲取小組列表（公開訪問）', 1, 290),
('/api/church/public/groups/*', 'GET', 1, NULL, NULL, '獲取單一小組詳情（公開訪問）', 1, 291);
