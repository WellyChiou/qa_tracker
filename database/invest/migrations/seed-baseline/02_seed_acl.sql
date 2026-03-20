-- Slice C seed: Invest ACL data

USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO roles (role_name, description)
VALUES
('ROLE_INVEST_ADMIN', 'Invest 系統管理員')
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
VALUES
('INVEST_DASHBOARD_VIEW', '查看 Invest Dashboard', 'invest_dashboard', 'view', '可查看持股總覽 Dashboard'),
('INVEST_PORTFOLIO_VIEW', '查看持股', 'portfolio', 'view', '可查看持股列表與明細'),
('INVEST_PORTFOLIO_EDIT', '編輯持股', 'portfolio', 'edit', '可新增/修改/刪除持股'),
('INVEST_STOCK_VIEW', '查看股票主資料', 'stock', 'view', '可查看股票主資料'),
('INVEST_STOCK_EDIT', '編輯股票主資料', 'stock', 'edit', '可新增/修改/刪除股票主資料'),
('INVEST_STOCK_PRICE_DAILY_VIEW', '查看每日行情', 'stock_price_daily', 'view', '可查看每日行情'),
('INVEST_STOCK_PRICE_DAILY_EDIT', '編輯每日行情', 'stock_price_daily', 'edit', '可新增/修改/刪除每日行情'),
('INVEST_RISK_RESULT_VIEW', '查看風險分析', 'portfolio_risk_result', 'view', '可查看持股風險分析結果'),
('INVEST_RISK_RECALCULATE', '手動重算風險', 'portfolio_risk_result', 'recalculate', '可手動重算風險分析結果')
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    resource = VALUES(resource),
    action = VALUES(action),
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_INVEST_ADMIN'
  AND p.permission_code LIKE 'INVEST_%';

INSERT INTO users (
    uid,
    username,
    email,
    password,
    display_name,
    provider_id,
    is_enabled,
    is_account_non_locked,
    created_at,
    updated_at
)
VALUES (
    'invest-admin-001',
    'admin',
    'admin@invest.local',
    '$2a$10$lUNZEYnqKQQAr8D3kZqCb.iSU0L7RWMIYLFP7LMqRANshdLcGpO8a',
    'Invest 系統管理員',
    'local',
    1,
    1,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    email = VALUES(email),
    password = VALUES(password),
    display_name = VALUES(display_name),
    provider_id = VALUES(provider_id),
    is_enabled = VALUES(is_enabled),
    is_account_non_locked = VALUES(is_account_non_locked),
    updated_at = CURRENT_TIMESTAMP;

INSERT IGNORE INTO user_roles (user_uid, role_id, created_at)
SELECT 'invest-admin-001', r.id, NOW()
FROM roles r
WHERE r.role_name = 'ROLE_INVEST_ADMIN';

INSERT INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description)
VALUES
('INVEST_DASHBOARD', 'Invest Dashboard', '📊', '/invest-admin/', NULL, 1, 1, 1, 'INVEST_DASHBOARD_VIEW', 'Invest 儀表板'),
('INVEST_PORTFOLIOS', '持股中心', '💼', '/invest-admin/portfolios', NULL, 2, 1, 1, 'INVEST_PORTFOLIO_VIEW', '持股列表與明細'),
('INVEST_STOCKS', '股票主資料', '🏷️', '/invest-admin/stocks', NULL, 3, 1, 1, 'INVEST_STOCK_VIEW', '股票主資料管理'),
('INVEST_STOCK_PRICE_DAILIES', '每日行情', '📈', '/invest-admin/stock-price-dailies', NULL, 4, 1, 1, 'INVEST_STOCK_PRICE_DAILY_VIEW', '每日行情管理'),
('INVEST_RISK_RESULTS', '風險分析結果', '⚠️', '/invest-admin/portfolio-risk-results', NULL, 5, 0, 0, 'INVEST_RISK_RESULT_VIEW', '持股風險分析結果（預留，尚未開放前端頁面）')
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    icon = VALUES(icon),
    url = VALUES(url),
    order_index = VALUES(order_index),
    is_active = VALUES(is_active),
    show_in_dashboard = VALUES(show_in_dashboard),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO url_permissions (url_pattern, http_method, is_public, required_role, required_permission, description, is_active, order_index)
VALUES
('/api/invest/auth/login', 'POST', 1, NULL, NULL, 'Invest 登入', 1, 10),
('/api/invest/auth/refresh', 'POST', 1, NULL, NULL, 'Invest 刷新 token', 1, 11),
('/api/invest/auth/register', 'POST', 1, NULL, NULL, 'Invest 註冊', 1, 12),
('/api/invest/auth/current-user', 'GET', 1, NULL, NULL, 'Invest 取得目前使用者', 1, 13),
('/api/invest/hello', 'GET', 1, NULL, NULL, 'Invest 健康檢查', 1, 14),

('/api/invest/portfolio-dashboard/overview', 'GET', 0, NULL, 'INVEST_DASHBOARD_VIEW', '查看 dashboard', 1, 100),

('/api/invest/stocks/paged', 'GET', 0, NULL, 'INVEST_STOCK_VIEW', '查看股票列表 paged', 1, 110),
('/api/invest/stocks/options', 'GET', 0, NULL, 'INVEST_STOCK_VIEW', '查看股票 options', 1, 111),
('/api/invest/stocks/all', 'GET', 0, NULL, 'INVEST_STOCK_VIEW', '查看股票 all', 1, 112),
('/api/invest/stocks', 'POST', 0, NULL, 'INVEST_STOCK_EDIT', '新增股票', 1, 113),
('/api/invest/stocks/*', 'PUT', 0, NULL, 'INVEST_STOCK_EDIT', '更新股票', 1, 114),
('/api/invest/stocks/*', 'DELETE', 0, NULL, 'INVEST_STOCK_EDIT', '刪除股票', 1, 115),

('/api/invest/stock-price-dailies/paged', 'GET', 0, NULL, 'INVEST_STOCK_PRICE_DAILY_VIEW', '查看行情 paged', 1, 120),
('/api/invest/stock-price-dailies/all', 'GET', 0, NULL, 'INVEST_STOCK_PRICE_DAILY_VIEW', '查看行情 all', 1, 121),
('/api/invest/stock-price-dailies', 'POST', 0, NULL, 'INVEST_STOCK_PRICE_DAILY_EDIT', '新增行情', 1, 122),
('/api/invest/stock-price-dailies/*', 'PUT', 0, NULL, 'INVEST_STOCK_PRICE_DAILY_EDIT', '更新行情', 1, 123),
('/api/invest/stock-price-dailies/*', 'DELETE', 0, NULL, 'INVEST_STOCK_PRICE_DAILY_EDIT', '刪除行情', 1, 124),

('/api/invest/portfolios/paged', 'GET', 0, NULL, 'INVEST_PORTFOLIO_VIEW', '查看持股 paged', 1, 130),
('/api/invest/portfolios/all', 'GET', 0, NULL, 'INVEST_PORTFOLIO_VIEW', '查看持股 all', 1, 131),
('/api/invest/portfolios/*', 'GET', 0, NULL, 'INVEST_PORTFOLIO_VIEW', '查看持股明細', 1, 132),
('/api/invest/portfolios', 'POST', 0, NULL, 'INVEST_PORTFOLIO_EDIT', '新增持股', 1, 133),
('/api/invest/portfolios/*', 'PUT', 0, NULL, 'INVEST_PORTFOLIO_EDIT', '更新持股', 1, 134),
('/api/invest/portfolios/*', 'DELETE', 0, NULL, 'INVEST_PORTFOLIO_EDIT', '刪除持股', 1, 135),

('/api/invest/portfolio-risk-results/paged', 'GET', 0, NULL, 'INVEST_RISK_RESULT_VIEW', '查看風險結果 paged', 1, 140),
('/api/invest/portfolio-risk-results/*', 'GET', 0, NULL, 'INVEST_RISK_RESULT_VIEW', '查看風險結果明細', 1, 141),
('/api/invest/portfolio-risk-results/latest', 'GET', 0, NULL, 'INVEST_RISK_RESULT_VIEW', '查看最新風險結果', 1, 142),
('/api/invest/portfolio-risk-results/recalculate/*', 'POST', 0, NULL, 'INVEST_RISK_RECALCULATE', '手動重算風險', 1, 143)
ON DUPLICATE KEY UPDATE
    is_public = VALUES(is_public),
    required_role = VALUES(required_role),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    is_active = VALUES(is_active),
    order_index = VALUES(order_index),
    updated_at = CURRENT_TIMESTAMP;
