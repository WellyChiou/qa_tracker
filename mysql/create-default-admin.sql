-- 創建預設管理員帳號
-- 預設帳號：admin
-- 預設密碼：admin123
-- 
-- 注意：此密碼為 BCrypt 加密後的結果
-- 建議首次登入後立即修改密碼

USE qa_tracker;

-- 創建預設管理員用戶（如果不存在）
INSERT IGNORE INTO users (
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
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- admin123 的 BCrypt hash
    '系統管理員',
    'local',
    1,
    1
);

-- 分配管理員角色
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

