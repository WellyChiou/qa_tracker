-- 更新管理員密碼
-- 使用方式：
-- 1. 訪問 http://your-server:8080/api/utils/hash-password?password=admin123 獲取 hash
-- 2. 將獲取到的 hash 替換下面的 YOUR_HASH_HERE
-- 3. 執行此 SQL

USE qa_tracker;

-- 更新管理員密碼（將 YOUR_HASH_HERE 替換為從 API 獲取的 hash）
UPDATE users 
SET password = 'YOUR_HASH_HERE'
WHERE username = 'admin';

-- 如果管理員帳號不存在，創建新帳號（同樣需要替換 YOUR_HASH_HERE）
INSERT INTO users (
    uid,
    username,
    email,
    password,
    display_name,
    provider_id,
    is_enabled,
    is_account_non_locked
) 
SELECT 
    UUID(),
    'admin',
    'admin@example.com',
    'YOUR_HASH_HERE',
    '系統管理員',
    'local',
    1,
    1
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

-- 確保分配管理員角色
INSERT IGNORE INTO user_roles (user_uid, role_id)
SELECT u.uid, r.id
FROM users u
CROSS JOIN roles r
WHERE u.username = 'admin' 
  AND r.role_name = 'ROLE_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur 
      WHERE ur.user_uid = u.uid AND ur.role_id = r.id
  );

