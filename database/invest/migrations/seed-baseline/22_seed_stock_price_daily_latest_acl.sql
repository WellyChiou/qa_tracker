USE invest;

INSERT INTO url_permissions (
    url_pattern,
    http_method,
    is_public,
    required_role,
    required_permission,
    description,
    is_active,
    order_index
)
SELECT
    '/api/invest/stock-price-dailies/latest',
    'GET',
    0,
    NULL,
    'INVEST_STOCK_PRICE_DAILY_VIEW',
    'Invest 每日行情最新視圖',
    1,
    121
WHERE NOT EXISTS (
    SELECT 1
    FROM url_permissions
    WHERE url_pattern = '/api/invest/stock-price-dailies/latest'
      AND http_method = 'GET'
);
