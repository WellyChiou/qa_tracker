-- Step 2 seed data for risk rules

USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO risk_rule (rule_code, rule_name, rule_type, severity, score_weight, enabled, description)
VALUES
('BREAK_COST', '跌破成本', 'DAILY_PORTFOLIO_RISK', 'MEDIUM', 1.0000, 1, '白話：目前價格低於你的平均成本。新手注意：不要因短期虧損就盲目攤平。'),
('CONSECUTIVE_DOWN', '連續下跌', 'DAILY_PORTFOLIO_RISK', 'MEDIUM', 1.0000, 1, '白話：最近連續幾天收盤走低。新手注意：先觀察是否止跌，不要情緒化加碼。'),
('BELOW_MA5', '跌破短期均線', 'DAILY_PORTFOLIO_RISK', 'MEDIUM', 1.0000, 1, '白話：收盤價低於短期均線，代表短線轉弱。新手注意：單一訊號不代表一定續跌。'),
('HIGH_VOLUME_DROP', '爆量下跌', 'DAILY_PORTFOLIO_RISK', 'HIGH', 1.0000, 1, '白話：放量且跌幅偏大，短線風險提升。新手注意：避免追價或衝動操作。'),
('LARGE_INTRADAY_SWING', '單日大幅震盪', 'DAILY_PORTFOLIO_RISK', 'MEDIUM', 1.0000, 1, '白話：單日高低振幅偏大。新手注意：高波動股票不宜只看短線紅綠。')
ON DUPLICATE KEY UPDATE
    rule_name = VALUES(rule_name),
    rule_type = VALUES(rule_type),
    severity = VALUES(severity),
    score_weight = VALUES(score_weight),
    enabled = VALUES(enabled),
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;
