-- Step 3 ACL seed: Daily report + job log + run-now job
-- 範圍：最小權限，不擴張系統設定骨架

USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
VALUES
('INVEST_DAILY_REPORT_VIEW', '查看每日報告', 'daily_report', 'view', '可查看每日報告列表與明細'),
('INVEST_JOB_RUN_DAILY_REPORT', '執行每日報告批次', 'daily_report_job', 'run', '可手動執行每日報告批次'),
('INVEST_SCHEDULER_JOB_LOG_VIEW', '查看批次執行紀錄', 'scheduler_job_log', 'view', '可查看每日批次執行紀錄')
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
      'INVEST_DAILY_REPORT_VIEW',
      'INVEST_JOB_RUN_DAILY_REPORT',
      'INVEST_SCHEDULER_JOB_LOG_VIEW'
  )
WHERE r.role_name = 'ROLE_INVEST_ADMIN';

INSERT INTO menu_items (
    menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description
)
SELECT
    'INVEST_DAILY_REPORTS',
    '每日報告',
    '🗓️',
    '/invest-admin/daily-reports',
    p.id,
    40,
    1,
    1,
    'INVEST_DAILY_REPORT_VIEW',
    'Invest 每日持股風險報告'
FROM menu_items p
WHERE p.menu_code = 'INVEST_BASIC_FEATURES'
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    icon = VALUES(icon),
    url = VALUES(url),
    parent_id = VALUES(parent_id),
    order_index = VALUES(order_index),
    is_active = VALUES(is_active),
    show_in_dashboard = VALUES(show_in_dashboard),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO url_permissions (url_pattern, http_method, is_public, required_role, required_permission, description, is_active, order_index)
VALUES
('/api/invest/daily-reports/paged', 'GET', 0, NULL, 'INVEST_DAILY_REPORT_VIEW', '查看每日報告列表', 1, 150),
('/api/invest/daily-reports/*', 'GET', 0, NULL, 'INVEST_DAILY_REPORT_VIEW', '查看每日報告明細', 1, 151),
('/api/invest/daily-reports/latest', 'GET', 0, NULL, 'INVEST_DAILY_REPORT_VIEW', '查看最新每日報告', 1, 152),
('/api/invest/scheduler-job-logs/paged', 'GET', 0, NULL, 'INVEST_SCHEDULER_JOB_LOG_VIEW', '查看批次執行紀錄', 1, 153),
('/api/invest/jobs/run-daily-portfolio-risk-report', 'POST', 0, NULL, 'INVEST_JOB_RUN_DAILY_REPORT', '手動執行每日報告批次', 1, 154)
ON DUPLICATE KEY UPDATE
    is_public = VALUES(is_public),
    required_role = VALUES(required_role),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    is_active = VALUES(is_active),
    order_index = VALUES(order_index),
    updated_at = CURRENT_TIMESTAMP;
