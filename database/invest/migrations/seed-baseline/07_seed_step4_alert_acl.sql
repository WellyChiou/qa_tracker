-- Step 4 ACL seed: alert setting/event + alert polling run-now

USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
VALUES
('INVEST_ALERT_SETTING_VIEW', '查看警示設定', 'portfolio_alert_setting', 'view', '可查看持股警示設定'),
('INVEST_ALERT_SETTING_EDIT', '編輯警示設定', 'portfolio_alert_setting', 'edit', '可編輯持股警示設定'),
('INVEST_ALERT_EVENT_VIEW', '查看警示事件', 'portfolio_alert_event', 'view', '可查看警示事件列表與摘要'),
('INVEST_JOB_RUN_ALERT_POLLING', '執行警示輪詢', 'alert_polling_job', 'run', '可手動執行警示輪詢')
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
      'INVEST_ALERT_SETTING_VIEW',
      'INVEST_ALERT_SETTING_EDIT',
      'INVEST_ALERT_EVENT_VIEW',
      'INVEST_JOB_RUN_ALERT_POLLING'
  )
WHERE r.role_name = 'ROLE_INVEST_ADMIN';

INSERT INTO menu_items (
    menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description
)
SELECT
    'INVEST_ALERT_EVENTS',
    '警示事件',
    '🚨',
    '/invest-admin/alerts',
    p.id,
    50,
    1,
    1,
    'INVEST_ALERT_EVENT_VIEW',
    '持股警示事件列表'
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
('/api/invest/portfolio-alert-settings/*', 'GET', 0, NULL, 'INVEST_ALERT_SETTING_VIEW', '查看持股警示設定', 1, 160),
('/api/invest/portfolio-alert-settings/*', 'PUT', 0, NULL, 'INVEST_ALERT_SETTING_EDIT', '編輯持股警示設定', 1, 161),
('/api/invest/portfolio-alert-events/paged', 'GET', 0, NULL, 'INVEST_ALERT_EVENT_VIEW', '查看警示事件列表', 1, 162),
('/api/invest/portfolio-alert-events/latest', 'GET', 0, NULL, 'INVEST_ALERT_EVENT_VIEW', '查看最新警示事件', 1, 163),
('/api/invest/jobs/run-alert-polling', 'POST', 0, NULL, 'INVEST_JOB_RUN_ALERT_POLLING', '手動執行警示輪詢', 1, 164)
ON DUPLICATE KEY UPDATE
    is_public = VALUES(is_public),
    required_role = VALUES(required_role),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    is_active = VALUES(is_active),
    order_index = VALUES(order_index),
    updated_at = CURRENT_TIMESTAMP;
