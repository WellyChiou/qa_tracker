-- Step 1 seed data for Invest domain

USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO stock (market, ticker, name, industry, is_active)
VALUES
('TW', '2330', '台積電', '半導體', 1),
('US', 'AAPL', 'Apple Inc.', 'Technology', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    industry = VALUES(industry),
    is_active = VALUES(is_active),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO stock_price_daily (
    stock_id, trade_date, open_price, high_price, low_price, close_price, volume, change_amount, change_percent
)
SELECT s.id, '2026-03-16', 980.0, 995.0, 976.0, 990.0, 31000000, 8.0, 0.81
FROM stock s
WHERE s.market = 'TW' AND s.ticker = '2330'
  AND NOT EXISTS (
      SELECT 1 FROM stock_price_daily d WHERE d.stock_id = s.id AND d.trade_date = '2026-03-16'
  );

INSERT INTO stock_price_daily (
    stock_id, trade_date, open_price, high_price, low_price, close_price, volume, change_amount, change_percent
)
SELECT s.id, '2026-03-17', 991.0, 1002.0, 985.0, 997.0, 28400000, 7.0, 0.71
FROM stock s
WHERE s.market = 'TW' AND s.ticker = '2330'
  AND NOT EXISTS (
      SELECT 1 FROM stock_price_daily d WHERE d.stock_id = s.id AND d.trade_date = '2026-03-17'
  );

INSERT INTO stock_price_daily (
    stock_id, trade_date, open_price, high_price, low_price, close_price, volume, change_amount, change_percent
)
SELECT s.id, '2026-03-18', 998.0, 1008.0, 993.0, 1005.0, 26500000, 8.0, 0.80
FROM stock s
WHERE s.market = 'TW' AND s.ticker = '2330'
  AND NOT EXISTS (
      SELECT 1 FROM stock_price_daily d WHERE d.stock_id = s.id AND d.trade_date = '2026-03-18'
  );

INSERT INTO stock_price_daily (
    stock_id, trade_date, open_price, high_price, low_price, close_price, volume, change_amount, change_percent
)
SELECT s.id, '2026-03-16', 189.2, 191.4, 188.1, 190.9, 51400000, 1.3, 0.69
FROM stock s
WHERE s.market = 'US' AND s.ticker = 'AAPL'
  AND NOT EXISTS (
      SELECT 1 FROM stock_price_daily d WHERE d.stock_id = s.id AND d.trade_date = '2026-03-16'
  );

INSERT INTO stock_price_daily (
    stock_id, trade_date, open_price, high_price, low_price, close_price, volume, change_amount, change_percent
)
SELECT s.id, '2026-03-17', 191.0, 193.2, 190.6, 192.8, 48200000, 1.9, 1.00
FROM stock s
WHERE s.market = 'US' AND s.ticker = 'AAPL'
  AND NOT EXISTS (
      SELECT 1 FROM stock_price_daily d WHERE d.stock_id = s.id AND d.trade_date = '2026-03-17'
  );

INSERT INTO stock_price_daily (
    stock_id, trade_date, open_price, high_price, low_price, close_price, volume, change_amount, change_percent
)
SELECT s.id, '2026-03-18', 193.0, 194.4, 191.8, 193.7, 46700000, 0.9, 0.47
FROM stock s
WHERE s.market = 'US' AND s.ticker = 'AAPL'
  AND NOT EXISTS (
      SELECT 1 FROM stock_price_daily d WHERE d.stock_id = s.id AND d.trade_date = '2026-03-18'
  );

INSERT INTO portfolio (user_id, stock_id, avg_cost, quantity, total_cost, note, is_active)
SELECT 'invest-admin-001', s.id, 950.0000, 10.0000, 9500.00, '台股核心持股', 1
FROM stock s
WHERE s.market = 'TW' AND s.ticker = '2330'
  AND NOT EXISTS (
      SELECT 1 FROM portfolio p WHERE p.user_id = 'invest-admin-001' AND p.stock_id = s.id
  );

INSERT INTO portfolio (user_id, stock_id, avg_cost, quantity, total_cost, note, is_active)
SELECT 'invest-admin-001', s.id, 180.0000, 20.0000, 3600.00, '美股觀察持股', 1
FROM stock s
WHERE s.market = 'US' AND s.ticker = 'AAPL'
  AND NOT EXISTS (
      SELECT 1 FROM portfolio p WHERE p.user_id = 'invest-admin-001' AND p.stock_id = s.id
  );

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
