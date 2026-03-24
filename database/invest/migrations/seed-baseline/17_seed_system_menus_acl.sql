-- System Menus management ACL seed
-- Scope:
-- 1) 新增菜單管理編輯權限
-- 2) 新增 system menus API URL 權限對接

USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
VALUES
('INVEST_SYS_MENUS_EDIT', '管理菜單資料', 'system_menu', 'edit', '可新增/編輯/刪除菜單與切換啟用狀態')
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
      'INVEST_SYS_MENUS_VIEW',
      'INVEST_SYS_MENUS_EDIT'
  )
WHERE r.role_name = 'ROLE_INVEST_ADMIN';

UPDATE menu_items
SET description = '管理 invest 菜單樹、父子層級與 required permission 綁定',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'INVEST_SYS_MENUS';

INSERT INTO url_permissions (url_pattern, http_method, is_public, required_role, required_permission, description, is_active, order_index)
VALUES
('/api/invest/system/menus/tree', 'GET', 0, NULL, 'INVEST_SYS_MENUS_VIEW', '查看菜單樹', 1, 231),
('/api/invest/system/menus/*', 'GET', 0, NULL, 'INVEST_SYS_MENUS_VIEW', '查看菜單明細', 1, 232),
('/api/invest/system/menus/parent-options', 'GET', 0, NULL, 'INVEST_SYS_MENUS_EDIT', '查看父菜單選項', 1, 233),
('/api/invest/system/menus/required-permission-options', 'GET', 0, NULL, 'INVEST_SYS_MENUS_EDIT', '查看 required permission 選項', 1, 234),
('/api/invest/system/menus', 'POST', 0, NULL, 'INVEST_SYS_MENUS_EDIT', '新增菜單', 1, 235),
('/api/invest/system/menus/*', 'PUT', 0, NULL, 'INVEST_SYS_MENUS_EDIT', '更新菜單', 1, 236),
('/api/invest/system/menus/*/enabled', 'PUT', 0, NULL, 'INVEST_SYS_MENUS_EDIT', '切換菜單啟用狀態', 1, 237),
('/api/invest/system/menus/*', 'DELETE', 0, NULL, 'INVEST_SYS_MENUS_EDIT', '刪除菜單', 1, 238)
ON DUPLICATE KEY UPDATE
    is_public = VALUES(is_public),
    required_role = VALUES(required_role),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    is_active = VALUES(is_active),
    order_index = VALUES(order_index),
    updated_at = CURRENT_TIMESTAMP;
