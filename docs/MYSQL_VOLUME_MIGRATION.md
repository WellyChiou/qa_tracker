# MySQL Volume 遷移完整指南

## 概述

本指南說明如何將 MySQL 資料庫從 Docker named volume 遷移到主機目錄，以及遷移後的清理工作。

## 目錄

1. [遷移步驟](#遷移步驟)
2. [遷移完成確認](#遷移完成確認)
3. [清理舊 Volumes](#清理舊-volumes)
4. [故障排除](#故障排除)

---

## 遷移步驟

### 步驟 1: 備份現有資料（重要！）

**重要：需要在包含 `docker-compose.yml` 的專案根目錄執行命令**

```bash
# 方法 1: 切換到專案根目錄（推薦）
cd /root/project/work/docker-vue-java-mysql
docker compose exec mysql mysqldump -u root -prootpassword --all-databases > /root/project/work/mysql_backup_$(date +%Y%m%d_%H%M%S).sql

# 方法 2: 使用 -f 參數指定配置文件路徑
docker compose -f /root/project/work/docker-vue-java-mysql/docker-compose.yml exec mysql mysqldump -u root -prootpassword --all-databases > /root/project/work/mysql_backup_$(date +%Y%m%d_%H%M%S).sql

# 或者分別導出兩個資料庫
docker compose exec mysql mysqldump -u root -prootpassword qa_tracker > /root/project/work/qa_tracker_backup_$(date +%Y%m%d_%H%M%S).sql
docker compose exec mysql mysqldump -u root -prootpassword church > /root/project/work/church_backup_$(date +%Y%m%d_%H%M%S).sql
```

### 步驟 2: 停止服務

```bash
cd /root/project/work/docker-vue-java-mysql
docker compose down
```

### 步驟 3: 檢查舊 Volume

```bash
# 列出所有 volumes，查找 mysql_data
docker volume ls | grep mysql

# 檢查 volume 的詳細信息
docker volume inspect qa_tracker_mysql_data
# 或
docker volume inspect docker-vue-java-mysql_mysql_data
```

### 步驟 4: 創建目標目錄並設置權限

```bash
# 創建目標目錄
mkdir -p /root/project/work/mysql

# 設置權限（MySQL 容器內的 mysql 用戶 UID 是 999）
chown -R 999:999 /root/project/work/mysql
chmod 755 /root/project/work/mysql
```

### 步驟 5: 遷移資料

```bash
# 使用臨時容器複製資料
# 先找到正確的 volume 名稱
VOLUME_NAME=$(docker volume ls | grep mysql_data | awk '{print $2}' | head -1)

# 遷移資料
docker run --rm \
  -v "$VOLUME_NAME":/source:ro \
  -v /root/project/work/mysql:/target \
  alpine sh -c "cp -a /source/. /target/"

# 再次設置權限
chown -R 999:999 /root/project/work/mysql
```

### 步驟 6: 更新 docker-compose.yml

確保 `docker-compose.yml` 已配置為使用主機目錄：

```yaml
volumes:
  - /root/project/work/mysql:/var/lib/mysql
```

### 步驟 7: 啟動服務

```bash
cd /root/project/work/docker-vue-java-mysql
docker compose up -d mysql

# 等待 MySQL 啟動完成（約 30 秒）
sleep 30
```

### 步驟 8: 驗證資料

```bash
# 檢查資料庫是否存在
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;"

# 檢查資料是否完整
docker compose exec mysql mysql -u root -prootpassword qa_tracker -e "SELECT COUNT(*) FROM users;"
docker compose exec mysql mysql -u root -prootpassword church -e "SELECT COUNT(*) FROM sunday_messages;"
```

---

## 遷移完成確認

### 確認資料存儲位置

**是的，之後所有資料都會放在 `/root/project/work/mysql`**

這是因為 `docker-compose.yml` 已經配置了 volume 映射：

```yaml
volumes:
  - /root/project/work/mysql:/var/lib/mysql
```

### 驗證遷移完成

```bash
# 1. 確認 docker-compose.yml 配置正確
grep -A 1 "volumes:" docker-compose.yml | grep mysql

# 2. 確認資料目錄存在且有資料
ls -lh /root/project/work/mysql | head -10
du -sh /root/project/work/mysql

# 3. 確認 MySQL 正在使用這個目錄
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;"
```

### 資料存儲結構

```
/root/project/work/
├── mysql/                    # MySQL 資料庫文件
│   ├── ibdata1              # InnoDB 資料文件
│   ├── ib_logfile0          # InnoDB 日誌文件
│   ├── qa_tracker/          # qa_tracker 資料庫
│   ├── church/              # church 資料庫
│   └── ...
└── uploads/                  # 上傳的文件
    └── church/
        ├── activities/
        └── sunday-messages/
```

---

## 清理舊 Volumes

### 安全檢查步驟

在刪除 volume 之前，請務必確認以下事項：

#### 1. 確認當前 MySQL 使用的存儲位置

```bash
cd /root/project/work/docker-vue-java-mysql
grep -A 2 "volumes:" docker-compose.yml | grep mysql
# 應該看到：- /root/project/work/mysql:/var/lib/mysql
```

#### 2. 確認主機目錄有資料

```bash
du -sh /root/project/work/mysql
ls -la /root/project/work/mysql | head -10
```

#### 3. 確認 MySQL 正在使用主機目錄

```bash
docker inspect mysql_db | grep -A 10 "Mounts"
# 應該看到 "Source": "/root/project/work/mysql"
```

#### 4. 確認資料庫可以正常查詢

```bash
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;"
docker compose exec mysql mysql -u root -prootpassword qa_tracker -e "SELECT COUNT(*) FROM users;"
```

#### 5. 確認 volume 沒有被當前容器使用

```bash
docker ps -a --filter volume=qa_tracker_mysql_data
# 如果沒有輸出，表示沒有容器在使用
```

### 安全刪除流程

只有在**所有檢查都通過**後，才執行刪除：

```bash
# 1. 停止所有服務
docker compose down

# 2. 再次確認沒有容器使用該 volume
docker ps -a --filter volume=qa_tracker_mysql_data
# 應該沒有輸出

# 3. 刪除 volume
docker volume rm qa_tracker_mysql_data

# 4. 重新啟動服務（使用主機目錄）
docker compose up -d

# 5. 驗證資料還在
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;"
```

### 強制刪除（如果 volume 正在使用）

如果出現 "volume is in use" 錯誤：

```bash
# 1. 停止所有容器
docker compose down

# 2. 查找並刪除使用該 volume 的容器
docker ps -a --filter volume=qa_tracker_mysql_data --format "{{.ID}}" | xargs -r docker rm -f

# 3. 刪除 volume
docker volume rm qa_tracker_mysql_data

# 4. 重新啟動服務
docker compose up -d
```

### 清理建議

**推薦做法：**
- ✅ 保留一個有資料的 volume 作為備份（如 `docker-vue-java-mysql_mysql_data`）
- ❌ 刪除空的 volume（如 `qa_tracker_mysql_data`）

**完全清理（如果確定不需要備份）：**
```bash
docker volume rm docker-vue-java-mysql_mysql_data
docker volume rm qa_tracker_mysql_data
```

---

## 故障排除

### 問題 1: 找不到 volume

```bash
# 檢查所有 volumes
docker volume ls

# 檢查 volume 詳細信息
docker volume inspect <volume_name>
```

### 問題 2: 權限錯誤

```bash
# 強制設置權限
sudo chown -R 999:999 /root/project/work/mysql
sudo chmod -R 755 /root/project/work/mysql
```

### 問題 3: 遷移後 MySQL 無法啟動

```bash
# 檢查日誌
docker compose logs mysql

# 檢查目錄權限
ls -la /root/project/work/mysql

# 檢查文件完整性
ls -la /root/project/work/mysql | head -20
```

### 問題 4: 從備份恢復

如果遷移失敗，可以從備份恢復：

```bash
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

---

## 備份策略

### 定期備份腳本

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

---

## 注意事項

1. **備份優先**：遷移前務必備份資料
2. **權限設置**：確保 MySQL 容器可以寫入新目錄（UID/GID 999）
3. **磁盤空間**：確保 `/root/project/work` 有足夠的磁盤空間
4. **測試運行**：遷移後讓系統運行一段時間，確認一切正常
5. **備份策略**：建議定期備份 `/root/project/work/mysql` 目錄

---

## 總結

**遷移完成後：**
- ✅ 資料現在存儲在 `/root/project/work/mysql`
- ✅ 之後所有資料都會自動存儲在這裡
- ✅ Docker 重新部署不會影響資料
- ✅ 資料持久化在主機文件系統
- ✅ 容易備份和管理

