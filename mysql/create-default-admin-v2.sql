-- 創建預設管理員帳號（版本 2）
-- 預設帳號：admin
-- 預設密碼：admin123
-- 
-- 注意：此密碼需要先通過 BCrypt 加密生成 hash
-- 使用方式：
-- 1. 訪問 http://your-server:8080/api/utils/hash-password?password=admin123 獲取 hash
-- 2. 將獲取到的 hash 替換下面的密碼 hash
-- 3. 執行此 SQL 腳本

USE qa_tracker;

-- 刪除舊的管理員帳號（如果存在）
DELETE FROM user_roles WHERE user_uid IN (SELECT uid FROM users WHERE username = 'admin');
DELETE FROM users WHERE username = 'admin';

-- 創建預設管理員用戶
-- 注意：請先訪問 /api/utils/hash-password?password=admin123 獲取正確的 hash 並替換下面的值
INSERT INTO users (
    uid,
    username,
    email,
    password,
    display_name,
    provider_id,
    is_enabled,
    is_account_non_locked
) VALUES (
    UUID(),
    'admin',
    'admin@example.com',
    'PLACEHOLDER_PASSWORD_HASH', -- 請替換為從 /api/utils/hash-password 獲取的 hash
    '系統管理員',
    'local',
    1,
    1
);

-- 分配管理員角色
INSERT INTO user_roles (user_uid, role_id)
SELECT u.uid, r.id
FROM users u
CROSS JOIN roles r
WHERE u.username = 'admin' 
  AND r.role_name = 'ROLE_ADMIN';

