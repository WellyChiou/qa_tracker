-- 建立教會系統預設管理員帳號
-- 執行此腳本前，請先執行 church-security-tables.sql

USE church;

-- 建立預設管理員帳號
-- 預設密碼：admin123（請在首次登入後立即修改）
-- 使用 BCrypt 加密（cost factor 10）
-- 您可以使用 backend 的 /api/utils/generate-bcrypt-hash 端點來生成新的密碼 hash

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
    'admin@church.local' AS email,
    '$2a$10$KilvCsL.TYApEME.V1aKq.2bzpJAmb//XuhzFH2g6gci3539HOKei' AS password, -- admin123
    '系統管理員' AS display_name,
    'local' AS provider_id,
    1 AS is_enabled,
    1 AS is_account_non_locked,
    NOW() AS created_at,
    NOW() AS updated_at
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'admin'
);

-- 為管理員分配 ADMIN 角色
INSERT INTO user_roles (user_uid, role_id)
SELECT 
    u.uid,
    r.id
FROM users u
CROSS JOIN roles r
WHERE u.username = 'admin' 
  AND r.role_name = 'ROLE_CHURCH_ADMIN'
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
    GROUP_CONCAT(r.role_name) AS roles
FROM users u
LEFT JOIN user_roles ur ON u.uid = ur.user_uid
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.username = 'admin'
GROUP BY u.uid, u.username, u.email, u.display_name;

-- 使用說明：
-- 1. 預設帳號：admin
-- 2. 預設密碼：admin123
-- 3. 請在首次登入後立即修改密碼
-- 4. 要生成新的密碼 hash，可以使用：
--    POST /api/utils/generate-bcrypt-hash
--    Body: { "password": "您的新密碼" }

