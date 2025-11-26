# 🛡️ 資料庫資料保護指南

## ⚠️ 重要：什麼操作會清空資料庫？

### ❌ **會清空資料的操作**

#### 1. 刪除 Docker Volume（最危險！）
```bash
# ⚠️ 這個命令會刪除所有資料！
docker compose down -v

# 或手動刪除 volume
docker volume rm docker-vue-java-mysql_mysql_data
```

#### 2. 重新建立資料庫容器（如果 volume 被刪除）
```bash
# 如果先執行了 docker compose down -v，再執行這個就會清空
docker compose up -d mysql
```

#### 3. 手動刪除資料庫或資料表
```sql
-- ⚠️ 這些 SQL 會清空資料
DROP DATABASE qa_tracker;
DROP TABLE records;
TRUNCATE TABLE records;
```

### ✅ **不會清空資料的操作**

#### 1. `docker compose up -d --build` ✅ 安全
```bash
# 這個命令不會清空資料庫！
docker compose up -d --build
```
**原因：**
- 只會重新構建後端和前端容器
- MySQL 容器使用 `mysql_data` volume 持久化儲存
- Volume 中的資料不會被刪除

#### 2. `docker compose restart` ✅ 安全
```bash
# 重啟服務，資料完全安全
docker compose restart
```

#### 3. `docker compose down` ✅ 安全（不加 -v）
```bash
# 停止服務，但保留 volume
docker compose down

# 之後再啟動，資料還在
docker compose up -d
```

#### 4. 修改程式碼後重新構建 ✅ 安全
```bash
# 修改 Java 或前端程式碼後
docker compose up -d --build backend
docker compose up -d --build frontend
```

#### 5. 修改 `schema.sql` ✅ 安全（如果使用 IF NOT EXISTS）
```sql
-- 我們的 schema.sql 使用 IF NOT EXISTS，不會刪除現有資料
CREATE TABLE IF NOT EXISTS users (...)
```

---

## 🔒 資料儲存位置

### Docker Volume 持久化

您的資料庫資料儲存在 Docker Volume 中：

```yaml
volumes:
  mysql_data:  # 這個 volume 儲存所有資料庫資料
```

**Volume 位置：**
- Linux: `/var/lib/docker/volumes/docker-vue-java-mysql_mysql_data/_data`
- 只要這個 volume 存在，資料就不會丟失

### 檢查 Volume 是否存在

```bash
# 列出所有 volumes
docker volume ls

# 應該會看到
# docker-vue-java-mysql_mysql_data

# 檢查 volume 詳情
docker volume inspect docker-vue-java-mysql_mysql_data
```

---

## 💾 備份資料庫（強烈建議！）

### 方法 1: 使用 mysqldump（推薦）

```bash
# 備份資料庫
docker compose exec mysql mysqldump -u appuser -papppassword qa_tracker > backup-$(date +%Y%m%d).sql

# 或備份到檔案
docker compose exec mysql mysqldump -u appuser -papppassword qa_tracker > /root/backups/qa_tracker-$(date +%Y%m%d-%H%M%S).sql
```

### 方法 2: 備份整個 Volume

```bash
# 停止 MySQL 容器
docker compose stop mysql

# 備份 volume
docker run --rm -v docker-vue-java-mysql_mysql_data:/data -v $(pwd):/backup alpine tar czf /backup/mysql-data-backup-$(date +%Y%m%d).tar.gz /data

# 重新啟動
docker compose start mysql
```

### 方法 3: 自動備份腳本

建立 `backup-db.sh`：

```bash
#!/bin/bash
BACKUP_DIR="/root/backups"
DATE=$(date +%Y%m%d-%H%M%S)
mkdir -p $BACKUP_DIR

# 備份資料庫
docker compose exec -T mysql mysqldump -u appuser -papppassword qa_tracker > $BACKUP_DIR/qa_tracker-$DATE.sql

# 壓縮備份
gzip $BACKUP_DIR/qa_tracker-$DATE.sql

# 刪除 7 天前的備份
find $BACKUP_DIR -name "qa_tracker-*.sql.gz" -mtime +7 -delete

echo "備份完成: qa_tracker-$DATE.sql.gz"
```

設定定時備份（crontab）：

```bash
# 編輯 crontab
crontab -e

# 每天凌晨 2 點備份
0 2 * * * /root/project/work/docker-vue-java-mysql/backup-db.sh
```

---

## 🔄 恢復資料庫

### 從 SQL 備份恢復

```bash
# 恢復資料庫
docker compose exec -T mysql mysql -u appuser -papppassword qa_tracker < backup-20241201.sql

# 或從壓縮檔恢復
gunzip < backup-20241201.sql.gz | docker compose exec -T mysql mysql -u appuser -papppassword qa_tracker
```

### 從 Volume 備份恢復

```bash
# 停止 MySQL
docker compose stop mysql

# 恢復 volume
docker run --rm -v docker-vue-java-mysql_mysql_data:/data -v $(pwd):/backup alpine tar xzf /backup/mysql-data-backup-20241201.tar.gz -C /

# 重新啟動
docker compose start mysql
```

---

## ✅ 安全操作檢查清單

執行任何命令前，檢查：

### ✅ 安全操作（資料不會丟失）

- [x] `docker compose up -d --build`
- [x] `docker compose restart`
- [x] `docker compose down`（不加 -v）
- [x] `docker compose stop`
- [x] `docker compose start`
- [x] 修改程式碼後重新構建
- [x] 修改配置檔案

### ⚠️ 危險操作（可能清空資料）

- [ ] `docker compose down -v` ⚠️ **會刪除所有資料！**
- [ ] `docker volume rm docker-vue-java-mysql_mysql_data` ⚠️ **會刪除所有資料！**
- [ ] 執行 `DROP DATABASE` 或 `DROP TABLE` SQL ⚠️
- [ ] 刪除整個專案目錄（如果 volume 在專案內）⚠️

---

## 🎯 實際使用建議

### 日常開發（資料已存在）

```bash
# ✅ 安全：修改程式碼後重新構建
docker compose up -d --build backend

# ✅ 安全：重啟服務
docker compose restart

# ✅ 安全：查看日誌
docker compose logs backend
```

### 首次部署或重新建立

```bash
# 如果資料庫是空的，可以安全執行
docker compose up -d --build

# schema.sql 會自動執行（使用 IF NOT EXISTS，不會刪除現有資料）
```

### 需要清空資料庫時（謹慎！）

```bash
# 1. 先備份！
docker compose exec mysql mysqldump -u appuser -papppassword qa_tracker > backup.sql

# 2. 確認備份成功
ls -lh backup.sql

# 3. 然後才清空（如果需要）
docker compose down -v
docker compose up -d --build
```

---

## 🔍 驗證資料是否還在

### 檢查資料庫記錄數

```bash
# 檢查記錄數
docker compose exec mysql mysql -u appuser -papppassword qa_tracker -e "SELECT COUNT(*) as total FROM records;"

# 查看一些記錄
docker compose exec mysql mysql -u appuser -papppassword qa_tracker -e "SELECT * FROM records LIMIT 5;"
```

### 檢查 Volume 大小

```bash
# Volume 有資料的話，大小不會是 0
docker volume inspect docker-vue-java-mysql_mysql_data | grep Mountpoint
du -sh /var/lib/docker/volumes/docker-vue-java-mysql_mysql_data/_data
```

---

## 📝 總結

### ✅ **`docker compose up -d --build` 不會清空資料庫！**

**原因：**
1. 資料儲存在 Docker Volume 中（持久化）
2. 只會重新構建容器，不會刪除 Volume
3. `schema.sql` 使用 `IF NOT EXISTS`，不會覆蓋現有資料

### ⚠️ **只有這些操作會清空資料：**
1. `docker compose down -v`（刪除 volume）
2. 手動刪除 volume
3. 執行 DROP 語句

### 💡 **最佳實踐：**
1. **定期備份**：設定自動備份腳本
2. **小心使用 `-v` 參數**：只在確定要清空資料時使用
3. **驗證備份**：定期測試備份是否可恢復
4. **使用 IF NOT EXISTS**：SQL 腳本使用安全語法

---

**記住：只要 `mysql_data` volume 還在，您的資料就安全！** 🛡️

