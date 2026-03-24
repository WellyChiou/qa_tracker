USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
VALUES
('INVEST_STRATEGY_VIEW', '檢視策略設定', 'strategy_settings', 'view', '可檢視 Invest 策略門檻設定'),
('INVEST_STRATEGY_EDIT', '編輯策略設定', 'strategy_settings', 'edit', '可編輯 Invest 策略門檻設定並升版')
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
  ON p.permission_code IN ('INVEST_STRATEGY_VIEW', 'INVEST_STRATEGY_EDIT')
WHERE r.role_name = 'ROLE_INVEST_ADMIN';

INSERT INTO system_settings (setting_key, setting_value, setting_type, category, description, is_editable)
VALUES
('invest.strategy.version', '1', 'number', 'invest.strategy', '策略設定版本號（系統自動遞增）', 0),
('invest.strategy.strength.strong_min', '80', 'number', 'invest.strategy', 'STRONG 最低分數門檻', 1),
('invest.strategy.strength.good_min', '60', 'number', 'invest.strategy', 'GOOD 最低分數門檻', 1),
('invest.strategy.strength.weak_max', '39', 'number', 'invest.strategy', 'WEAK 最高分數門檻', 1),
('invest.strategy.data_quality.min_history_days', '20', 'number', 'invest.strategy', '資料品質判定最小歷史天數', 1),
('invest.strategy.data_quality.stale_days', '3', 'number', 'invest.strategy', '資料品質判定 stale 日曆天數', 1),
('invest.strategy.opportunity.observe_min_score', '80', 'number', 'invest.strategy', 'OBSERVE 最低分數門檻', 1),
('invest.strategy.opportunity.wait_pullback_min_score', '65', 'number', 'invest.strategy', 'WAIT_PULLBACK 最低分數門檻', 1),
('invest.strategy.opportunity.reevaluate_min_score', '45', 'number', 'invest.strategy', 'REEVALUATE 最低分數門檻', 1)
ON DUPLICATE KEY UPDATE
    setting_value = VALUES(setting_value),
    setting_type = VALUES(setting_type),
    category = VALUES(category),
    description = VALUES(description),
    is_editable = VALUES(is_editable),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO url_permissions (url_pattern, http_method, is_public, required_role, required_permission, description, is_active, order_index)
SELECT '/api/invest/system/strategy-settings', 'GET', 0, NULL, 'INVEST_STRATEGY_VIEW', '查詢 Invest 策略設定', 1, 291
WHERE NOT EXISTS (
    SELECT 1 FROM url_permissions
    WHERE url_pattern = '/api/invest/system/strategy-settings'
      AND http_method = 'GET'
);

INSERT INTO url_permissions (url_pattern, http_method, is_public, required_role, required_permission, description, is_active, order_index)
SELECT '/api/invest/system/strategy-settings', 'PUT', 0, NULL, 'INVEST_STRATEGY_EDIT', '更新 Invest 策略設定', 1, 292
WHERE NOT EXISTS (
    SELECT 1 FROM url_permissions
    WHERE url_pattern = '/api/invest/system/strategy-settings'
      AND http_method = 'PUT'
);
