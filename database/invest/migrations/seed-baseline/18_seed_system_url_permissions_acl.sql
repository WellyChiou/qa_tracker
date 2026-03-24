-- System URL Permissions management ACL seed
-- Scope:
-- 1) 新增 URL 權限管理編輯權限
-- 2) 新增 system url permissions API URL 權限對接

USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
VALUES
('INVEST_SYS_URL_PERMISSIONS_EDIT', '管理 URL 權限資料', 'system_url_permission', 'edit', '可新增/編輯/刪除 URL 權限規則與排序')
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    resource = VALUES(resource),
    action = VALUES(action),
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
  ON p.permission_code IN (
      'INVEST_SYS_URL_PERMISSIONS_VIEW',
      'INVEST_SYS_URL_PERMISSIONS_EDIT'
  )
WHERE r.role_name = 'ROLE_INVEST_ADMIN';

UPDATE menu_items
SET description = '管理 invest URL pattern / method / required permission 授權規則',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'INVEST_SYS_URL_PERMISSIONS';

INSERT INTO url_permissions (url_pattern, http_method, is_public, required_role, required_permission, description, is_active, order_index)
VALUES
('/api/invest/system/url-permissions/paged', 'GET', 0, NULL, 'INVEST_SYS_URL_PERMISSIONS_VIEW', '查看 URL 權限列表 paged', 1, 241),
('/api/invest/system/url-permissions/*', 'GET', 0, NULL, 'INVEST_SYS_URL_PERMISSIONS_VIEW', '查看 URL 權限明細', 1, 242),
('/api/invest/system/url-permissions/method-options', 'GET', 0, NULL, 'INVEST_SYS_URL_PERMISSIONS_VIEW', '查看 HTTP 方法選項', 1, 243),
('/api/invest/system/url-permissions/permission-options', 'GET', 0, NULL, 'INVEST_SYS_URL_PERMISSIONS_EDIT', '查看權限選項', 1, 244),
('/api/invest/system/url-permissions', 'POST', 0, NULL, 'INVEST_SYS_URL_PERMISSIONS_EDIT', '新增 URL 權限規則', 1, 245),
('/api/invest/system/url-permissions/*', 'PUT', 0, NULL, 'INVEST_SYS_URL_PERMISSIONS_EDIT', '更新 URL 權限規則', 1, 246),
('/api/invest/system/url-permissions/*', 'DELETE', 0, NULL, 'INVEST_SYS_URL_PERMISSIONS_EDIT', '刪除 URL 權限規則', 1, 247)
ON DUPLICATE KEY UPDATE
    is_public = VALUES(is_public),
    required_role = VALUES(required_role),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    is_active = VALUES(is_active),
    order_index = VALUES(order_index),
    updated_at = CURRENT_TIMESTAMP;
