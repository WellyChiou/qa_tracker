-- 1. 建立 personal 資料庫的 system_settings 表
CREATE TABLE IF NOT EXISTS `system_settings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(50) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `is_editable` bit(1) NOT NULL,
  `setting_key` varchar(100) NOT NULL,
  `setting_type` varchar(20) NOT NULL,
  `setting_value` text,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_system_settings_key` (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. 將 config 表的資料遷移到 system_settings
-- 使用 INSERT IGNORE 避免重複鍵錯誤
-- 自動判斷分類與類型
INSERT IGNORE INTO `system_settings` 
(`setting_key`, `setting_value`, `description`, `category`, `setting_type`, `is_editable`, `created_at`, `updated_at`)
SELECT 
    config_key, 
    config_value, 
    description,
    CASE 
        WHEN config_key LIKE 'line.bot.%' THEN 'linebot'
        WHEN config_key LIKE 'backup.%' THEN 'backup'
        WHEN config_key LIKE 'jwt.%' THEN 'jwt'
        ELSE 'system'
    END as category,
    CASE 
        WHEN config_value = 'true' OR config_value = 'false' THEN 'boolean'
        WHEN config_key LIKE '%_days' OR config_key LIKE '%_expiration' THEN 'number'
        ELSE 'string'
    END as setting_type,
    1 as is_editable,
    NOW(),
    NOW()
FROM `config`;

-- 3. 刪除舊的 config 表
-- 請確認資料已成功遷移後再執行此行 (建議先註解掉或手動確認)
-- DROP TABLE `config`;

