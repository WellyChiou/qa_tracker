-- 移除個人網站和教會資料庫中的 LINE 群組 ID 配置
-- 教會群組現在統一使用個人網站資料庫的 line_groups 表管理
-- 透過 group_code = 'CHURCH_TECH_CONTROL' 識別

-- 移除個人網站資料庫（qa_tracker）中的配置
USE qa_tracker;

DELETE FROM system_settings 
WHERE setting_key = 'line.bot.church-group-id';

DELETE FROM system_settings 
WHERE setting_key = 'line.bot.church-group-ids';

-- 移除教會資料庫（church）中的 LINE Bot 相關配置
-- 這些配置現在統一從個人網站資料庫（qa_tracker）讀取
USE church;

-- 移除 LINE 群組 ID 配置
DELETE FROM system_settings 
WHERE setting_key = 'line.bot.church-group-id';

DELETE FROM system_settings 
WHERE setting_key = 'line.bot.church-group-ids';

-- 移除 LINE Bot 基本配置（現在統一從個人網站資料庫讀取）
DELETE FROM system_settings 
WHERE setting_key = 'line.bot.channel-token';

DELETE FROM system_settings 
WHERE setting_key = 'line.bot.channel-secret';

DELETE FROM system_settings 
WHERE setting_key = 'line.bot.webhook-url';

DELETE FROM system_settings 
WHERE setting_key = 'line.bot.admin-user-id';
