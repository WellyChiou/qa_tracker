-- System Scheduler management ACL seed
-- Scope:
-- 1) 新增排程管理（新增/編輯/刪除/啟用停用）權限
-- 2) 新增 system scheduler management API URL 權限對接

USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
VALUES
('INVEST_SYS_SCHEDULER_MANAGE', '管理排程任務設定', 'system_scheduler', 'manage', '可新增/編輯/刪除/啟用停用排程任務設定')
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
      'INVEST_SYS_SCHEDULER_VIEW',
      'INVEST_SYS_SCHEDULER_RUN',
      'INVEST_SYS_SCHEDULER_MANAGE'
  )
WHERE r.role_name = 'ROLE_INVEST_ADMIN';

INSERT INTO url_permissions (url_pattern, http_method, is_public, required_role, required_permission, description, is_active, order_index)
VALUES
('/api/invest/system/scheduler/jobs/*', 'GET', 0, NULL, 'INVEST_SYS_SCHEDULER_VIEW', '查看單一系統排程任務', 1, 193),
('/api/invest/system/scheduler/jobs', 'POST', 0, NULL, 'INVEST_SYS_SCHEDULER_MANAGE', '新增系統排程任務設定', 1, 194),
('/api/invest/system/scheduler/jobs/*', 'PUT', 0, NULL, 'INVEST_SYS_SCHEDULER_MANAGE', '編輯系統排程任務設定', 1, 195),
('/api/invest/system/scheduler/jobs/*/toggle', 'PUT', 0, NULL, 'INVEST_SYS_SCHEDULER_MANAGE', '切換系統排程任務啟用狀態', 1, 196),
('/api/invest/system/scheduler/jobs/*', 'DELETE', 0, NULL, 'INVEST_SYS_SCHEDULER_MANAGE', '刪除系統排程任務設定', 1, 197)
ON DUPLICATE KEY UPDATE
    is_public = VALUES(is_public),
    required_role = VALUES(required_role),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    is_active = VALUES(is_active),
    order_index = VALUES(order_index),
    updated_at = CURRENT_TIMESTAMP;
