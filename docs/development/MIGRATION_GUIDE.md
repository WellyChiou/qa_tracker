# 🚀 Firebase 到 MySQL 完整遷移指南

本指南將協助您將 QA Tracker 從 Firebase 完整遷移到 MySQL + Java + Vue 架構。

## 📋 遷移步驟總覽

1. ✅ **準備工作** - 檢查環境和備份資料
2. ✅ **資料庫設置** - 建立 MySQL 資料表
3. ✅ **資料遷移** - 從 Firebase 匯出並匯入 MySQL
4. ✅ **後端部署** - 部署 Java Spring Boot API
5. ✅ **前端部署** - 部署 Vue 前端
6. ✅ **驗證測試** - 確認所有功能正常

---

## 步驟 1: 準備工作

### 1.1 檢查虛擬主機環境

```bash
# SSH 連接到虛擬主機
ssh root@38.54.89.136

# 確認 Docker 和 Docker Compose 已安裝
docker --version
docker compose version
```

### 1.2 備份 Firebase 資料

**重要：遷移前務必備份！**

1. 前往 Firebase Console: https://console.firebase.google.com/
2. 選擇專案：`fb-issue-record`
3. 前往 Firestore Database
4. 使用 Firebase CLI 匯出：

```bash
# 安裝 Firebase CLI（在本地電腦）
npm install -g firebase-tools

# 登入 Firebase
firebase login

# 匯出 Firestore 資料
firebase firestore:export ./firebase-backup-$(date +%Y%m%d)
```

---

## 步驟 2: 上傳專案到虛擬主機

### 2.1 在本地打包專案

```bash
cd /Users/wellychiou/my-github/docker-vue-java-mysql
tar -czf qa-tracker-migration.tar.gz .
```

### 2.2 上傳到虛擬主機

```bash
# 上傳到虛擬主機
scp qa-tracker-migration.tar.gz root@38.54.89.136:/root/project/work/

# SSH 到虛擬主機
ssh root@38.54.89.136

# 解壓專案
cd /root/project/work
tar -xzf qa-tracker-migration.tar.gz
cd docker-vue-java-mysql
```

---

## 步驟 3: 建立資料庫結構

### 3.1 啟動 MySQL 容器（會自動執行 schema.sql）

```bash
# 啟動所有服務（MySQL 會自動執行 schema.sql）
docker compose up -d mysql

# 等待 MySQL 完全啟動
docker compose logs mysql

# 驗證資料庫已建立
docker compose exec mysql mysql -u appuser -papppassword -e "SHOW DATABASES;"
docker compose exec mysql mysql -u appuser -papppassword qa_tracker -e "SHOW TABLES;"
```

應該會看到：
- `qa_tracker` 資料庫
- `users` 表
- `records` 表
- `config` 表

---

## 步驟 4: 資料遷移

### 4.1 準備 Firebase 匯出資料

將 Firebase 匯出的資料轉換為以下 JSON 格式：

```json
{
  "users": {
    "user-uid-1": {
      "email": "user@example.com",
      "displayName": "User Name",
      "photoURL": "https://...",
      "providerId": "google.com",
      "lastLoginAt": { "seconds": 1234567890, "nanoseconds": 0 },
      "createdAt": { "seconds": 1234567890, "nanoseconds": 0 }
    }
  },
  "records": {
    "record-id-1": {
      "issue_number": 1234,
      "status": 1,
      "category": 1,
      "feature": "功能描述",
      "memo": "備註",
      "test_plan": "0",
      "bug_found": 0,
      "optimization_points": 0,
      "verify_failed": 0,
      "test_cases": 0,
      "file_count": 0,
      "test_start_date": { "seconds": 1234567890, "nanoseconds": 0 },
      "eta_date": { "seconds": 1234567890, "nanoseconds": 0 },
      "completed_at": null,
      "created_by_uid": "user-uid-1",
      "created_at": { "seconds": 1234567890, "nanoseconds": 0 },
      "updated_at": { "seconds": 1234567890, "nanoseconds": 0 }
    }
  },
  "config": {
    "gitlab": {
      "token": "your-gitlab-token"
    }
  }
}
```

### 4.2 執行遷移腳本

```bash
# 進入遷移目錄
cd migrate

# 安裝 Node.js 依賴（如果虛擬主機有 Node.js）
npm install mysql2

# 執行遷移
node firebase-to-mysql.js ../firebase-data.json
```

**注意**：如果虛擬主機沒有 Node.js，可以在本地執行遷移腳本，但需要確保能連接到虛擬主機的 MySQL。

### 4.3 驗證遷移結果

```bash
# 檢查記錄數
docker compose exec mysql mysql -u appuser -papppassword qa_tracker -e "SELECT COUNT(*) as total FROM records;"
docker compose exec mysql mysql -u appuser -papppassword qa_tracker -e "SELECT COUNT(*) as total FROM users;"

# 檢查一些記錄
docker compose exec mysql mysql -u appuser -papppassword qa_tracker -e "SELECT * FROM records LIMIT 5;"
```

---

## 步驟 5: 啟動所有服務

### 5.1 啟動後端和前端

```bash
# 回到專案根目錄
cd /root/project/work/docker-vue-java-mysql

# 啟動所有服務
docker compose up -d --build

# 檢查服務狀態
docker compose ps

# 查看日誌
docker compose logs backend-personal
docker compose logs frontend-personal
```

### 5.2 驗證 API

```bash
# 測試個人系統後端 API
curl http://localhost/api/records/stats/in-progress

# 應該會看到 JSON 回應
```

---

## 步驟 6: 訪問應用

### 6.1 在瀏覽器訪問

- **前端**: http://38.54.89.136
- **後端 API**: http://38.54.89.136/api/records

### 6.2 功能測試

1. ✅ 查看記錄列表
2. ✅ 新增記錄
3. ✅ 編輯記錄
4. ✅ 刪除記錄
5. ✅ 搜尋和篩選
6. ✅ 匯出 Excel

---

## 🔧 疑難排解

### 問題 1: MySQL 容器無法啟動

```bash
# 檢查日誌
docker compose logs mysql

# 檢查端口是否被佔用
sudo ss -tulpn | grep :3306
```

### 問題 2: 後端無法連接資料庫

```bash
# 檢查後端日誌
docker compose logs backend-personal

# 測試資料庫連線
docker compose exec mysql mysql -u appuser -papppassword qa_tracker -e "SELECT 1;"
```

### 問題 3: 前端無法連接後端

檢查 CORS 配置和防火牆設定。

### 問題 4: 資料遷移失敗

1. 檢查 JSON 格式是否正確
2. 檢查時間戳記格式
3. 檢查外鍵約束（先遷移 users，再遷移 records）

---

## 📝 遷移檢查清單

- [ ] Firebase 資料已備份
- [ ] 專案已上傳到虛擬主機
- [ ] MySQL 資料庫已建立
- [ ] 資料表結構正確（users, records, config）
- [ ] Firebase 資料已匯出為 JSON
- [ ] 資料已成功遷移到 MySQL
- [ ] 後端 API 正常運行
- [ ] 前端頁面可以訪問
- [ ] 所有功能測試通過
- [ ] 資料完整性驗證通過

---

## 🎉 遷移完成後

1. **更新 DNS**（如果有域名）
2. **設定 SSL 證書**（推薦使用 Let's Encrypt）
3. **設定自動備份**（MySQL 資料）
4. **監控服務狀態**
5. **通知使用者**系統已遷移

---

## 📞 需要協助？

如果遇到問題，請檢查：
- `TROUBLESHOOTING.md` - 故障排除指南
- Docker 容器日誌

祝遷移順利！🚀
