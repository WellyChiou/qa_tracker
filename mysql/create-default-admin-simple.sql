-- 建立個人系統預設管理員帳號（簡化版）
-- 預設帳號：admin
-- 預設密碼：admin123
-- 
-- 此腳本使用預先生成的 BCrypt hash（與教會系統相同）
-- 密碼 "admin123" 的 BCrypt hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

USE qa_tracker;

-- 檢查並建立管理員帳號（如果不存在）
INSERT INTO users (
    uid,
    username,
    email,
    password,
    display_name,
    provider_id,
    is_enabled,
    is_account_non_locked,
    created_at,
    updated_at
)
SELECT 
    UUID() AS uid,
    'admin' AS username,
    'admin@example.com' AS email,
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' AS password, -- admin123
    '系統管理員' AS display_name,
    'local' AS provider_id,
    1 AS is_enabled,
    1 AS is_account_non_locked,
    NOW() AS created_at,
    NOW() AS updated_at
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'admin'
);

-- 為管理員分配 ADMIN 角色（如果角色存在且尚未分配）
INSERT INTO user_roles (user_uid, role_id, created_at)
SELECT 
    u.uid,
    r.id,
    NOW()
FROM users u
CROSS JOIN roles r
WHERE u.username = 'admin' 
  AND r.role_name = 'ROLE_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur 
    WHERE ur.user_uid = u.uid AND ur.role_id = r.id
  );

-- 顯示建立結果
SELECT 
    u.uid,
    u.username,
    u.email,
    u.display_name,
    u.is_enabled,
    u.is_account_non_locked,
    GROUP_CONCAT(r.role_name) AS roles
FROM users u
LEFT JOIN user_roles ur ON u.uid = ur.user_uid
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.username = 'admin'
GROUP BY u.uid, u.username, u.email, u.display_name, u.is_enabled, u.is_account_non_locked;

-- 使用說明：
-- 1. 預設帳號：admin
-- 2. 預設密碼：admin123
-- 3. 請在首次登入後立即修改密碼
-- 4. 此腳本可以安全地重複執行（使用 IF NOT EXISTS 和 NOT EXISTS 檢查）

