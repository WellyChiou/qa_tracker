# MySQL 啟動問題診斷與修復

## 問題診斷

### 步驟 1: 檢查容器狀態

```bash
# 查看 MySQL 容器狀態
docker compose ps mysql

# 查看 MySQL 容器日誌
docker compose logs mysql

# 查看最近的錯誤日誌
docker compose logs mysql --tail 50
```

### 步驟 2: 檢查目錄和權限

```bash
# 檢查目錄是否存在
ls -la /root/project/work/mysql

# 檢查目錄權限
stat /root/project/work/mysql
```

## 常見問題與解決方案

### 問題 1: 目錄不存在

**症狀：** 容器無法啟動，日誌顯示 "No such file or directory"

**解決方案：**

```bash
# 創建目錄
mkdir -p /root/project/work/mysql

# 設置權限（MySQL 容器內的 mysql 用戶 UID 通常是 999）
chown -R 999:999 /root/project/work/mysql
chmod 755 /root/project/work/mysql

# 重新啟動
docker compose up -d mysql
```

### 問題 2: 權限不足

**症狀：** 容器啟動後立即退出，日誌顯示 "Permission denied"

**解決方案：**

```bash
# 設置正確的權限
chown -R 999:999 /root/project/work/mysql
chmod 755 /root/project/work/mysql

# 如果目錄已存在但權限不對，可能需要先停止容器
docker compose down
chown -R 999:999 /root/project/work/mysql
chmod 755 /root/project/work/mysql
docker compose up -d mysql
```

### 問題 3: 目錄為空，MySQL 需要初始化

**症狀：** 目錄存在但為空，MySQL 無法找到資料庫文件

**解決方案 A: 如果這是新安裝（可以重新初始化）**

```bash
# 確保目錄存在且權限正確
mkdir -p /root/project/work/mysql
chown -R 999:999 /root/project/work/mysql
chmod 755 /root/project/work/mysql

# 啟動服務（MySQL 會自動初始化）
docker compose up -d mysql

# 等待 MySQL 初始化完成（約 30 秒）
sleep 30

# 檢查狀態
docker compose logs mysql
```

**解決方案 B: 如果已有資料需要遷移**

```bash
# 1. 停止服務
docker compose down

# 2. 遷移資料（從舊的 volume）
docker run --rm \
  -v qa_tracker_mysql_data:/source:ro \
  -v /root/project/work/mysql:/target \
  alpine sh -c "cp -a /source/. /target/"

# 3. 設置權限
chown -R 999:999 /root/project/work/mysql
chmod 755 /root/project/work/mysql

# 4. 啟動服務
docker compose up -d mysql
```

### 問題 4: SELinux 問題（如果啟用了 SELinux）

**症狀：** 權限看起來正確，但容器仍無法寫入

**解決方案：**

```bash
# 設置 SELinux 上下文
chcon -Rt svirt_sandbox_file_t /root/project/work/mysql

# 或禁用 SELinux 對該目錄的限制（不推薦，但可以臨時解決）
setsebool -P container_manage_cgroup on
```

## 完整修復流程（推薦）

如果以上方法都不行，執行以下完整流程：

```bash
# 1. 停止所有服務
docker compose down

# 2. 檢查並創建目錄
mkdir -p /root/project/work/mysql

# 3. 如果目錄不為空，先備份
if [ "$(ls -A /root/project/work/mysql)" ]; then
    echo "目錄不為空，備份中..."
    tar -czf /root/project/work/mysql_backup_$(date +%Y%m%d_%H%M%S).tar.gz -C /root/project/work mysql
fi

# 4. 設置權限
chown -R 999:999 /root/project/work/mysql
chmod 755 /root/project/work/mysql

# 5. 如果目錄為空，需要遷移資料
if [ ! "$(ls -A /root/project/work/mysql)" ]; then
    echo "目錄為空，開始遷移資料..."
    docker run --rm \
      -v qa_tracker_mysql_data:/source:ro \
      -v /root/project/work/mysql:/target \
      alpine sh -c "cp -a /source/. /target/"
    
    # 再次設置權限（遷移後）
    chown -R 999:999 /root/project/work/mysql
fi

# 6. 啟動服務
docker compose up -d mysql

# 7. 等待啟動完成
echo "等待 MySQL 啟動..."
sleep 30

# 8. 檢查狀態
docker compose ps mysql
docker compose logs mysql --tail 20

# 9. 驗證資料庫
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;"
```

## 快速診斷命令

執行以下命令快速診斷：

```bash
echo "=== 容器狀態 ==="
docker compose ps mysql

echo -e "\n=== 目錄檢查 ==="
ls -la /root/project/work/ | grep mysql

echo -e "\n=== 權限檢查 ==="
stat /root/project/work/mysql 2>/dev/null || echo "目錄不存在"

echo -e "\n=== 最近日誌 ==="
docker compose logs mysql --tail 20
```

## 如果仍然無法啟動

如果以上方法都無法解決，請提供以下信息：

1. `docker compose logs mysql` 的完整輸出
2. `docker compose ps mysql` 的輸出
3. `ls -la /root/project/work/mysql` 的輸出（如果目錄存在）
4. `stat /root/project/work/mysql` 的輸出

然後可以考慮：
- 從備份恢復（使用之前備份的 SQL 文件）
- 臨時恢復使用 named volume 進行診斷

