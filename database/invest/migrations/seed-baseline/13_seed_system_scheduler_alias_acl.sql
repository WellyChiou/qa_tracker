-- System Scheduler alias API ACL seed
-- Scope:
-- 1) 新增 church-style alias URL 權限
-- 2) 沿用既有 INVEST_SYS_SCHEDULER_VIEW / INVEST_SYS_SCHEDULER_RUN

USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO url_permissions (url_pattern, http_method, is_public, required_role, required_permission, description, is_active, order_index)
VALUES
('/api/invest/system/scheduler/jobs/*/execute', 'POST', 0, NULL, 'INVEST_SYS_SCHEDULER_RUN', '執行系統排程任務 execute alias', 1, 198),
('/api/invest/system/scheduler/jobs/*/executions', 'GET', 0, NULL, 'INVEST_SYS_SCHEDULER_VIEW', '查詢系統排程 execution 列表 alias', 1, 199),
('/api/invest/system/scheduler/jobs/*/executions/latest', 'GET', 0, NULL, 'INVEST_SYS_SCHEDULER_VIEW', '查詢系統排程最新 execution alias', 1, 200)
ON DUPLICATE KEY UPDATE
    is_public = VALUES(is_public),
    required_role = VALUES(required_role),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    is_active = VALUES(is_active),
    order_index = VALUES(order_index),
    updated_at = CURRENT_TIMESTAMP;
