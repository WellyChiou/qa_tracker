-- 正式維運資料：補齊股票主資料與 admin 持股（非 demo）
-- 執行前提：invest ACL baseline 已建立，且 users.username='admin' 存在

USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 1) 股票主資料（is_active=1，供 /api/invest/stocks/options 使用）
INSERT INTO stock (market, ticker, name, industry, is_active)
VALUES
    ('TW', '5880', '合庫金控', '金融', 1),
    ('TW', '00692', '富邦公司治理', 'ETF', 1),
    ('TW', '00937B', '群益 ESG 投等債 20+', 'ETF', 1),
    ('US', 'VOO', 'Vanguard S&P 500 ETF', 'ETF', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    industry = VALUES(industry),
    is_active = VALUES(is_active),
    updated_at = CURRENT_TIMESTAMP;

-- 2) 持股資料：綁定目前 admin 使用者（users.uid）
--    規則：total_cost = ROUND(avg_cost * quantity, 2)（HALF_UP 由 MySQL ROUND 實際執行）
SET @admin_uid = (
    SELECT uid FROM users WHERE username = 'admin' AND is_enabled = 1 LIMIT 1
);

-- 5880
SET @stock_id_5880 = (
    SELECT id FROM stock WHERE market = 'TW' AND ticker = '5880' LIMIT 1
);
UPDATE portfolio
SET avg_cost = 23.15,
    quantity = 37513,
    total_cost = ROUND(23.15 * 37513, 2),
    note = '幣別:TWD',
    is_active = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = @admin_uid AND stock_id = @stock_id_5880;
INSERT INTO portfolio (user_id, stock_id, avg_cost, quantity, total_cost, note, is_active)
SELECT @admin_uid, @stock_id_5880, 23.15, 37513, ROUND(23.15 * 37513, 2), '幣別:TWD', 1
WHERE @admin_uid IS NOT NULL
  AND @stock_id_5880 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM portfolio
      WHERE user_id = @admin_uid AND stock_id = @stock_id_5880 AND is_active = 1
  );

-- 00692
SET @stock_id_00692 = (
    SELECT id FROM stock WHERE market = 'TW' AND ticker = '00692' LIMIT 1
);
UPDATE portfolio
SET avg_cost = 31.98,
    quantity = 3321,
    total_cost = ROUND(31.98 * 3321, 2),
    note = '幣別:TWD',
    is_active = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = @admin_uid AND stock_id = @stock_id_00692;
INSERT INTO portfolio (user_id, stock_id, avg_cost, quantity, total_cost, note, is_active)
SELECT @admin_uid, @stock_id_00692, 31.98, 3321, ROUND(31.98 * 3321, 2), '幣別:TWD', 1
WHERE @admin_uid IS NOT NULL
  AND @stock_id_00692 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM portfolio
      WHERE user_id = @admin_uid AND stock_id = @stock_id_00692 AND is_active = 1
  );

-- 00937B
SET @stock_id_00937B = (
    SELECT id FROM stock WHERE market = 'TW' AND ticker = '00937B' LIMIT 1
);
UPDATE portfolio
SET avg_cost = 9.97,
    quantity = 6000,
    total_cost = ROUND(9.97 * 6000, 2),
    note = '幣別:TWD',
    is_active = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = @admin_uid AND stock_id = @stock_id_00937B;
INSERT INTO portfolio (user_id, stock_id, avg_cost, quantity, total_cost, note, is_active)
SELECT @admin_uid, @stock_id_00937B, 9.97, 6000, ROUND(9.97 * 6000, 2), '幣別:TWD', 1
WHERE @admin_uid IS NOT NULL
  AND @stock_id_00937B IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM portfolio
      WHERE user_id = @admin_uid AND stock_id = @stock_id_00937B AND is_active = 1
  );

-- VOO
SET @stock_id_VOO = (
    SELECT id FROM stock WHERE market = 'US' AND ticker = 'VOO' LIMIT 1
);
UPDATE portfolio
SET avg_cost = 399.13,
    quantity = 58,
    total_cost = ROUND(399.13 * 58, 2),
    note = '幣別:USD',
    is_active = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = @admin_uid AND stock_id = @stock_id_VOO;
INSERT INTO portfolio (user_id, stock_id, avg_cost, quantity, total_cost, note, is_active)
SELECT @admin_uid, @stock_id_VOO, 399.13, 58, ROUND(399.13 * 58, 2), '幣別:USD', 1
WHERE @admin_uid IS NOT NULL
  AND @stock_id_VOO IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM portfolio
      WHERE user_id = @admin_uid AND stock_id = @stock_id_VOO AND is_active = 1
  );
