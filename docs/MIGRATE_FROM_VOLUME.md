# 從 Docker Volume 遷移 MySQL 資料

## 遷移步驟

### 步驟 1: 檢查舊 Volume 是否存在

```bash
# 列出所有 volumes，查找 mysql_data
docker volume ls | grep mysql

# 檢查 volume 的詳細信息
docker volume inspect qa_tracker_mysql_data
```

### 步驟 2: 停止 MySQL 服務（重要！）

```bash
# 在專案根目錄執行
cd /root/project/work/docker-vue-java-mysql

# 停止 MySQL 容器
docker compose stop mysql

# 或者停止所有服務
docker compose down
```

### 步驟 3: 確保目標目錄存在並設置權限

```bash
# 創建目標目錄
mkdir -p /root/project/work/mysql

# 設置權限（MySQL 容器內的 mysql 用戶 UID 是 999）
chown -R 999:999 /root/project/work/mysql
chmod 755 /root/project/work/mysql
```

### 步驟 4: 遷移資料

```bash
# 使用臨時容器複製資料
docker run --rm \
  -v qa_tracker_mysql_data:/source:ro \
  -v /root/project/work/mysql:/target \
  alpine sh -c "cp -a /source/. /target/"

# 如果上面的命令找不到 volume，嘗試使用完整 volume 名稱
# 先查找完整的 volume 名稱
docker volume ls

# 然後使用完整名稱（例如：docker-vue-java-mysql_mysql_data）
docker run --rm \
  -v docker-vue-java-mysql_mysql_data:/source:ro \
  -v /root/project/work/mysql:/target \
  alpine sh -c "cp -a /source/. /target/"
```

### 步驟 5: 再次設置權限（遷移後）

```bash
# 確保權限正確
chown -R 999:999 /root/project/work/mysql
chmod 755 /root/project/work/mysql

# 檢查文件是否已複製
ls -la /root/project/work/mysql | head -20
```

### 步驟 6: 啟動 MySQL 服務

```bash
# 在專案根目錄
cd /root/project/work/docker-vue-java-mysql

# 啟動 MySQL
docker compose up -d mysql

# 等待 MySQL 啟動完成（約 30 秒）
sleep 30

# 檢查日誌
docker compose logs mysql --tail 20
```

### 步驟 7: 驗證資料

```bash
# 檢查資料庫是否存在
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;"

# 檢查 qa_tracker 資料庫的表
docker compose exec mysql mysql -u root -prootpassword qa_tracker -e "SHOW TABLES;"

# 檢查 church 資料庫的表
docker compose exec mysql mysql -u root -prootpassword church -e "SHOW TABLES;"

# 檢查資料數量（示例）
docker compose exec mysql mysql -u root -prootpassword qa_tracker -e "SELECT COUNT(*) as user_count FROM users;" 2>/dev/null
docker compose exec mysql mysql -u root -prootpassword church -e "SELECT COUNT(*) as message_count FROM sunday_messages;" 2>/dev/null
```

## 完整遷移腳本

創建一個遷移腳本 `/root/project/work/migrate_from_volume.sh`：

```bash
#!/bin/bash

set -e  # 遇到錯誤立即退出

PROJECT_DIR="/root/project/work/docker-vue-java-mysql"
TARGET_DIR="/root/project/work/mysql"

echo "=== MySQL Volume 遷移腳本 ==="

# 步驟 1: 檢查 volume
echo "1. 檢查舊 volume..."
VOLUME_NAME=$(docker volume ls | grep mysql_data | awk '{print $2}' | head -1)

if [ -z "$VOLUME_NAME" ]; then
    echo "❌ 找不到 mysql_data volume"
    echo "可用的 volumes:"
    docker volume ls
    exit 1
fi

echo "✅ 找到 volume: $VOLUME_NAME"

# 步驟 2: 停止 MySQL
echo "2. 停止 MySQL 服務..."
cd "$PROJECT_DIR"
docker compose stop mysql || docker compose down

# 步驟 3: 創建目標目錄
echo "3. 創建目標目錄..."
mkdir -p "$TARGET_DIR"
chown -R 999:999 "$TARGET_DIR"
chmod 755 "$TARGET_DIR"

# 步驟 4: 遷移資料
echo "4. 遷移資料（這可能需要一些時間）..."
docker run --rm \
  -v "$VOLUME_NAME":/source:ro \
  -v "$TARGET_DIR":/target \
  alpine sh -c "cp -a /source/. /target/"

if [ $? -eq 0 ]; then
    echo "✅ 資料遷移成功"
else
    echo "❌ 資料遷移失敗"
    exit 1
fi

# 步驟 5: 設置權限
echo "5. 設置權限..."
chown -R 999:999 "$TARGET_DIR"

# 步驟 6: 啟動 MySQL
echo "6. 啟動 MySQL..."
cd "$PROJECT_DIR"
docker compose up -d mysql

echo "7. 等待 MySQL 啟動..."
sleep 30

# 步驟 7: 驗證
echo "8. 驗證資料..."
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;" || echo "⚠️ MySQL 尚未完全啟動，請稍後再驗證"

echo ""
echo "=== 遷移完成 ==="
echo "請執行以下命令驗證："
echo "  docker compose exec mysql mysql -u root -prootpassword -e 'SHOW DATABASES;'"
echo "  docker compose exec mysql mysql -u root -prootpassword qa_tracker -e 'SHOW TABLES;'"
echo "  docker compose exec mysql mysql -u root -prootpassword church -e 'SHOW TABLES;'"
```

使用腳本：

```bash
# 賦予執行權限
chmod +x /root/project/work/migrate_from_volume.sh

# 執行遷移
/root/project/work/migrate_from_volume.sh
```

## 快速遷移命令（一行）

```bash
cd /root/project/work/docker-vue-java-mysql && \
docker compose stop mysql && \
mkdir -p /root/project/work/mysql && \
chown -R 999:999 /root/project/work/mysql && \
docker run --rm -v qa_tracker_mysql_data:/source:ro -v /root/project/work/mysql:/target alpine sh -c "cp -a /source/. /target/" && \
chown -R 999:999 /root/project/work/mysql && \
docker compose up -d mysql && \
echo "遷移完成，等待 MySQL 啟動..." && \
sleep 30 && \
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;"
```

## 如果找不到 Volume

如果找不到 `qa_tracker_mysql_data`，嘗試查找其他可能的 volume 名稱：

```bash
# 列出所有 volumes
docker volume ls

# 查找包含 mysql 的 volume
docker volume ls | grep -i mysql

# 查找專案相關的 volume（根據專案目錄名稱）
docker volume ls | grep docker-vue-java-mysql
```

然後使用找到的 volume 名稱替換命令中的 `qa_tracker_mysql_data`。

## 注意事項

1. **備份優先**：遷移前建議先備份（雖然您已經嘗試過，但備份文件是空的）
2. **停止服務**：遷移前必須停止 MySQL，否則可能導致資料不一致
3. **權限設置**：確保目錄權限正確（999:999）
4. **磁盤空間**：確保有足夠的磁盤空間
5. **遷移時間**：根據資料量大小，遷移可能需要幾分鐘

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

