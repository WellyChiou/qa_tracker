-- System Permissions management ACL seed
-- Scope:
-- 1) 新增權限管理編輯權限
-- 2) 新增 system permissions API URL 權限對接

USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
VALUES
('INVEST_SYS_PERMISSIONS_EDIT', '管理權限資料', 'system_permission', 'edit', '可新增/編輯/刪除權限與查詢綁定角色')
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
      'INVEST_SYS_PERMISSIONS_VIEW',
      'INVEST_SYS_PERMISSIONS_EDIT'
  )
WHERE r.role_name = 'ROLE_INVEST_ADMIN';

UPDATE menu_items
SET description = '管理 invest ACL 權限代碼、資源與動作',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'INVEST_SYS_PERMISSIONS';

INSERT INTO url_permissions (url_pattern, http_method, is_public, required_role, required_permission, description, is_active, order_index)
VALUES
('/api/invest/system/permissions/paged', 'GET', 0, NULL, 'INVEST_SYS_PERMISSIONS_VIEW', '查看權限列表 paged', 1, 221),
('/api/invest/system/permissions/*/role-bindings', 'GET', 0, NULL, 'INVEST_SYS_PERMISSIONS_VIEW', '查看權限角色綁定', 1, 222),
('/api/invest/system/permissions/*', 'GET', 0, NULL, 'INVEST_SYS_PERMISSIONS_VIEW', '查看權限明細', 1, 223),
('/api/invest/system/permissions', 'POST', 0, NULL, 'INVEST_SYS_PERMISSIONS_EDIT', '新增權限', 1, 224),
('/api/invest/system/permissions/*', 'PUT', 0, NULL, 'INVEST_SYS_PERMISSIONS_EDIT', '更新權限', 1, 225),
('/api/invest/system/permissions/*', 'DELETE', 0, NULL, 'INVEST_SYS_PERMISSIONS_EDIT', '刪除權限', 1, 226)
ON DUPLICATE KEY UPDATE
    is_public = VALUES(is_public),
    required_role = VALUES(required_role),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    is_active = VALUES(is_active),
    order_index = VALUES(order_index),
    updated_at = CURRENT_TIMESTAMP;
