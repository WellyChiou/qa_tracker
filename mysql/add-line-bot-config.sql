-- 添加 LINE Bot 配置（QR Code URL 和 Bot ID）
-- 執行此 SQL 以添加 LINE Bot 的 QR Code 和 Bot ID 配置

USE qa_tracker;

-- 插入 LINE Bot QR Code URL 配置（可選）
-- 將 QR Code 圖片上傳到可公開訪問的位置，然後填入 URL
INSERT INTO config (config_key, config_value, description) 
VALUES ('line_bot_qr_code_url', '', 'LINE Bot QR Code 圖片 URL（用於用戶掃描加入 Bot）')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 插入 LINE Bot 加入連結配置（推薦，優先使用）
-- 格式：https://line.me/R/ti/p/@您的ChannelID
-- 在 LINE Developers Console 的 Messaging API 頁面可以找到 Channel ID
INSERT INTO config (config_key, config_value, description) 
VALUES ('line_bot_join_url', '', 'LINE Bot 加入連結（用戶點擊即可加入 Bot）')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 插入 LINE Bot ID 配置（可選，如果沒有加入連結）
-- 在 LINE Developers Console 中找到 Channel ID 並填入
INSERT INTO config (config_key, config_value, description) 
VALUES ('line_bot_id', '', 'LINE Bot Channel ID（用於生成加入連結）')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 查看配置
SELECT config_key, 
       CASE 
         WHEN config_key = 'line_bot_qr_code_url' AND config_value != '' THEN '已配置'
         WHEN config_key = 'line_bot_join_url' AND config_value != '' THEN '已配置'
         WHEN config_key = 'line_bot_id' AND config_value != '' THEN '已配置'
         ELSE '未配置'
       END as status,
       CASE 
         WHEN config_key = 'line_bot_join_url' AND config_value != '' THEN LEFT(config_value, 50)
         WHEN config_key = 'line_bot_id' AND config_value != '' THEN config_value
         ELSE ''
       END as preview_value,
       description
FROM config 
WHERE config_key IN ('line_bot_qr_code_url', 'line_bot_join_url', 'line_bot_id');

