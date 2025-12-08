-- 更新教會系統 admin 用戶的密碼
-- 使用正確的 BCrypt hash

USE church;

-- 更新 admin 用戶的密碼
UPDATE users 
SET password = '$2a$10$KilvCsL.TYApEME.V1aKq.2bzpJAmb//XuhzFH2g6gci3539HOKei',
    updated_at = NOW()
WHERE username = 'admin';

-- 顯示更新結果
SELECT 
    username,
    email,
    display_name,
    is_enabled,
    is_account_non_locked,
    updated_at
FROM users
WHERE username = 'admin';

