-- System Scheduler runtime ACL seed
-- Scope:
-- 1) 新增系統排程 Run Now 權限
-- 2) 新增 system scheduler API URL 權限對接

USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
VALUES
('INVEST_SYS_SCHEDULER_RUN', '執行排程管理 Run Now', 'system_scheduler', 'run', '可透過系統排程管理頁執行 Run Now')
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
      'INVEST_SYS_SCHEDULER_RUN'
  )
WHERE r.role_name = 'ROLE_INVEST_ADMIN';

INSERT INTO url_permissions (url_pattern, http_method, is_public, required_role, required_permission, description, is_active, order_index)
VALUES
('/api/invest/system/scheduler/jobs', 'GET', 0, NULL, 'INVEST_SYS_SCHEDULER_VIEW', '查看系統排程任務列表', 1, 190),
('/api/invest/system/scheduler/jobs/*/logs/paged', 'GET', 0, NULL, 'INVEST_SYS_SCHEDULER_VIEW', '查看系統排程任務執行紀錄', 1, 191),
('/api/invest/system/scheduler/jobs/*/run-now', 'POST', 0, NULL, 'INVEST_SYS_SCHEDULER_RUN', '執行系統排程任務 Run Now', 1, 192)
ON DUPLICATE KEY UPDATE
    is_public = VALUES(is_public),
    required_role = VALUES(required_role),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    is_active = VALUES(is_active),
    order_index = VALUES(order_index),
    updated_at = CURRENT_TIMESTAMP;
