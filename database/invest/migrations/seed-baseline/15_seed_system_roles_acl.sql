-- System Roles management ACL seed
-- Scope:
-- 1) 新增角色管理編輯權限
-- 2) 新增 system roles API URL 權限對接

USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
VALUES
('INVEST_SYS_ROLES_EDIT', '管理角色資料', 'system_role', 'edit', '可新增/編輯/刪除角色與更新角色權限')
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
      'INVEST_SYS_ROLES_VIEW',
      'INVEST_SYS_ROLES_EDIT'
  )
WHERE r.role_name = 'ROLE_INVEST_ADMIN';

UPDATE menu_items
SET description = '管理 invest 角色資料與角色權限綁定',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'INVEST_SYS_ROLES';

INSERT INTO url_permissions (url_pattern, http_method, is_public, required_role, required_permission, description, is_active, order_index)
VALUES
('/api/invest/system/roles/paged', 'GET', 0, NULL, 'INVEST_SYS_ROLES_VIEW', '查看角色列表 paged', 1, 211),
('/api/invest/system/roles/permission-options', 'GET', 0, NULL, 'INVEST_SYS_ROLES_VIEW', '查看角色可選權限清單', 1, 212),
('/api/invest/system/roles/*', 'GET', 0, NULL, 'INVEST_SYS_ROLES_VIEW', '查看角色明細', 1, 213),
('/api/invest/system/roles/*/permissions', 'GET', 0, NULL, 'INVEST_SYS_ROLES_VIEW', '查看角色權限綁定', 1, 214),
('/api/invest/system/roles', 'POST', 0, NULL, 'INVEST_SYS_ROLES_EDIT', '新增角色', 1, 215),
('/api/invest/system/roles/*', 'PUT', 0, NULL, 'INVEST_SYS_ROLES_EDIT', '更新角色資料', 1, 216),
('/api/invest/system/roles/*/permissions', 'PUT', 0, NULL, 'INVEST_SYS_ROLES_EDIT', '更新角色權限綁定', 1, 217),
('/api/invest/system/roles/*', 'DELETE', 0, NULL, 'INVEST_SYS_ROLES_EDIT', '刪除角色', 1, 218)
ON DUPLICATE KEY UPDATE
    is_public = VALUES(is_public),
    required_role = VALUES(required_role),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    is_active = VALUES(is_active),
    order_index = VALUES(order_index),
    updated_at = CURRENT_TIMESTAMP;
