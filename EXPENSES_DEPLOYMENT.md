# Expenses 系統部署指南

## 快速部署步驟

### 1. 重新構建後端（包含新的 Expenses API）

```bash
# SSH 到虛擬主機
ssh root@38.54.89.136

# 進入專案目錄
cd /root/project/work/docker-vue-java-mysql

# 重新構建後端（會自動建立新的資料表）
docker compose up -d --build backend

# 查看後端日誌，確認啟動成功
docker compose logs -f backend
```

### 2. 驗證資料表是否建立

```bash
# 進入 MySQL 容器
docker exec -it mysql_db mysql -u appuser -papppassword qa_tracker

# 檢查資料表
SHOW TABLES;

# 應該會看到以下資料表：
# - users
# - records
# - config
# - expenses      ← 新增
# - assets        ← 新增
# - exchange_rates ← 新增
```

### 3. 執行資料遷移

#### 方法一：使用本地 HTTP 伺服器（推薦）

```bash
# 在本地電腦執行
cd /Users/wellychiou/my-github/fb-issue-record
python3 -m http.server 8000
```

然後在瀏覽器中打開：
- `http://localhost:8000/expenses.html`

#### 方法二：直接打開檔案

如果原始 `expenses.html` 在本地，可以直接用瀏覽器打開檔案。

### 4. 匯入資料

1. 在 `expenses.html` 頁面中，點擊「🔄 匯入到新系統」按鈕
2. 輸入新系統 API 地址：`http://38.54.89.136:8080`
3. 選擇要匯入的資料類型：
   - ✅ 匯入記帳記錄 (expenses)
   - ✅ 匯入資產組合 (assets)
   - ✅ 匯入匯率資料 (exchange_rates)
4. 建議勾選「檢查重複的記錄」
5. 點擊「開始匯入」
6. 等待匯入完成，查看結果

### 5. 驗證資料遷移結果

```bash
# 進入 MySQL 容器
docker exec -it mysql_db mysql -u appuser -papppassword qa_tracker

# 檢查記帳記錄數量
SELECT COUNT(*) as '記帳記錄數' FROM expenses;

# 檢查資產記錄數量
SELECT COUNT(*) as '資產記錄數' FROM assets;

# 檢查匯率記錄數量
SELECT COUNT(*) as '匯率記錄數' FROM exchange_rates;

# 查看前 5 筆記帳記錄
SELECT * FROM expenses ORDER BY created_at DESC LIMIT 5;

# 查看前 5 筆資產記錄
SELECT * FROM assets ORDER BY created_at DESC LIMIT 5;
```

## 測試 API

### 測試記帳記錄 API

```bash
# 取得記帳記錄（分頁）
curl http://38.54.89.136:8080/api/expenses?page=0&size=10

# 取得所有記帳記錄
curl http://38.54.89.136:8080/api/expenses/all

# 根據年份和月份篩選
curl "http://38.54.89.136:8080/api/expenses?year=2025&month=1"

# 根據類型篩選
curl "http://38.54.89.136:8080/api/expenses?type=支出"
```

### 測試資產 API

```bash
# 取得所有資產
curl http://38.54.89.136:8080/api/assets

# 取得單筆資產
curl http://38.54.89.136:8080/api/assets/1
```

### 測試匯率 API

```bash
# 取得指定日期的匯率
curl http://38.54.89.136:8080/api/exchange-rates/2025-01-01

# 取得指定日期或之前的最新匯率
curl http://38.54.89.136:8080/api/exchange-rates/latest/2025-01-01
```

## 常見問題

### Q: 匯入時出現 Mixed Content 錯誤？

**A:** 這是因為原始頁面在 HTTPS 網站上，而新系統 API 是 HTTP。解決方案：
1. 使用本地 HTTP 伺服器執行遷移（推薦）
2. 在新系統設置 HTTPS

### Q: 匯入失敗，顯示「無法連接到新系統」？

**A:** 檢查：
1. 後端服務是否正常運行：`docker compose ps`
2. 防火牆是否開放 8080 端口：`sudo ufw status`
3. API 地址是否正確：`http://38.54.89.136:8080`

### Q: 資料表沒有建立？

**A:** 檢查：
1. `mysql/schema.sql` 是否包含新的資料表定義
2. MySQL 容器是否正常運行：`docker compose ps mysql`
3. 查看 MySQL 日誌：`docker compose logs mysql`

### Q: 匯入時出現外鍵約束錯誤？

**A:** 這是因為 `created_by_uid` 或 `updated_by_uid` 對應的用戶不存在。系統會自動將這些欄位設為 `null`，但如果仍有錯誤，可以：
1. 先匯入用戶資料（如果有的話）
2. 或手動修改資料，將這些欄位設為 `null`

## 下一步

目前已完成：
- ✅ 資料庫結構
- ✅ 後端 API
- ✅ 資料遷移功能

待完成（可選）：
- ⏳ 建立新的前端 `expenses.html` 頁面（連接新系統 API）
- ⏳ 添加圖表分析功能
- ⏳ 添加資產組合管理功能

## 相關文件

- `EXPENSES_MIGRATION.md` - 詳細的遷移指南
- `SIMPLE_MIGRATION.md` - tracker.html 的遷移指南（可參考）
- `MIXED_CONTENT_FIX.md` - Mixed Content 問題解決方案

