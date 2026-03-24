-- System Users management ACL seed
-- Scope:
-- 1) 新增用戶管理編輯/重設密碼權限
-- 2) 新增 system users API URL 權限對接

USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
VALUES
('INVEST_SYS_USERS_EDIT', '管理用戶資料', 'system_user', 'edit', '可新增/編輯/啟用停用用戶'),
('INVEST_SYS_USERS_RESET_PASSWORD', '重設用戶密碼', 'system_user', 'reset_password', '可重設用戶密碼')
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
      'INVEST_SYS_USERS_VIEW',
      'INVEST_SYS_USERS_EDIT',
      'INVEST_SYS_USERS_RESET_PASSWORD'
  )
WHERE r.role_name = 'ROLE_INVEST_ADMIN';

UPDATE menu_items
SET description = '管理 invest 後台帳號、狀態與密碼重設',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'INVEST_SYS_USERS';

INSERT INTO url_permissions (url_pattern, http_method, is_public, required_role, required_permission, description, is_active, order_index)
VALUES
('/api/invest/system/users/paged', 'GET', 0, NULL, 'INVEST_SYS_USERS_VIEW', '查看用戶列表 paged', 1, 201),
('/api/invest/system/users/*', 'GET', 0, NULL, 'INVEST_SYS_USERS_VIEW', '查看用戶明細', 1, 202),
('/api/invest/system/users', 'POST', 0, NULL, 'INVEST_SYS_USERS_EDIT', '新增用戶', 1, 203),
('/api/invest/system/users/*', 'PUT', 0, NULL, 'INVEST_SYS_USERS_EDIT', '更新用戶資料', 1, 204),
('/api/invest/system/users/*/enabled', 'PUT', 0, NULL, 'INVEST_SYS_USERS_EDIT', '切換用戶啟用狀態', 1, 205),
('/api/invest/system/users/*/reset-password', 'POST', 0, NULL, 'INVEST_SYS_USERS_RESET_PASSWORD', '重設用戶密碼', 1, 206)
ON DUPLICATE KEY UPDATE
    is_public = VALUES(is_public),
    required_role = VALUES(required_role),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    is_active = VALUES(is_active),
    order_index = VALUES(order_index),
    updated_at = CURRENT_TIMESTAMP;
