-- 移除已廢棄的每日提醒相關系統參數
-- 執行此腳本以清理 system_settings 表中的無用數據

DELETE FROM system_settings 
WHERE setting_key IN (
    'line.bot.daily-reminder-enabled', 
    'line.bot.daily-reminder-time'
);

-- 確認刪除結果
SELECT * FROM system_settings WHERE category = 'linebot';

