# 教會簽到系統驗證指南

## 一、資料庫初始化

### 1.1 執行 Schema 腳本

```bash
# 確保 church 資料庫已建立並包含簽到系統的資料表
mysql -uroot -p church < mysql/church-schema.sql
```

### 1.2 添加 member_no 欄位到 persons 表

```bash
# 為 persons 表添加 member_no 欄位（用於簽到系統）
mysql -uroot -p church < mysql/add-member-no-to-persons.sql
```

**重要說明：**
- 簽到系統現在使用 `persons` 表，而不是獨立的 `members` 表
- `persons` 表需要 `member_no` 欄位來支援簽到功能
- 如果 `persons` 表中已有資料，需要手動為每筆記錄添加 `member_no`

### 1.3 配置 URL 權限

```bash
# 執行 URL 權限配置（必須執行，否則 API 無法正常運作）
mysql -uroot -p church < mysql/add-checkin-url-permissions.sql
```

### 1.4 準備測試資料

```sql
USE church;

-- 1. 為 persons 表添加測試人員（包含 member_no）
INSERT INTO persons (person_name, display_name, member_no, is_active) VALUES
('張三', '張三', 'A001', 1),
('李四', '李四', 'A002', 1),
('王五', '王五', 'A003', 1)
ON DUPLICATE KEY UPDATE 
  person_name=VALUES(person_name),
  display_name=VALUES(display_name),
  member_no=VALUES(member_no);

-- 如果 persons 表中已有資料但沒有 member_no，可以這樣更新：
-- UPDATE persons SET member_no = 'A001' WHERE person_name = '張三';
-- UPDATE persons SET member_no = 'A002' WHERE person_name = '李四';
-- UPDATE persons SET member_no = 'A003' WHERE person_name = '王五';

-- 2. 建立今天的場次（範例：2024-12-21 主日）
INSERT INTO sessions (session_type, title, session_date, session_code, open_at, close_at, status) VALUES
('SUNDAY', '主日崇拜', '2024-12-21', 'SUN20241221', 
 '2024-12-21 08:30:00', '2024-12-21 10:00:00', 'ACTIVE')
ON DUPLICATE KEY UPDATE title=VALUES(title);

-- 3. 確認資料
SELECT id, person_name, display_name, member_no, is_active FROM persons WHERE member_no IS NOT NULL;
SELECT * FROM sessions WHERE session_date = CURDATE();
```

## 二、後端 API 測試

### ⚠️ 重要：API 訪問方式

**通過 Nginx 反向代理（標準配置）：**
- 使用端口 `80`（默認端口，可省略）：`http://localhost/api/...`
- 或明確指定：`http://localhost:80/api/...`
- nginx 會將 `/api` 路徑轉發到後端容器

**直接訪問後端（僅用於調試）：**
- 如果需要直接訪問後端容器，需要先暴露端口：
  - 在 `docker-compose.yml` 中取消註釋 `ports: - "8080:8080"`
  - 然後使用：`http://localhost:8080/api/...`

**如果遇到 301 重定向錯誤：**
- 確認使用正確的 URL：`http://localhost/api/...`（不要加端口號，或明確使用 `:80`）
- 檢查 nginx 容器是否正常運行：`docker ps | grep nginx`
- 檢查 nginx 配置是否正確轉發 `/api` 路徑

### 2.1 測試公開 API（無需認證）

#### 測試 1：取得短效 Token

```bash
# 通過 nginx 反向代理（標準配置，端口 80）
curl -X GET "http://localhost/api/church/checkin/public/sessions/SUN20241221/token"

# 預期回應：
# {"token":"ABC123","expiresAt":"2024-12-21T09:00:00"}
```

#### 測試 2：會眾自助簽到

```bash
# 先取得 token
TOKEN=$(curl -s -X GET "http://localhost/api/church/checkin/public/sessions/SUN20241221/token" | jq -r '.token')

# 執行簽到
curl -X POST "http://localhost/api/church/checkin/public/sessions/SUN20241221/checkin" \
  -H "Content-Type: application/json" \
  -d "{\"memberNo\":\"A001\",\"token\":\"$TOKEN\"}"

# 預期回應：
# {"status":"OK","name":"張三"}
```

#### 測試 3：重複簽到（應返回錯誤）

```bash
curl -X POST "http://localhost/api/church/checkin/public/sessions/SUN20241221/checkin" \
  -H "Content-Type: application/json" \
  -d "{\"memberNo\":\"A001\",\"token\":\"$TOKEN\"}"

# 預期回應：
# {"code":"ALREADY_CHECKED_IN","message":"Already checked in."}
```

### 2.2 測試管理 API（需要 JWT 認證）

#### 步驟 1：登入取得 JWT Token

```bash
# 登入教會後台（需要先有教會用戶帳號）
curl -X POST "http://localhost/api/church/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"your_password\"}"

# 預期回應：
# {"accessToken":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...","refreshToken":"..."}
```

#### 步驟 2：使用 Token 測試管理 API

```bash
# 設定 Token（從登入回應中取得）
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# 測試 1：取得今日場次
curl -X GET "http://localhost/api/church/checkin/admin/sessions/today" \
  -H "Authorization: Bearer $TOKEN"

# 測試 2：取得場次統計
curl -X GET "http://localhost/api/church/checkin/admin/sessions/1/stats" \
  -H "Authorization: Bearer $TOKEN"

# 測試 3：取得場次簽到名單
curl -X GET "http://localhost/api/church/checkin/admin/sessions/1/checkins" \
  -H "Authorization: Bearer $TOKEN"

# 測試 4：新增補登
curl -X POST "http://localhost/api/church/checkin/admin/manual-checkins" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"sessionId\":1,\"memberNo\":\"A002\",\"note\":\"遲到補登\"}"

# 測試 5：取得補登稽核列表
curl -X GET "http://localhost/api/church/checkin/admin/manual-checkins" \
  -H "Authorization: Bearer $TOKEN"

# 測試 6：取消補登
curl -X PATCH "http://localhost/api/church/checkin/admin/manual-checkins/2/cancel" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"note\":\"輸入錯誤\"}"
```

## 三、前端功能測試

### 3.1 會眾簽到頁面測試

1. **開啟簽到頁面**
   - 訪問：`http://localhost:5175/church-admin/SUN20241221`
   - 或生產環境：`https://your-domain.com/church-admin/SUN20241221`

2. **測試簽到流程**
   - 輸入會員編號：`A001`
   - 點擊「簽到」按鈕
   - 應該顯示：「張○三，簽到完成 ✅」

3. **測試錯誤情況**
   - 輸入不存在的會員編號：應顯示「查無此會員編號 ❌」
   - 重複簽到：應顯示「已簽到完成，請勿重複簽到 ✅」（使用成功樣式，但提示不要重複）
   - 在簽到時間外：應顯示「目前不在簽到時間內 ⛔」

### 3.2 同工後台測試

1. **登入後台**
   - 訪問：`http://localhost:5175/church-admin/login`
   - 使用教會管理員帳號登入

2. **訪問同工後台**
   - 訪問：`http://localhost:5175/church-admin/checkin/admin`
   - 應該看到今日場次列表

3. **測試功能**
   - **查看簽到名單**：應該顯示已簽到的會員
   - **顯示 QR Code**：
     - 點擊「顯示 QR Code」按鈕
     - 應該彈出 QR Code 視窗，顯示該場次的簽到頁面 QR Code
     - QR Code 內容為：`http://localhost/church-admin/{sessionCode}`
     - 可以掃描此 QR Code 進入簽到頁面
   - **補登功能**：
     - 輸入會員編號：`A003`
     - 輸入備註：`遲到`
     - 點擊「補登」
     - 應該顯示「補登成功 ✅」
   - **匯出 CSV**：點擊「匯出 CSV」按鈕，應該下載 CSV 檔案
   - **自動刷新**：等待 5 秒，簽到名單應該自動更新

### 3.3 補登稽核頁面測試

1. **訪問補登稽核頁**
   - 訪問：`http://localhost:5175/church-admin/checkin/admin/manual`
   - 或從同工後台點擊「補登紀錄」按鈕

2. **測試搜尋功能**
   - 搜尋會員編號：`A002`
   - 搜尋姓名：`李四`
   - 搜尋場次：`主日`

3. **測試日期篩選**
   - 選擇開始日期和結束日期
   - 點擊「搜尋」

4. **測試取消補登**
   - 找到一個未取消的補登記錄
   - 點擊「取消」按鈕
   - 確認取消原因
   - 應該顯示為「已取消」狀態

5. **測試匯出 CSV**
   - 點擊「匯出 CSV」按鈕
   - 應該下載包含所有補登記錄的 CSV 檔案

## 四、JWT 認證驗證

### 4.1 驗證 CurrentUser 功能

1. **執行補登操作**
   ```sql
   -- 檢查補登記錄中的 manual_by 欄位
   SELECT id, member_no, manual_by, manual_note, checked_in_at 
   FROM checkins 
   WHERE manual = true 
   ORDER BY checked_in_at DESC 
   LIMIT 5;
   ```

2. **執行取消補登操作**
   ```sql
   -- 檢查取消記錄中的 canceled_by 欄位
   SELECT id, member_no, canceled_by, canceled_note, canceled_at 
   FROM checkins 
   WHERE canceled = true 
   ORDER BY canceled_at DESC 
   LIMIT 5;
   ```

3. **驗證結果**
   - `manual_by` 和 `canceled_by` 應該顯示登入的用戶名（不是 "system"）
   - 如果顯示 "system"，表示 JWT 認證可能有問題

## 五、URL 權限驗證

### 5.1 驗證公開 API 無需認證

```bash
# 不帶 Token 應該可以訪問（通過 nginx，端口 80）
curl -X GET "http://localhost/api/church/checkin/public/sessions/SUN20241221/token"

# 應該返回 200 OK，不是 401 Unauthorized
# 如果返回 301，請確認使用正確的 URL：http://localhost/api/...（不要加端口號）
```

### 5.2 驗證管理 API 需要認證

```bash
# 不帶 Token 應該被拒絕
curl -X GET "http://localhost/api/church/checkin/admin/sessions/today"

# 應該返回 401 Unauthorized 或 403 Forbidden
```

### 5.3 檢查 URL 權限配置

```sql
USE church;

-- 檢查 URL 權限是否正確配置
SELECT url_pattern, http_method, is_public, required_role, is_active, description
FROM url_permissions
WHERE url_pattern LIKE '/api/church/checkin/%'
ORDER BY order_index;
```

## 六、資料庫驗證

### 6.1 檢查簽到記錄

```sql
USE church;

-- 查看所有簽到記錄（使用 persons 表）
SELECT 
    c.id,
    s.session_code,
    p.member_no,
    COALESCE(p.display_name, p.person_name) as name,
    c.checked_in_at,
    c.manual,
    c.manual_by,
    c.canceled,
    c.canceled_by,
    c.ip
FROM checkins c
JOIN sessions s ON s.id = c.session_id
JOIN persons p ON p.id = c.member_id
ORDER BY c.checked_in_at DESC;
```

**注意：** 簽到系統現在使用 `persons` 表，`checkins.member_id` 指向 `persons.id`。

### 6.2 檢查 Token 記錄

```sql
-- 查看 session_tokens（應該只保留最新的 5 筆）
SELECT * FROM session_tokens 
WHERE session_id = 1 
ORDER BY expires_at DESC;
```

## 七、常見問題排查

### 7.0 API 返回 301 重定向錯誤

**問題現象：**
```bash
curl -X GET "http://localhost/api/church/checkin/public/sessions/SUN20241221/token"
# 返回：<html><head><title>301 Moved Permanently</title></head>...
```

**可能原因：**
1. 缺少端口號：直接訪問 `http://localhost` 時，nginx 可能重定向
2. nginx 配置問題：反向代理未正確配置

**解決方法：**

**方法 1：通過 nginx 訪問（標準方式）**
```bash
# 使用端口 80（默認端口，可省略）
curl -X GET "http://localhost/api/church/checkin/public/sessions/SUN20241221/token"
```

**方法 2：檢查服務是否運行**
```bash
# 檢查 nginx 容器是否運行
docker ps | grep nginx

# 檢查後端容器是否運行
docker ps | grep backend

# 檢查 nginx 日誌
docker logs nginx_proxy

# 檢查後端日誌
docker logs java_backend

# 測試 nginx 轉發
curl http://localhost/api/hello
```

**方法 3：如果使用 nginx，檢查配置**
```bash
# 確認 nginx 配置包含以下內容：
# location /api {
#     proxy_pass http://backend:8080;
#     proxy_set_header Host $host;
#     proxy_set_header X-Real-IP $remote_addr;
# }
```

### 7.1 API 返回 401/403 錯誤

**可能原因：**
- URL 權限未配置
- JWT Token 無效或過期
- Token 未正確傳遞

**解決方法：**
```bash
# 1. 確認 URL 權限已配置
mysql -uroot -p church -e "SELECT * FROM url_permissions WHERE url_pattern LIKE '/api/church/checkin/%';"

# 2. 重新登入取得新 Token
# 3. 確認請求 Header 包含：Authorization: Bearer <token>
```

### 7.2 補登時 manual_by 顯示 "system"

**可能原因：**
- JWT 認證未正確設置 SecurityContext
- CurrentUser 無法取得認證信息

**解決方法：**
```java
// 在 Controller 中檢查認證狀態
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
System.out.println("Current user: " + (auth != null ? auth.getName() : "null"));
```

### 7.3 前端無法訪問 API

**可能原因：**
- CORS 配置問題
- API 路徑錯誤
- 前端 base URL 配置錯誤

**解決方法：**
```javascript
// 檢查前端 api.js 中的 getApiBaseUrl()
console.log(getApiBaseUrl()); // 應該輸出 /api 或 http://localhost/api
```

### 7.4 簽到時間窗口檢查失敗

**可能原因：**
- sessions 表的 open_at/close_at 未正確設置
- 時區問題

**解決方法：**
```sql
-- 檢查場次的時間窗口
SELECT id, session_code, open_at, close_at, NOW() as current_time
FROM sessions
WHERE id = 1;
```

## 八、完整測試流程建議

1. ✅ **資料庫初始化**：執行 schema 和 URL 權限 SQL
2. ✅ **準備測試資料**：建立會員和場次
3. ✅ **測試公開 API**：會眾簽到功能
4. ✅ **測試管理 API**：同工後台功能
5. ✅ **測試前端頁面**：三個主要頁面
6. ✅ **驗證 JWT 認證**：確認操作人記錄正確
7. ✅ **驗證 URL 權限**：確認權限控制正常
8. ✅ **測試錯誤處理**：各種錯誤情況
9. ✅ **測試 CSV 匯出**：確認匯出功能正常
10. ✅ **測試自動刷新**：同工後台自動更新

## 九、測試工具推薦

- **Postman**：用於 API 測試
- **curl**：命令行測試工具
- **瀏覽器開發者工具**：檢查網路請求和回應
- **MySQL Workbench**：查看資料庫記錄

完成以上測試後，簽到系統應該可以正常運作！

