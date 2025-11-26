# 📊 在新系統查看資料指南

## 🎯 訪問新系統

### 方法 1: 直接訪問（推薦）

在瀏覽器打開：

```
http://38.54.89.136/tracker.html
```

或

```
http://38.54.89.136/
```

然後點擊 `tracker.html` 連結。

---

## 🔍 查看資料的方式

### 1. 前端頁面（圖形化界面）

**訪問地址：**
```
http://38.54.89.136/tracker.html
```

**功能：**
- ✅ 查看所有記錄
- ✅ 搜尋和篩選
- ✅ 新增、編輯、刪除記錄
- ✅ 統計資訊

### 2. API 直接查詢

**查看所有記錄：**
```
http://38.54.89.136:8080/api/records
```

**查看執行中的記錄：**
```
http://38.54.89.136:8080/api/records/stats/in-progress
```

**搜尋記錄：**
```
http://38.54.89.136:8080/api/records?issueNumber=1234
http://38.54.89.136:8080/api/records?status=1
http://38.54.89.136:8080/api/records?category=1
```

### 3. 資料庫直接查詢

**使用 MySQL 工具：**
- 使用 MySQL Workbench、DBeaver 等工具連線
- 主機：`38.54.89.136`
- 端口：`3306`
- 資料庫：`qa_tracker`
- 使用者：`appuser`
- 密碼：`apppassword`

**查詢 SQL：**
```sql
-- 查看所有記錄
SELECT * FROM records ORDER BY issue_number DESC;

-- 查看執行中的記錄
SELECT * FROM records WHERE status = 1;

-- 統計記錄數
SELECT COUNT(*) as total FROM records;
SELECT status, COUNT(*) as count FROM records GROUP BY status;
```

---

## 🚀 快速開始

### 步驟 1: 確認服務運行

```bash
# SSH 到虛擬主機
ssh root@38.54.89.136

# 檢查服務狀態
cd /root/project/work/docker-vue-java-mysql
docker compose ps
```

應該看到三個容器都是 `Up` 狀態。

### 步驟 2: 在瀏覽器打開

訪問：
```
http://38.54.89.136/tracker.html
```

### 步驟 3: 查看資料

頁面應該會自動載入所有記錄。

---

## 📋 前端功能說明

### 主要功能

1. **記錄列表**
   - 顯示所有匯入的記錄
   - 可以排序和篩選

2. **搜尋功能**
   - 依 Issue Number 搜尋
   - 依狀態、類型篩選
   - 全文搜尋（功能描述、備註）

3. **統計資訊**
   - 執行中的記錄數
   - 各狀態的統計

4. **CRUD 操作**
   - 新增記錄
   - 編輯記錄
   - 刪除記錄

---

## 🔧 如果無法訪問

### 檢查前端是否運行

```bash
# 檢查容器狀態
docker compose ps frontend

# 查看前端日誌
docker compose logs frontend

# 測試前端
curl http://localhost/tracker.html
```

### 檢查防火牆

```bash
# 確認 80 端口已開放
sudo ufw status | grep 80

# 如果沒有，開放端口
sudo ufw allow 80/tcp
sudo ufw reload
```

### 檢查雲服務商安全組

確保安全組已開放 80 端口。

---

## 📊 API 端點列表

### 記錄相關

- `GET /api/records` - 取得所有記錄（支援分頁、搜尋、篩選）
- `GET /api/records/{id}` - 取得單筆記錄
- `POST /api/records` - 新增記錄
- `PUT /api/records/{id}` - 更新記錄
- `DELETE /api/records/{id}` - 刪除記錄

### 統計相關

- `GET /api/records/stats/in-progress` - 取得執行中的記錄數

### 搜尋參數

- `issueNumber` - Issue 編號
- `status` - 狀態（0=執行中止, 1=執行中, 2=完成）
- `category` - 類型（1=BUG, 2=改善, 3=優化, 4=模組, 5=QA）
- `testPlan` - Test Plan（0=否, 1=是）
- `bugFound` - 發現 BUG（0=否, 1=是）
- `keyword` - 關鍵字搜尋（功能描述、備註）
- `page` - 頁碼（從 0 開始）
- `size` - 每頁筆數

---

## 💡 使用範例

### 範例 1: 查看所有記錄

```
http://38.54.89.136:8080/api/records?page=0&size=20
```

### 範例 2: 搜尋特定 Issue

```
http://38.54.89.136:8080/api/records?issueNumber=1234
```

### 範例 3: 查看執行中的記錄

```
http://38.54.89.136:8080/api/records?status=1
```

### 範例 4: 關鍵字搜尋

```
http://38.54.89.136:8080/api/records?keyword=登入
```

---

## ✅ 驗證資料已匯入

### 方法 1: 前端頁面

訪問 `http://38.54.89.136/tracker.html`，應該會看到記錄列表。

### 方法 2: API 查詢

```bash
# 查看記錄數
curl http://38.54.89.136:8080/api/records?size=1

# 應該會看到 totalElements 顯示總筆數
```

### 方法 3: 資料庫查詢

```bash
# SSH 到虛擬主機
ssh root@38.54.89.136

# 查詢記錄數
docker compose exec mysql mysql -u appuser -papppassword qa_tracker -e "SELECT COUNT(*) as total FROM records;"
```

---

## 🎉 完成！

現在您可以在新系統查看所有匯入的資料了！

**主要訪問地址：**
- 前端頁面：`http://38.54.89.136/tracker.html`
- API：`http://38.54.89.136:8080/api/records`

祝您使用愉快！🚀

