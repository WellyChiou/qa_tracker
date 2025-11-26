-- 添加資產表的 name 和 currency 欄位
-- 執行此 SQL 以更新現有數據庫

USE qa_tracker;

-- 添加 name 欄位（如果不存在）
ALTER TABLE assets 
ADD COLUMN IF NOT EXISTS name VARCHAR(200) COMMENT '資產名稱' AFTER asset_type;

-- 添加 currency 欄位（如果不存在）
ALTER TABLE assets 
ADD COLUMN IF NOT EXISTS currency VARCHAR(10) DEFAULT 'TWD' COMMENT '幣別：TWD、USD、EUR、JPY、CNY' AFTER name;

-- 為現有記錄生成資產名稱和幣別
UPDATE assets 
SET 
    name = CASE 
        WHEN stock_code IS NULL THEN COALESCE(asset_type, '資產')
        WHEN stock_code REGEXP '^[A-Za-z]' THEN CONCAT(stock_code, ' ', COALESCE(asset_type, '資產'))
        WHEN stock_code LIKE '00%' THEN CONCAT('台股 ETF ', stock_code)
        ELSE CONCAT('台股 ', stock_code)
    END,
    currency = CASE 
        WHEN stock_code IS NULL THEN 'TWD'
        WHEN stock_code REGEXP '^[A-Za-z]' THEN 'USD'
        ELSE 'TWD'
    END
WHERE name IS NULL OR currency IS NULL OR name = '' OR currency = '';

