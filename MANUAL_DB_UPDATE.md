# 手動資料庫更新指南 - LINE Bot 功能

如果自動腳本無法執行，請按照以下步驟手動更新資料庫。

## 步驟 1：確保 MySQL 容器運行

```bash
# 啟動 MySQL 容器（如果還沒啟動）
docker-compose up -d mysql

# 或使用新版 Docker
docker compose up -d mysql
```

## 步驟 2：檢查當前資料庫結構

```bash
# 檢查 users 表結構
docker-compose exec mysql mysql -u appuser -papppassword qa_tracker -e "DESCRIBE users;"

# 或使用新版 Docker
docker compose exec mysql mysql -u appuser -papppassword qa_tracker -e "DESCRIBE users;"
```

## 步驟 3：執行資料庫遷移

### 方法 1：複製檔案到容器

```bash
# 複製遷移腳本到容器
docker-compose cp mysql/check-and-update-users-table.sql mysql:/tmp/

# 執行遷移腳本
docker-compose exec mysql mysql -u appuser -papppassword qa_tracker < mysql/check-and-update-users-table.sql

# 或使用新版 Docker
docker compose cp mysql/check-and-update-users-table.sql mysql:/tmp/
docker compose exec mysql mysql -u appuser -papppassword qa_tracker < mysql/check-and-update-users-table.sql
```

### 方法 2：直接執行 SQL 命令

連接到 MySQL 容器並執行：

```bash
# 進入 MySQL 容器
docker-compose exec mysql mysql -u appuser -papppassword qa_tracker

# 在 MySQL 提示符下執行：
source /tmp/check-and-update-users-table.sql
```

## 步驟 4：驗證更新結果

```bash
# 檢查更新後的表結構
docker-compose exec mysql mysql -u appuser -papppassword qa_tracker -e "DESCRIBE users;"

# 應該看到新增的 line_user_id 欄位：
# | line_user_id | varchar(50) | YES | UNI | NULL | |
```

## 步驟 5：重新啟動應用程式

```bash
# 重新啟動所有服務
docker-compose restart

# 或使用新版 Docker
docker compose restart
```

## 故障排除

### 如果遇到權限問題

```bash
# 檢查容器狀態
docker-compose ps

# 檢查 MySQL 日誌
docker-compose logs mysql
```

### 如果需要重新初始化資料庫

```bash
# 停止並刪除 MySQL 容器（會刪除所有資料！）
docker-compose down -v

# 重新啟動（會重新初始化資料庫）
docker-compose up --build mysql
```

## 驗證 LINE Bot 功能

1. **訪問應用程式：** http://localhost
2. **登入帳號**
3. **進入個人資料：** 右上角用戶名稱 → 個人資料設定
4. **綁定 LINE：** 在 LINE Bot 設定區域輸入您的 LINE 用戶 ID

### 測試 LINE 功能

綁定成功後，在 LINE 中發送：
```
幫助          - 查看指令
狀態          - 查看今日統計
支出 餐費 150 午餐  - 記錄費用
收入 薪水 50000     - 記錄收入
```
