# MySQL 資料庫還原指南

## 還原步驟

### 步驟 1: 確認備份文件位置

```bash
# 列出備份文件
ls -lh /root/project/work/mysql_backup_*.sql

# 查看最新的備份文件
ls -lt /root/project/work/mysql_backup_*.sql | head -1
```

### 步驟 2: 確認當前資料庫狀態（可選）

```bash
# 檢查當前資料庫
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;"

# 檢查資料庫是否為空
docker compose exec mysql mysql -u root -prootpassword qa_tracker -e "SHOW TABLES;" 2>/dev/null || echo "qa_tracker 為空"
docker compose exec mysql mysql -u root -prootpassword church -e "SHOW TABLES;" 2>/dev/null || echo "church 為空"
```

### 步驟 3: 還原資料庫

**方法 A: 還原所有資料庫（推薦，如果備份是 --all-databases）**

```bash
# 在專案根目錄執行
cd /root/project/work/docker-vue-java-mysql  # 或您的專案根目錄

# 還原所有資料庫（替換 YYYYMMDD_HHMMSS 為實際的備份文件名）
docker compose exec -T mysql mysql -u root -prootpassword < /root/project/work/mysql_backup_YYYYMMDD_HHMMSS.sql

# 或者使用完整路徑
docker compose -f /root/project/work/docker-vue-java-mysql/docker-compose.yml exec -T mysql mysql -u root -prootpassword < /root/project/work/mysql_backup_YYYYMMDD_HHMMSS.sql
```

**方法 B: 分別還原各個資料庫（如果備份是分別導出的）**

```bash
# 還原 qa_tracker 資料庫
docker compose exec -T mysql mysql -u root -prootpassword qa_tracker < /root/project/work/qa_tracker_backup_YYYYMMDD_HHMMSS.sql

# 還原 church 資料庫
docker compose exec -T mysql mysql -u root -prootpassword church < /root/project/work/church_backup_YYYYMMDD_HHMMSS.sql
```

### 步驟 4: 驗證還原結果

```bash
# 檢查資料庫是否存在
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;"

# 檢查 qa_tracker 資料庫的表
docker compose exec mysql mysql -u root -prootpassword qa_tracker -e "SHOW TABLES;"

# 檢查 church 資料庫的表
docker compose exec mysql mysql -u root -prootpassword church -e "SHOW TABLES;"

# 檢查資料數量（示例）
docker compose exec mysql mysql -u root -prootpassword qa_tracker -e "SELECT COUNT(*) as user_count FROM users;"
docker compose exec mysql mysql -u root -prootpassword church -e "SELECT COUNT(*) as message_count FROM sunday_messages;"
```

## 完整還原腳本

創建一個還原腳本 `/root/project/work/restore_mysql.sh`：

```bash
#!/bin/bash

# 設置備份文件路徑（請修改為實際的備份文件名）
BACKUP_FILE="/root/project/work/mysql_backup_YYYYMMDD_HHMMSS.sql"

# 專案根目錄（請修改為實際路徑）
PROJECT_DIR="/root/project/work/docker-vue-java-mysql"

# 檢查備份文件是否存在
if [ ! -f "$BACKUP_FILE" ]; then
    echo "錯誤：備份文件不存在: $BACKUP_FILE"
    echo "可用的備份文件："
    ls -lh /root/project/work/mysql_backup_*.sql 2>/dev/null || echo "沒有找到備份文件"
    exit 1
fi

# 切換到專案目錄
cd "$PROJECT_DIR" || exit 1

echo "開始還原資料庫..."
echo "備份文件: $BACKUP_FILE"

# 還原資料庫
docker compose exec -T mysql mysql -u root -prootpassword < "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo "✅ 資料庫還原成功！"
    
    # 驗證
    echo "驗證還原結果..."
    docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;"
else
    echo "❌ 資料庫還原失敗！"
    exit 1
fi
```

使用腳本：

```bash
# 賦予執行權限
chmod +x /root/project/work/restore_mysql.sh

# 編輯腳本，修改備份文件路徑
nano /root/project/work/restore_mysql.sh

# 執行還原
/root/project/work/restore_mysql.sh
```

## 快速還原命令（一行）

```bash
# 替換 YYYYMMDD_HHMMSS 為實際的備份文件名
cd /root/project/work/docker-vue-java-mysql && docker compose exec -T mysql mysql -u root -prootpassword < /root/project/work/mysql_backup_YYYYMMDD_HHMMSS.sql && echo "還原完成" && docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;"

# mac version
cd /Users/wellychiou/my-github/docker-vue-java-mysql && \
sed '/^mysqldump: /d' /Users/wellychiou/my-github/backups/qa_tracker_20251216_180000.sql \
| docker compose exec -T mysql mysql -uroot -prootpassword qa_tracker

cd /Users/wellychiou/my-github/docker-vue-java-mysql && \
sed '/^mysqldump: /d' /Users/wellychiou/my-github/backups/church_20251216_180000.sql \
| docker compose exec -T mysql mysql -uroot -prootpassword church

```

## 注意事項

1. **備份文件路徑**：確保備份文件路徑正確
2. **專案目錄**：需要在包含 `docker-compose.yml` 的目錄執行
3. **資料庫狀態**：還原前建議先確認當前資料庫狀態
4. **權限問題**：確保有讀取備份文件的權限
5. **磁盤空間**：確保有足夠的磁盤空間

## 如果還原失敗

### 錯誤 1: 找不到備份文件

```bash
# 列出所有備份文件
ls -lh /root/project/work/mysql_backup_*.sql

# 使用完整路徑
docker compose exec -T mysql mysql -u root -prootpassword < /完整/路徑/mysql_backup_YYYYMMDD_HHMMSS.sql
```

### 錯誤 2: 權限不足

```bash
# 檢查文件權限
ls -l /root/project/work/mysql_backup_*.sql

# 修改權限（如果需要）
chmod 644 /root/project/work/mysql_backup_*.sql
```

### 錯誤 3: MySQL 容器未運行

```bash
# 檢查容器狀態
docker compose ps mysql

# 啟動容器
docker compose up -d mysql

# 等待啟動完成
sleep 10
```

### 錯誤 4: 字符編碼問題

如果還原時出現字符編碼錯誤，可以指定編碼：

```bash
docker compose exec -T mysql mysql -u root -prootpassword --default-character-set=utf8mb4 < /root/project/work/mysql_backup_YYYYMMDD_HHMMSS.sql
```

## 還原後檢查清單

- [ ] 資料庫列表正確（qa_tracker, church）
- [ ] 表結構完整
- [ ] 資料數量正確
- [ ] 應用程序可以正常連接
- [ ] 測試關鍵功能是否正常

