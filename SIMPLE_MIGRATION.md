# 🚀 簡單遷移指南（無需安裝工具）

## ✅ 已完成的改進

我已經在原系統的 `tracker.html` 中添加了「匯入到新系統」功能，**您不需要安裝任何工具**！

---

## 📋 使用步驟

### 步驟 1: 確保新系統已部署

在虛擬主機上確認新系統已運行：

```bash
# SSH 到虛擬主機
ssh root@38.54.89.136

# 檢查服務狀態
cd /root/project/work/docker-vue-java-mysql
docker compose ps

# 應該看到三個容器都是 Up 狀態
```

### 步驟 2: 在原系統打開 tracker.html

1. 打開您原本的 Firebase 系統
2. 登入後進入 `tracker.html`
3. 在右上角會看到新的按鈕：**「匯入到新系統」**

### 步驟 3: 執行匯入

1. 點擊「匯入到新系統」按鈕
2. 輸入新系統的 API 地址：
   ```
   http://38.54.89.136:8080
   ```
   （系統會自動加上 `/api`，所以不需要輸入完整路徑）

3. 勾選「檢查重複」（建議勾選，會自動略過已存在的記錄）

4. 點擊「開始匯入」

5. 等待匯入完成（會顯示進度條和狀態）

### 步驟 4: 查看結果

匯入完成後會顯示：
- ✅ 成功匯入的筆數
- ⊘ 略過的筆數（已存在）
- ❌ 失敗的筆數（如果有）

---

## 🎯 功能特點

### ✅ 自動化處理
- 自動讀取所有 Firebase 記錄
- 自動轉換資料格式
- 自動處理時間戳記轉換

### ✅ 智能檢查
- 可選的重複檢查（依 `issue_number`）
- 已存在的記錄會自動略過，不會重複匯入

### ✅ 進度顯示
- 即時顯示匯入進度
- 顯示當前正在匯入的 Issue 編號
- 完成後顯示詳細統計

### ✅ 錯誤處理
- 自動重試機制
- 詳細的錯誤訊息
- 失敗的記錄會記錄在控制台

---

## 🔧 技術細節

### 資料轉換

系統會自動將 Firebase 格式轉換為 MySQL 格式：

| Firebase | MySQL |
|----------|-------|
| `issue_number` | `issueNumber` |
| `test_plan` (字串) | `testPlan` (字串) |
| `bug_found` (數字) | `bugFound` (數字) |
| `created_at` (Timestamp) | `createdAt` (自動) |
| `test_start_date` (Timestamp) | `testStartDate` (日期字串) |

### API 端點

系統會呼叫以下 API：
- `GET /api/records/stats/in-progress` - 測試連線
- `GET /api/records?issueNumber=XXX` - 檢查重複
- `POST /api/records` - 匯入記錄

---

## ⚠️ 注意事項

### 1. 網路連線
- 確保原系統可以訪問虛擬主機的 IP（38.54.89.136）
- 如果無法訪問，可能是防火牆或 CORS 問題

### 2. 資料完整性
- 建議先匯入少量資料測試
- 確認無誤後再匯入全部資料

### 3. 重複匯入
- 如果勾選「檢查重複」，已存在的記錄會被略過
- 如果不勾選，可能會建立重複記錄

### 4. 大量資料
- 如果資料量很大（>1000 筆），匯入可能需要一些時間
- 系統會每 10 筆稍作延遲，避免 API 過載

---

## 🐛 疑難排解

### 問題 1: 無法連接到新系統

**錯誤訊息：** "無法連接到新系統，請檢查 API 地址"

**解決方案：**
1. 確認新系統已啟動：`docker compose ps`
2. 確認 API 地址正確：`http://38.54.89.136:8080`
3. 檢查防火牆是否開放 8080 端口
4. 在瀏覽器直接訪問：`http://38.54.89.136:8080/api/records/stats/in-progress`

### 問題 2: CORS 錯誤

**錯誤訊息：** "CORS policy" 相關錯誤

**解決方案：**
- 後端已配置 CORS，應該不會有此問題
- 如果仍有問題，檢查後端日誌：`docker compose logs backend`

### 問題 3: 匯入失敗

**錯誤訊息：** 某些記錄匯入失敗

**解決方案：**
1. 查看瀏覽器控制台的錯誤訊息
2. 檢查後端日誌：`docker compose logs backend`
3. 確認資料格式是否正確

---

## 📊 匯入後驗證

### 檢查匯入結果

```bash
# SSH 到虛擬主機
ssh root@38.54.89.136

# 檢查記錄數
docker compose exec mysql mysql -u appuser -papppassword qa_tracker -e "SELECT COUNT(*) as total FROM records;"

# 查看一些記錄
docker compose exec mysql mysql -u appuser -papppassword qa_tracker -e "SELECT issue_number, feature, status FROM records LIMIT 10;"
```

### 在前端查看

訪問新系統的前端：
```
http://38.54.89.136/tracker.html
```

應該會看到所有匯入的記錄。

---

## 🎉 完成！

匯入完成後，您就可以：
1. ✅ 在新系統使用所有功能
2. ✅ 資料已完整遷移
3. ✅ 不需要再使用 Firebase

---

## 💡 提示

- **建議保留原系統一段時間**，確認新系統運作正常後再停用
- **定期備份新系統的資料庫**（參考 `DATABASE_DATA_PROTECTION.md`）
- 如果匯入過程中斷，可以重新執行，已存在的記錄會被略過

祝遷移順利！🚀

