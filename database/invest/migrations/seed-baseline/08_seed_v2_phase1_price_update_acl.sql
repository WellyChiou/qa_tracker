-- V2 Phase 1-1 ACL seed
-- Scope: manual run price update for current user holdings

USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
VALUES
('INVEST_JOB_RUN_PRICE_UPDATE', '執行持股行情更新', 'price_update_job', 'run', '可手動執行目前登入者持股行情更新')
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    resource = VALUES(resource),
    action = VALUES(action),
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.permission_code = 'INVEST_JOB_RUN_PRICE_UPDATE'
WHERE r.role_name = 'ROLE_INVEST_ADMIN';

INSERT INTO url_permissions (url_pattern, http_method, is_public, required_role, required_permission, description, is_active, order_index)
VALUES
('/api/invest/jobs/run-price-update', 'POST', 0, NULL, 'INVEST_JOB_RUN_PRICE_UPDATE', '手動執行持股行情更新', 1, 170)
ON DUPLICATE KEY UPDATE
    is_public = VALUES(is_public),
    required_role = VALUES(required_role),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    is_active = VALUES(is_active),
    order_index = VALUES(order_index),
    updated_at = CURRENT_TIMESTAMP;
