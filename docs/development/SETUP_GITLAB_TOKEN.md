# 🔑 GitLab Token 設定指南

## ❓ 為什麼會出現「請輸入 GitLab Token」？

這是因為：
1. **後端 API 已建立**：現在可以從資料庫讀取 Token
2. **資料庫中的 Token 為空**：`config` 表中的 `gitlab_token` 值為空字串
3. **需要先設定 Token**：第一次使用需要輸入並儲存

---

## ✅ 解決方案

### 方法 1: 在資料庫中直接設定（推薦）

```sql
-- 使用 MySQL 工具連線後執行
UPDATE config 
SET config_value = '您的GitLabToken' 
WHERE config_key = 'gitlab_token';
```

### 方法 2: 透過前端設定

1. 點擊「GitLab Issues」按鈕
2. 當提示輸入 Token 時，輸入您的 GitLab Token
3. 系統會自動儲存到資料庫

### 方法 3: 透過 API 設定

```bash
# 使用 curl 設定
curl -X POST http://38.54.89.136:8080/api/config/gitlab_token \
  -H "Content-Type: application/json" \
  -d '{"value": "您的GitLabToken", "description": "GitLab API Token"}'
```

---

## 🔧 後端 API

### 讀取 Token

```
GET /api/config/gitlab_token
```

回應：
```json
{
  "value": "your-token-here"
}
```

### 儲存 Token

```
POST /api/config/gitlab_token
Content-Type: application/json

{
  "value": "your-token-here",
  "description": "GitLab API Token"
}
```

---

## 📝 設定步驟

### 步驟 1: 取得 GitLab Token

1. 登入 GitLab
2. 前往「Settings」→「Access Tokens」
3. 建立新的 Token，權限選擇 `read_api`
4. 複製 Token

### 步驟 2: 設定到資料庫

**使用 MySQL 工具：**

```sql
-- 連線到資料庫
USE qa_tracker;

-- 更新 Token
UPDATE config 
SET config_value = '您的GitLabToken' 
WHERE config_key = 'gitlab_token';

-- 驗證
SELECT config_key, LEFT(config_value, 10) as token_preview, description 
FROM config 
WHERE config_key = 'gitlab_token';
```

**或使用命令列：**

```bash
# SSH 到虛擬主機
ssh root@38.54.89.136

# 更新 Token
docker compose exec mysql mysql -u appuser -papppassword qa_tracker -e "
UPDATE config 
SET config_value = '您的GitLabToken' 
WHERE config_key = 'gitlab_token';
"
```

### 步驟 3: 重新啟動個人系統後端（如果需要）

```bash
docker compose restart backend-personal
```

---

## ✅ 驗證設定

### 方法 1: 透過 API 測試

```bash
curl http://38.54.89.136:8080/api/config/gitlab_token
```

應該會看到：
```json
{
  "value": "您的Token（會完整顯示）"
}
```

### 方法 2: 在前端測試

1. 點擊「GitLab Issues」按鈕
2. 輸入 Assignee 和 Project
3. 點擊「查詢 Issues」
4. 如果 Token 正確，應該會顯示 Issues 列表
5. 如果 Token 錯誤或為空，會提示輸入

---

## 🔒 安全性提醒

### ⚠️ 注意事項

1. **Token 權限**：建議只給予 `read_api` 權限
2. **Token 保護**：不要將 Token 提交到 Git
3. **定期更換**：建議定期更換 Token

### 💡 最佳實踐

- 使用環境變數或加密儲存 Token
- 限制 Token 的權限範圍
- 定期審查 Token 使用情況

---

## 🎯 完成

設定完成後，下次使用 GitLab Issues 功能時就不會再提示輸入 Token 了！

如果還有問題，請檢查：
1. 資料庫中的 Token 是否正確設定
2. 後端 API 是否正常運行
3. Token 是否有正確的權限
