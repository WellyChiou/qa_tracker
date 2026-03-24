-- V2 Phase 2 ACL seed
-- Scope: strength / opportunity / watchlist / market-analysis job

USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
VALUES
('INVEST_STRENGTH_VIEW', '查看強勢股分析', 'strength_snapshot', 'view', '可查看強勢股分析列表與明細'),
('INVEST_OPPORTUNITY_VIEW', '查看機會股訊號', 'opportunity_signal', 'view', '可查看機會股訊號列表與明細'),
('INVEST_WATCHLIST_VIEW', '查看觀察清單', 'watchlist', 'view', '可查看預設觀察清單與標的'),
('INVEST_WATCHLIST_EDIT', '編輯觀察清單', 'watchlist', 'edit', '可新增/移除預設觀察清單標的'),
('INVEST_JOB_RUN_MARKET_ANALYSIS', '執行市場分析', 'market_analysis_job', 'run', '可手動執行強勢股/機會股分析')
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
      'INVEST_STRENGTH_VIEW',
      'INVEST_OPPORTUNITY_VIEW',
      'INVEST_WATCHLIST_VIEW',
      'INVEST_WATCHLIST_EDIT',
      'INVEST_JOB_RUN_MARKET_ANALYSIS'
  )
WHERE r.role_name = 'ROLE_INVEST_ADMIN';

INSERT INTO menu_items (
    menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description
)
SELECT
    'INVEST_STRENGTH_STOCKS',
    '強勢股分析',
    '💪',
    '/invest-admin/strong-stocks',
    p.id,
    60,
    1,
    1,
    'INVEST_STRENGTH_VIEW',
    '強勢股分析列表與明細'
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

INSERT INTO menu_items (
    menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description
)
SELECT
    'INVEST_OPPORTUNITY_SIGNALS',
    '機會股觀察',
    '🧭',
    '/invest-admin/opportunities',
    p.id,
    70,
    1,
    1,
    'INVEST_OPPORTUNITY_VIEW',
    '機會股觀察訊號列表'
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
('/api/invest/jobs/run-market-analysis', 'POST', 0, NULL, 'INVEST_JOB_RUN_MARKET_ANALYSIS', '手動執行市場分析', 1, 180),
('/api/invest/strength-snapshots/paged', 'GET', 0, NULL, 'INVEST_STRENGTH_VIEW', '查看強勢股列表', 1, 181),
('/api/invest/strength-snapshots/*', 'GET', 0, NULL, 'INVEST_STRENGTH_VIEW', '查看強勢股明細', 1, 182),
('/api/invest/opportunity-signals/paged', 'GET', 0, NULL, 'INVEST_OPPORTUNITY_VIEW', '查看機會股列表', 1, 183),
('/api/invest/opportunity-signals/*', 'GET', 0, NULL, 'INVEST_OPPORTUNITY_VIEW', '查看機會股明細', 1, 184),
('/api/invest/watchlists/default', 'GET', 0, NULL, 'INVEST_WATCHLIST_VIEW', '查看預設觀察清單', 1, 185),
('/api/invest/watchlists/default/items/paged', 'GET', 0, NULL, 'INVEST_WATCHLIST_VIEW', '查看預設觀察清單標的', 1, 186),
('/api/invest/watchlists/default/items', 'POST', 0, NULL, 'INVEST_WATCHLIST_EDIT', '新增預設觀察清單標的', 1, 187),
('/api/invest/watchlists/default/items/*', 'DELETE', 0, NULL, 'INVEST_WATCHLIST_EDIT', '移除預設觀察清單標的', 1, 188)
ON DUPLICATE KEY UPDATE
    is_public = VALUES(is_public),
    required_role = VALUES(required_role),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    is_active = VALUES(is_active),
    order_index = VALUES(order_index),
    updated_at = CURRENT_TIMESTAMP;
