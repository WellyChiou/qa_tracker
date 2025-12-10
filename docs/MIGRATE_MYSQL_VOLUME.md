# MySQL Volume 遷移指南

## 遷移目的

將 MySQL 資料庫從 Docker named volume (`mysql_data`) 遷移到主機目錄 (`/root/project/work/mysql`)，以便：
- 資料持久化在主機文件系統
- 更容易備份和管理
- Docker 重新部署不會影響資料

## 遷移步驟

### 步驟 1: 備份現有資料（重要！）

**重要：需要在包含 `docker-compose.yml` 的專案根目錄執行命令**

```bash
# 方法 1: 切換到專案根目錄（推薦）
cd /root/project/qa_tracker  # 或您的專案根目錄路徑
docker compose exec mysql mysqldump -u root -prootpassword --all-databases > /root/project/work/mysql_backup_$(date +%Y%m%d_%H%M%S).sql

# 方法 2: 使用 -f 參數指定配置文件路徑
docker compose -f /root/project/qa_tracker/docker-compose.yml exec mysql mysqldump -u root -prootpassword --all-databases > /root/project/work/mysql_backup_$(date +%Y%m%d_%H%M%S).sql

# 或者分別導出兩個資料庫
cd /root/project/qa_tracker  # 切換到專案根目錄
docker compose exec mysql mysqldump -u root -prootpassword qa_tracker > /root/project/work/qa_tracker_backup_$(date +%Y%m%d_%H%M%S).sql
docker compose exec mysql mysqldump -u root -prootpassword church > /root/project/work/church_backup_$(date +%Y%m%d_%H%M%S).sql
```

### 步驟 2: 停止服務

```bash
# 在專案根目錄執行
cd /root/project/qa_tracker  # 或您的專案根目錄路徑
docker compose down

# 或使用 -f 參數
docker compose -f /root/project/qa_tracker/docker-compose.yml down
```

### 步驟 3: 遷移資料（如果 mysql_data volume 中有資料）

```bash
# 查找 mysql_data volume 的位置
docker volume inspect qa_tracker_mysql_data

# 複製資料到新目錄（根據 volume 的實際位置調整路徑）
# 注意：volume 的位置通常在 /var/lib/docker/volumes/qa_tracker_mysql_data/_data
# 如果無法直接訪問，可以使用臨時容器複製

# 方法 1: 使用臨時容器複製（推薦）
docker run --rm \
  -v qa_tracker_mysql_data:/source:ro \
  -v /root/project/work/mysql:/target \
  alpine sh -c "cp -a /source/. /target/"

# 方法 2: 如果可以直接訪問 Docker volume 目錄
# cp -a /var/lib/docker/volumes/qa_tracker_mysql_data/_data/* /root/project/work/mysql/
```

### 步驟 4: 創建新目錄並設置權限

```bash
mkdir -p /root/project/work/mysql
chmod 755 /root/project/work/mysql
# MySQL 容器內的 mysql 用戶 UID 通常是 999
chown -R 999:999 /root/project/work/mysql
```

### 步驟 5: 更新 docker-compose.yml

已經更新為使用主機目錄映射：
```yaml
volumes:
  - /root/project/work/mysql:/var/lib/mysql
```

### 步驟 6: 啟動服務

```bash
# 在專案根目錄執行
cd /root/project/qa_tracker  # 或您的專案根目錄路徑
docker compose up -d

# 或使用 -f 參數
docker compose -f /root/project/qa_tracker/docker-compose.yml up -d
```

### 步驟 7: 驗證資料

```bash
# 在專案根目錄執行
cd /root/project/qa_tracker  # 或您的專案根目錄路徑

# 檢查資料庫是否正常
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;"

# 檢查資料是否完整
docker compose exec mysql mysql -u root -prootpassword qa_tracker -e "SELECT COUNT(*) FROM users;"
docker compose exec mysql mysql -u root -prootpassword church -e "SELECT COUNT(*) FROM sunday_messages;"
```

## 如果遷移失敗

如果新目錄中沒有資料或資料不完整，可以從備份恢復：

```bash
# 切換到專案根目錄
cd /root/project/qa_tracker  # 或您的專案根目錄路徑

# 停止服務
docker compose down

# 確保目錄存在
mkdir -p /root/project/work/mysql
chmod 755 /root/project/work/mysql
chown -R 999:999 /root/project/work/mysql

# 啟動服務（會自動初始化）
docker compose up -d

# 等待 MySQL 啟動完成
sleep 10

# 恢復資料（替換 YYYYMMDD_HHMMSS 為實際的備份文件名）
docker compose exec -T mysql mysql -u root -prootpassword < /root/project/work/qa_tracker_backup_YYYYMMDD_HHMMSS.sql
docker compose exec -T mysql mysql -u root -prootpassword < /root/project/work/church_backup_YYYYMMDD_HHMMSS.sql
```

## 清理舊 Volume（可選）

遷移成功並驗證無誤後，可以刪除舊的 named volume：

```bash
# 列出所有 volumes
docker volume ls

# 刪除舊的 mysql_data volume（請確認資料已成功遷移！）
docker volume rm qa_tracker_mysql_data
```

## 注意事項

1. **備份優先**：遷移前務必備份資料
2. **權限設置**：確保 MySQL 容器可以寫入新目錄（UID/GID 999）
3. **磁盤空間**：確保 `/root/project/work` 有足夠的磁盤空間
4. **備份策略**：建議定期備份 `/root/project/work/mysql` 目錄

## 備份腳本示例

創建定期備份腳本 `/root/project/work/backup_mysql.sh`：

```bash
#!/bin/bash
BACKUP_DIR="/root/project/work/mysql_backups"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# 備份整個 mysql 目錄
tar -czf $BACKUP_DIR/mysql_$DATE.tar.gz -C /root/project/work mysql

# 保留最近 7 天的備份
find $BACKUP_DIR -name "mysql_*.tar.gz" -mtime +7 -delete

echo "備份完成: $BACKUP_DIR/mysql_$DATE.tar.gz"
```

設置定時任務：
```bash
# 每天凌晨 2 點備份
0 2 * * * /root/project/work/backup_mysql.sh
```

