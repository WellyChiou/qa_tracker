-- V2 Phase 2-2: 歷史行情回補 ACL
-- 範圍：僅新增 run-price-backfill 所需最小權限

USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
VALUES
('INVEST_JOB_RUN_PRICE_BACKFILL', '執行歷史行情回補', 'price_backfill_job', 'run', '可手動執行持股與觀察清單歷史行情回補')
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    resource = VALUES(resource),
    action = VALUES(action),
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.permission_code = 'INVEST_JOB_RUN_PRICE_BACKFILL'
WHERE r.role_name = 'ROLE_INVEST_ADMIN';

INSERT INTO url_permissions (url_pattern, http_method, is_public, required_role, required_permission, description, is_active, order_index)
VALUES
('/api/invest/jobs/run-price-backfill', 'POST', 0, NULL, 'INVEST_JOB_RUN_PRICE_BACKFILL', '手動執行歷史行情回補', 1, 181)
ON DUPLICATE KEY UPDATE
    is_public = VALUES(is_public),
    required_role = VALUES(required_role),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    is_active = VALUES(is_active),
    order_index = VALUES(order_index),
    updated_at = CURRENT_TIMESTAMP;
