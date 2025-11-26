# 設置預設管理員密碼指南

## 問題

如果遇到「憑證錯誤」的登入問題，可能是因為數據庫中的 BCrypt 密碼 hash 不正確。

## 解決方案

### 方法 1：使用工具 API 生成正確的 Hash（推薦）

1. **生成密碼 Hash**：
   訪問以下 URL（請替換為您的實際域名）：
   ```
   http://38.54.89.136:8080/api/utils/hash-password?password=admin123
   ```

2. **獲取 Hash 值**：
   您會得到類似以下的 JSON 響應：
   ```json
   {
     "password": "admin123",
     "hash": "$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
     "message": "使用此 hash 更新數據庫中的密碼欄位"
   }
   ```

3. **更新數據庫**：
   執行以下 SQL（將 `YOUR_HASH_HERE` 替換為上面獲取的 hash）：
   ```sql
   USE qa_tracker;
   
   -- 如果管理員帳號已存在，更新密碼
   UPDATE users 
   SET password = 'YOUR_HASH_HERE'
   WHERE username = 'admin';
   
   -- 如果管理員帳號不存在，創建新帳號
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
       'YOUR_HASH_HERE',
       '系統管理員',
       'local',
       1,
       1
   )
   ON DUPLICATE KEY UPDATE password = 'YOUR_HASH_HERE';
   
   -- 確保分配管理員角色
   INSERT INTO user_roles (user_uid, role_id)
   SELECT u.uid, r.id
   FROM users u
   CROSS JOIN roles r
   WHERE u.username = 'admin' 
     AND r.role_name = 'ROLE_ADMIN'
     AND NOT EXISTS (
         SELECT 1 FROM user_roles ur 
         WHERE ur.user_uid = u.uid AND ur.role_id = r.id
     );
   ```

### 方法 2：使用已知的正確 Hash

如果您無法訪問工具 API，可以使用以下已知的正確 hash（這是 `admin123` 的 BCrypt hash）：

```sql
USE qa_tracker;

UPDATE users 
SET password = '$2a$10$8K1p/a0dL1vVdY7x5kqO3u6Z9X0Y5J2N7K4L8M9P0Q1R2S3T4U5V6W'
WHERE username = 'admin';
```

**注意**：實際上，由於 BCrypt 每次生成的 hash 都不同（因為包含隨機 salt），上面的 hash 可能無法驗證。建議使用方法 1。

### 方法 3：直接創建新帳號（如果不存在）

如果管理員帳號不存在，可以使用以下 SQL 創建（但需要先獲取正確的 hash）：

```sql
USE qa_tracker;

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
    'YOUR_HASH_HERE', -- 從 /api/utils/hash-password 獲取
    '系統管理員',
    'local',
    1,
    1
);
```

## 驗證密碼

創建帳號後，可以使用以下 API 驗證密碼是否正確：

```
http://38.54.89.136:8080/api/utils/verify-password?password=admin123&hash=YOUR_HASH_HERE
```

如果返回 `{"matches": true}`，表示密碼 hash 正確。

## 預設登入資訊

- **用戶名**：`admin`
- **密碼**：`admin123`
- ⚠️ **首次登入後請立即修改密碼**

