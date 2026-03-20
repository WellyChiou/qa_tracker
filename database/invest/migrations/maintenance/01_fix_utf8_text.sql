-- Fix mojibake text in invest seeded data
-- Root cause: seed scripts executed without SET NAMES utf8mb4 on existing environments.

USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Step 1 seed text
UPDATE stock
SET name = '台積電',
    industry = '半導體',
    updated_at = CURRENT_TIMESTAMP
WHERE market = 'TW' AND ticker = '2330';

UPDATE portfolio p
INNER JOIN stock s ON s.id = p.stock_id
SET p.note = '台股核心持股',
    p.updated_at = CURRENT_TIMESTAMP
WHERE s.market = 'TW' AND s.ticker = '2330';

UPDATE portfolio p
INNER JOIN stock s ON s.id = p.stock_id
SET p.note = '美股觀察持股',
    p.updated_at = CURRENT_TIMESTAMP
WHERE s.market = 'US' AND s.ticker = 'AAPL';

-- Step 2 risk rules text
UPDATE risk_rule
SET rule_name = '跌破成本',
    description = '白話：目前價格低於你的平均成本。新手注意：不要因短期虧損就盲目攤平。',
    updated_at = CURRENT_TIMESTAMP
WHERE rule_code = 'BREAK_COST';

UPDATE risk_rule
SET rule_name = '連續下跌',
    description = '白話：最近連續幾天收盤走低。新手注意：先觀察是否止跌，不要情緒化加碼。',
    updated_at = CURRENT_TIMESTAMP
WHERE rule_code = 'CONSECUTIVE_DOWN';

UPDATE risk_rule
SET rule_name = '跌破短期均線',
    description = '白話：收盤價低於短期均線，代表短線轉弱。新手注意：單一訊號不代表一定續跌。',
    updated_at = CURRENT_TIMESTAMP
WHERE rule_code = 'BELOW_MA5';

UPDATE risk_rule
SET rule_name = '爆量下跌',
    description = '白話：放量且跌幅偏大，短線風險提升。新手注意：避免追價或衝動操作。',
    updated_at = CURRENT_TIMESTAMP
WHERE rule_code = 'HIGH_VOLUME_DROP';

UPDATE risk_rule
SET rule_name = '單日大幅震盪',
    description = '白話：單日高低振幅偏大。新手注意：高波動股票不宜只看短線紅綠。',
    updated_at = CURRENT_TIMESTAMP
WHERE rule_code = 'LARGE_INTRADAY_SWING';

-- ACL seed text
UPDATE roles
SET description = 'Invest 系統管理員',
    updated_at = CURRENT_TIMESTAMP
WHERE role_name = 'ROLE_INVEST_ADMIN';

UPDATE users
SET display_name = 'Invest 系統管理員',
    updated_at = CURRENT_TIMESTAMP
WHERE uid = 'invest-admin-001';

UPDATE permissions
SET permission_name = '查看 Invest Dashboard',
    description = '可查看持股總覽 Dashboard',
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code = 'INVEST_DASHBOARD_VIEW';

UPDATE permissions
SET permission_name = '查看持股',
    description = '可查看持股列表與明細',
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code = 'INVEST_PORTFOLIO_VIEW';

UPDATE permissions
SET permission_name = '編輯持股',
    description = '可新增/修改/刪除持股',
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code = 'INVEST_PORTFOLIO_EDIT';

UPDATE permissions
SET permission_name = '查看股票主資料',
    description = '可查看股票主資料',
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code = 'INVEST_STOCK_VIEW';

UPDATE permissions
SET permission_name = '編輯股票主資料',
    description = '可新增/修改/刪除股票主資料',
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code = 'INVEST_STOCK_EDIT';

UPDATE permissions
SET permission_name = '查看每日行情',
    description = '可查看每日行情',
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code = 'INVEST_STOCK_PRICE_DAILY_VIEW';

UPDATE permissions
SET permission_name = '編輯每日行情',
    description = '可新增/修改/刪除每日行情',
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code = 'INVEST_STOCK_PRICE_DAILY_EDIT';

UPDATE permissions
SET permission_name = '查看風險分析',
    description = '可查看持股風險分析結果',
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code = 'INVEST_RISK_RESULT_VIEW';

UPDATE permissions
SET permission_name = '手動重算風險',
    description = '可手動重算風險分析結果',
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code = 'INVEST_RISK_RECALCULATE';

UPDATE menu_items
SET menu_name = 'Invest Dashboard',
    icon = '📊',
    url = '/invest-admin/',
    required_permission = 'INVEST_DASHBOARD_VIEW',
    description = 'Invest 儀表板',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'INVEST_DASHBOARD';

UPDATE menu_items
SET menu_name = '持股中心',
    icon = '💼',
    url = '/invest-admin/portfolios',
    required_permission = 'INVEST_PORTFOLIO_VIEW',
    description = '持股列表與明細',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'INVEST_PORTFOLIOS';

UPDATE menu_items
SET menu_name = '股票主資料',
    icon = '🏷️',
    url = '/invest-admin/stocks',
    required_permission = 'INVEST_STOCK_VIEW',
    description = '股票主資料管理',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'INVEST_STOCKS';

UPDATE menu_items
SET menu_name = '每日行情',
    icon = '📈',
    url = '/invest-admin/stock-price-dailies',
    required_permission = 'INVEST_STOCK_PRICE_DAILY_VIEW',
    description = '每日行情管理',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'INVEST_STOCK_PRICE_DAILIES';

UPDATE menu_items
SET menu_name = '風險分析結果',
    icon = '⚠️',
    url = '/invest-admin/portfolio-risk-results',
    required_permission = 'INVEST_RISK_RESULT_VIEW',
    is_active = 0,
    show_in_dashboard = 0,
    description = '持股風險分析結果（預留，尚未開放前端頁面）',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'INVEST_RISK_RESULTS';
