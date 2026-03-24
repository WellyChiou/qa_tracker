USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO system_settings (setting_key, setting_value, setting_type, category, description, is_editable)
VALUES
('invest.analysis.fx.usd_to_twd', '32', 'number', 'invest.analysis', 'USD 轉 TWD 顯示匯率（用於彙總顯示）', 1)
ON DUPLICATE KEY UPDATE
    setting_value = VALUES(setting_value),
    setting_type = VALUES(setting_type),
    category = VALUES(category),
    description = VALUES(description),
    is_editable = VALUES(is_editable),
    updated_at = CURRENT_TIMESTAMP;

