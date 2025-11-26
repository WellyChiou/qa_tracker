# Spring Security 登入系統與權限管理實作說明

## 📋 已完成的功能

### 1. **後端實作**

#### ✅ 依賴管理
- 已添加 `spring-boot-starter-security` 到 `pom.xml`
- 已添加 JWT 支援（可選，用於未來擴展）

#### ✅ 數據庫結構
- **實體類**：
  - `User` - 擴展用戶實體，添加 `username`, `password`, `isEnabled`, `isAccountNonLocked` 欄位
  - `Role` - 角色實體（支援多對多權限關聯）
  - `Permission` - 權限實體
  - `MenuItem` - 菜單項實體（支援層級結構）
  
- **關聯表**：
  - `user_roles` - 用戶角色關聯
  - `role_permissions` - 角色權限關聯
  - `user_permissions` - 用戶直接權限關聯（可選）

- **Migration 腳本**：
  - `mysql/add-security-tables-simple.sql` - 創建所有必要的表和預設數據

#### ✅ Spring Security 配置
- `SecurityConfig` - 安全配置類
  - 支援表單登入
  - 支援會話管理
  - 配置 URL 權限規則
  - 配置 CORS

#### ✅ 服務層
- `CustomUserDetailsService` - 自定義用戶認證服務
- `MenuService` - 菜單服務（根據用戶權限過濾菜單）

#### ✅ Controller 層
- `AuthController` - 認證相關 API
  - `/api/auth/login` - 登入
  - `/api/auth/logout` - 登出
  - `/api/auth/current-user` - 獲取當前用戶資訊
  - `/api/auth/register` - 註冊（可選）

- `MenuController` - 菜單 API
  - `/api/menus` - 獲取用戶可見菜單

### 2. **前端實作**

#### ✅ 登入頁面
- `login.html` - 美觀的登入頁面
  - 用戶名/密碼輸入
  - 錯誤訊息顯示
  - 登入狀態提示

#### ✅ 儀表板（保留狀態資訊）
- `index.html` - 更新後的儀表板
  - **保留原有功能**：顯示前端、後端、資料庫狀態
  - 添加導航欄和菜單系統
  - 顯示用戶資訊和登出按鈕
  - 根據用戶權限動態顯示菜單

#### ✅ API 服務
- `api.js` - 添加認證相關的 API 方法

### 3. **權限系統設計**

#### 預設角色
- `ROLE_ADMIN` - 系統管理員（擁有所有權限）
- `ROLE_USER` - 一般使用者（擁有讀取權限）
- `ROLE_VIEWER` - 唯讀使用者（擁有讀取權限）

#### 預設權限
- `EXPENSES_READ` - 查看記帳系統
- `EXPENSES_WRITE` - 編輯記帳系統
- `TRACKER_READ` - 查看 QA Tracker
- `TRACKER_WRITE` - 編輯 QA Tracker
- `ASSETS_READ` - 查看資產
- `ASSETS_WRITE` - 編輯資產
- `ADMIN_ACCESS` - 管理員存取

#### 預設菜單
- 儀表板（無需權限）
- 家庭記帳（需要 `EXPENSES_READ`）
- QA Tracker（需要 `TRACKER_READ`）
- 系統管理（需要 `ADMIN_ACCESS`）
  - 用戶管理
  - 角色管理
  - 權限管理
  - 菜單管理

## 🚀 使用步驟

### 1. 執行數據庫 Migration

執行以下 SQL 腳本：
```bash
mysql -u your_username -p qa_tracker < mysql/add-security-tables-simple.sql
```

或者使用安全版本（如果欄位已存在會自動跳過）：
```bash
mysql -u your_username -p qa_tracker < mysql/add-security-tables.sql
```

### 2. 創建預設管理員帳號

執行以下 SQL 腳本創建預設管理員帳號：
```bash
mysql -u your_username -p qa_tracker < mysql/create-default-admin.sql
```

**預設登入資訊**：
- **用戶名**：`admin`
- **密碼**：`admin123`

⚠️ **重要**：首次登入後請立即修改密碼！

#### 手動創建管理員帳號（可選）

如果您想使用自定義密碼，可以手動創建：
```sql
-- 創建用戶（UID 可以自定義）
INSERT INTO users (uid, username, email, password, display_name, provider_id, is_enabled, is_account_non_locked)
VALUES (
    UUID(),
    'admin',
    'admin@example.com',
    '$2a$10$YourEncodedPasswordHere', -- 使用 BCrypt 加密的密碼
    '系統管理員',
    'local',
    1,
    1
);

-- 分配管理員角色
INSERT INTO user_roles (user_uid, role_id)
SELECT u.uid, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.role_name = 'ROLE_ADMIN';
```

**生成 BCrypt 密碼**：可以使用以下 Java 代碼生成：
```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String encodedPassword = encoder.encode("your_password");
System.out.println(encodedPassword);
```

### 3. 配置後端

確保 `SecurityConfig` 配置正確，特別是：
- 靜態資源訪問權限
- 登入頁面路徑
- API 端點權限規則

### 4. 測試登入

1. 訪問 `http://your-domain/login.html`
2. 使用預設管理員帳號登入：
   - **用戶名**：`admin`
   - **密碼**：`admin123`
3. 登入成功後會跳轉到 `index.html`（儀表板）
4. 儀表板會顯示系統狀態和可用菜單
5. ⚠️ **重要**：首次登入後請立即修改密碼！

## 📝 注意事項

1. **index.html 保留狀態資訊**：
   - 系統狀態檢查功能已保留
   - 添加了用戶認證狀態檢查
   - 添加了動態菜單顯示

2. **權限控制層級**：
   - **角色級別**：通過 `ROLE_*` 控制
   - **權限級別**：通過 `PERM_*` 控制
   - **用戶級別**：可以直接為用戶分配權限（覆蓋角色權限）

3. **菜單系統**：
   - 支援層級結構（父子菜單）
   - 根據用戶權限動態過濾
   - 支援圖標和排序

4. **後續擴展**：
   - 可以添加 JWT Token 認證
   - 可以添加角色和權限管理頁面
   - 可以添加用戶管理頁面
   - 可以在現有 Controller 中添加 `@PreAuthorize` 註解進行方法級權限控制

## 🔧 待完成的功能（可選）

1. 用戶管理頁面（CRUD）
2. 角色管理頁面（CRUD）
3. 權限管理頁面（CRUD）
4. 菜單管理頁面（CRUD）
5. 在現有 API 中添加權限檢查註解
6. 添加密碼重置功能
7. 添加記住我功能

## 🐛 故障排除

### 問題：無法訪問 index.html
- **解決**：檢查 `SecurityConfig` 中的 URL 匹配規則，確保 `index.html` 和 `/` 路徑已配置為允許訪問

### 問題：登入後無法獲取菜單
- **解決**：檢查 `MenuService` 中的權限檢查邏輯，確保用戶有對應的權限

### 問題：SQL 執行錯誤
- **解決**：如果欄位已存在，SQL 會報錯但可以忽略。建議先檢查欄位是否存在。

