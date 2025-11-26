# 資料遷移指南

## 從 Firebase 遷移到 MySQL

### 步驟 1: 從 Firebase 匯出資料

1. 前往 Firebase Console: https://console.firebase.google.com/
2. 選擇專案：`fb-issue-record`
3. 前往 Firestore Database
4. 使用 Firebase CLI 匯出資料：

```bash
# 安裝 Firebase CLI
npm install -g firebase-tools

# 登入 Firebase
firebase login

# 匯出 Firestore 資料
firebase firestore:export ./firebase-export
```

或者使用 Firebase Console 的匯出功能（如果有提供）。

### 步驟 2: 轉換資料格式

Firebase 匯出的格式可能需要轉換。如果匯出的是 JSON 格式，需要轉換為以下結構：

```json
{
  "users": {
    "user-uid-1": {
      "email": "user@example.com",
      "displayName": "User Name",
      ...
    }
  },
  "records": {
    "record-id-1": {
      "issue_number": 1234,
      "status": 1,
      ...
    }
  },
  "config": {
    "gitlab": {
      "token": "..."
    }
  }
}
```

### 步驟 3: 執行遷移腳本

```bash
# 進入遷移目錄
cd migrate

# 安裝依賴
npm install node-fetch mysql2

# 執行遷移（確保 MySQL 容器已啟動）
node firebase-to-mysql.js ../firebase-export/firestore_export/firestore_export.overall_export_metadata
```

或者如果資料已經是正確的 JSON 格式：

```bash
node firebase-to-mysql.js firebase-data.json
```

### 步驟 4: 驗證遷移結果

連接到 MySQL 檢查資料：

```bash
# 進入 MySQL 容器
docker compose exec mysql mysql -u appuser -papppassword qa_tracker

# 檢查記錄數
SELECT COUNT(*) FROM records;
SELECT COUNT(*) FROM users;

# 檢查一些記錄
SELECT * FROM records LIMIT 10;
```

### 環境變數

可以透過環境變數設定資料庫連線：

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_USER=appuser
export DB_PASSWORD=apppassword
export DB_NAME=qa_tracker

node firebase-to-mysql.js firebase-data.json
```

### 注意事項

1. **備份資料**：遷移前請先備份 Firebase 資料
2. **測試環境**：建議先在測試環境執行遷移
3. **資料驗證**：遷移後請仔細檢查資料完整性
4. **時間戳記**：Firebase Timestamp 會自動轉換為 MySQL DATETIME
5. **重複執行**：腳本支援重複執行，已存在的記錄會被略過

### 疑難排解

#### 問題：連線失敗

檢查 MySQL 容器是否運行：
```bash
docker compose ps mysql
```

檢查資料庫是否存在：
```bash
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;"
```

#### 問題：編碼問題

確保 MySQL 使用 utf8mb4：
```sql
SHOW VARIABLES LIKE 'character_set%';
```

#### 問題：外鍵約束錯誤

如果 users 表為空，先遷移 users，再遷移 records。

