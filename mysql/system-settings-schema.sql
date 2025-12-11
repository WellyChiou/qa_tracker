-- ============================================
-- 系統參數設定表
-- ============================================
-- 用於存儲系統配置參數，包括備份設定等
-- ============================================

USE church;

-- 創建系統參數表
CREATE TABLE IF NOT EXISTS system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    setting_key VARCHAR(100) UNIQUE NOT NULL COMMENT '參數鍵（唯一）',
    setting_value TEXT COMMENT '參數值（JSON 或字串）',
    setting_type VARCHAR(20) NOT NULL DEFAULT 'string' COMMENT '參數類型：string, number, boolean, json',
    category VARCHAR(50) NOT NULL DEFAULT 'system' COMMENT '分類：backup, system, etc.',
    description VARCHAR(255) COMMENT '參數說明',
    is_editable TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可編輯（1=是，0=否）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_setting_key (setting_key),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系統參數設定表';

-- 插入預設的備份相關參數
INSERT INTO system_settings (setting_key, setting_value, setting_type, category, description, is_editable) VALUES
('backup.retention_days', '7', 'number', 'backup', '備份保留天數', 1),
('backup.schedule_time', '02:00', 'string', 'backup', '每日備份時間（HH:MM 格式）', 1),
('backup.enabled', 'true', 'boolean', 'backup', '是否啟用自動備份', 1),
('backup.mysql_service', 'mysql', 'string', 'backup', 'MySQL 服務名稱（docker compose 服務名）', 1),
('backup.mysql_root_password', 'rootpassword', 'string', 'backup', 'MySQL root 密碼', 1)
ON DUPLICATE KEY UPDATE 
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

-- 插入 LINE Bot 相關參數
-- 注意：使用 ON DUPLICATE KEY UPDATE 確保如果記錄已存在則更新值，不存在則插入
INSERT INTO system_settings (setting_key, setting_value, setting_type, category, description, is_editable) VALUES
('line.bot.channel-token', 'CY5CAs65/W4KfcU/6qTISLiqs2MCD9wHz/z/BF9vUexvUulGNrz4zPNQVy1saAlYggahICdEUiNLiVzkPYp6UDbX6QjMbvnjrkwBj750wd/nKS/sGNkJFaT705CF66mtF3LS4pLqJ1dx7TnA21+ZnQdB04t89/10/w1cDnyilFU=', 'string', 'linebot', 'LINE Bot Channel Token', 1),
('line.bot.channel-secret', '6075d3893ea1293b31307e8f673e26c2', 'string', 'linebot', 'LINE Bot Channel Secret', 1),
('line.bot.webhook-url', 'https://wc-project.duckdns.org/api/line/webhook', 'string', 'linebot', 'LINE Bot Webhook URL', 1),
('line.bot.daily-reminder-enabled', 'true', 'boolean', 'linebot', '是否啟用每日提醒', 1),
('line.bot.daily-reminder-time', '20:00', 'string', 'linebot', '每日提醒時間（HH:MM 格式）', 1),
('line.bot.admin-user-id', '', 'string', 'linebot', 'LINE Bot 管理員用戶 ID', 1),
('line.bot.church-group-id', 'C5246fe9f6b4f85ecbe0ff4e5299123bd', 'string', 'linebot', '教會 LINE 群組 ID（用於發送服事人員通知）', 1)
ON DUPLICATE KEY UPDATE 
    setting_value = VALUES(setting_value),
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

-- 插入 JWT 相關參數
INSERT INTO system_settings (setting_key, setting_value, setting_type, category, description, is_editable) VALUES
('jwt.secret', 'F/cPluFKK3/44X5iX9GdY6P7Ye+BIDdBTw6uljBTl9o=', 'string', 'jwt', 'JWT 密鑰（用於簽名 Token）', 1),
('jwt.access-token-expiration', '3600000', 'number', 'jwt', 'Access Token 過期時間（毫秒，預設1小時）', 1),
('jwt.refresh-token-enabled', 'true', 'boolean', 'jwt', '是否啟用 Refresh Token', 1),
('jwt.refresh-token-expiration', '604800000', 'number', 'jwt', 'Refresh Token 過期時間（毫秒，預設7天）', 1)
ON DUPLICATE KEY UPDATE 
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

-- 插入其他系統參數（可選）
INSERT INTO system_settings (setting_key, setting_value, setting_type, category, description, is_editable) VALUES
('system.timezone', 'Asia/Taipei', 'string', 'system', '系統時區', 1),
('system.language', 'zh_TW', 'string', 'system', '系統語言', 1)
ON DUPLICATE KEY UPDATE 
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

-- 顯示插入結果
SELECT 
    setting_key,
    setting_value,
    setting_type,
    category,
    description,
    is_editable
FROM system_settings
ORDER BY category, setting_key;

