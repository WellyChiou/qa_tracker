# Expenses 系統轉移指南

## 概述

本指南說明如何將 `expenses.html`（家庭記帳系統）從 Firebase 轉移到新的 Docker 化系統（Vue + Java + MySQL）。

## 已完成的工作

### 1. 資料庫結構
- ✅ 建立 `expenses` 表（記帳記錄）
- ✅ 建立 `assets` 表（資產組合）
- ✅ 建立 `exchange_rates` 表（匯率資料）

### 2. 後端 API
- ✅ ExpenseController：`/api/expenses`
- ✅ AssetController：`/api/assets`
- ✅ ExchangeRateController：`/api/exchange-rates`

## 待完成的工作

### 1. 前端頁面
需要建立新的 `frontend/app/expenses.html`，連接新系統的 API。

### 2. 資料遷移
在原始 `expenses.html` 中添加「匯入到新系統」功能，類似 `tracker.html` 的遷移功能。

## 資料表結構

### expenses 表
- `id`: 主鍵
- `firebase_id`: Firebase 原始 ID（遷移用）
- `date`: 日期
- `member`: 家庭成員（爸爸、媽媽、孩子、其他）
- `type`: 類型（收入、支出）
- `main_category`: 類別（食、衣、住、行、育、樂、醫療、其他支出、薪資、投資等）
- `sub_category`: 細項
- `amount`: 金額
- `currency`: 幣別（TWD、USD、EUR、JPY、CNY）
- `exchange_rate`: 匯率
- `description`: 描述
- `created_by_uid`: 建立者 UID
- `created_at`: 建立時間
- `updated_at`: 更新時間

### assets 表
- `id`: 主鍵
- `firebase_id`: Firebase 原始 ID（遷移用）
- `stock_code`: 股票代碼
- `asset_type`: 資產類型
- `quantity`: 數量
- `cost`: 成本
- `unit_price`: 單價
- `current_price`: 當前價格
- `member`: 家庭成員
- `category`: 類別
- `order_index`: 排序順序

### exchange_rates 表
- `id`: 主鍵
- `date`: 匯率日期（唯一）
- `usd_rate`: 美元匯率
- `eur_rate`: 歐元匯率
- `jpy_rate`: 日圓匯率
- `cny_rate`: 人民幣匯率

## API 端點

### Expenses
- `GET /api/expenses` - 取得記帳記錄（分頁）
- `GET /api/expenses/all` - 取得所有記帳記錄（不分頁）
- `GET /api/expenses/{id}` - 取得單筆記錄
- `POST /api/expenses` - 建立記錄
- `PUT /api/expenses/{id}` - 更新記錄
- `DELETE /api/expenses/{id}` - 刪除記錄
- `GET /api/expenses/firebase/{firebaseId}` - 根據 Firebase ID 查找

### Assets
- `GET /api/assets` - 取得所有資產
- `GET /api/assets/{id}` - 取得單筆資產
- `POST /api/assets` - 建立資產
- `PUT /api/assets/{id}` - 更新資產
- `DELETE /api/assets/{id}` - 刪除資產
- `GET /api/assets/firebase/{firebaseId}` - 根據 Firebase ID 查找

### Exchange Rates
- `GET /api/exchange-rates/{date}` - 取得指定日期的匯率
- `GET /api/exchange-rates/latest/{date}` - 取得指定日期或之前的最新匯率
- `POST /api/exchange-rates` - 建立匯率
- `PUT /api/exchange-rates/{date}` - 更新匯率

## 已完成的工作總結

### ✅ 資料庫結構
- 已在 `mysql/schema.sql` 中添加 `expenses`、`assets`、`exchange_rates` 三個資料表

### ✅ 後端 API
- ExpenseController：完整的 CRUD API
- AssetController：完整的 CRUD API
- ExchangeRateController：匯率管理 API

### ✅ 資料遷移功能
- 已在原始 `expenses.html` 中添加「匯入到新系統」功能
- 支援匯入記帳記錄、資產組合、匯率資料
- 支援檢查重複記錄
- 顯示匯入進度和結果

## 部署步驟

### 1. 重新構建後端（包含新的 API）

```bash
# SSH 到虛擬主機
ssh root@38.54.89.136

# 進入專案目錄
cd /root/project/work/docker-vue-java-mysql

# 重新構建後端（會自動建立新的資料表）
docker compose up -d --build backend
```

### 2. 驗證資料表是否建立

```bash
# 進入 MySQL 容器
docker exec -it mysql_db mysql -u appuser -papppassword qa_tracker

# 檢查資料表
SHOW TABLES;

# 應該會看到：
# - users
# - records
# - config
# - expenses
# - assets
# - exchange_rates
```

### 3. 執行資料遷移

1. 在本地打開原始 `expenses.html`（使用 Python HTTP 伺服器，避免 Mixed Content 問題）：
   ```bash
   cd /Users/wellychiou/my-github/fb-issue-record
   python3 -m http.server 8000
   ```

2. 在瀏覽器中打開 `http://localhost:8000/expenses.html`

3. 點擊「🔄 匯入到新系統」按鈕

4. 輸入新系統 API 地址：`http://38.54.89.136:8080`

5. 選擇要匯入的資料類型（記帳記錄、資產組合、匯率資料）

6. 點擊「開始匯入」

### 4. 驗證資料遷移結果

```bash
# 進入 MySQL 容器
docker exec -it mysql_db mysql -u appuser -papppassword qa_tracker

# 檢查記帳記錄數量
SELECT COUNT(*) FROM expenses;

# 檢查資產記錄數量
SELECT COUNT(*) FROM assets;

# 檢查匯率記錄數量
SELECT COUNT(*) FROM exchange_rates;
```

## 注意事項

1. **Mixed Content 問題**：如果原始 `expenses.html` 在 HTTPS 網站上（如 GitHub Pages），需要使用本地 HTTP 伺服器執行遷移，或在新系統設置 HTTPS。

2. **資料完整性**：匯入時會檢查 `created_by_uid` 和 `updated_by_uid` 對應的用戶是否存在，如果不存在會設為 `null` 以避免外鍵約束錯誤。

3. **重複檢查**：建議勾選「檢查重複的記錄」，避免重複匯入相同資料。

4. **前端頁面**：目前還沒有建立新的前端 `expenses.html` 頁面來連接新系統 API。如果需要，可以參考 `tracker.html` 的實現方式建立。

## 測試 API

```bash
# 測試取得記帳記錄
curl http://38.54.89.136:8080/api/expenses?page=0&size=10

# 測試取得資產
curl http://38.54.89.136:8080/api/assets

# 測試取得匯率
curl http://38.54.89.136:8080/api/exchange-rates/2025-01-01
```

