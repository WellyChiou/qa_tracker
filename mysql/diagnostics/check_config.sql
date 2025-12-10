-- 檢查 LINE Bot 配置
SELECT config_key, config_value 
FROM config 
WHERE config_key LIKE '%daily%reminder%' 
   OR config_key LIKE '%line%';

-- 檢查用戶是否有 LINE ID
SELECT display_name, line_user_id 
FROM users 
WHERE line_user_id IS NOT NULL AND line_user_id != '';
