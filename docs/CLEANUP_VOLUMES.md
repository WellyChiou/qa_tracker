# 清理舊 Volumes 指南

## 當前狀態

刪除後還剩兩個 volumes：
1. `docker-vue-java-mysql_mysql_data` - 206.1M（有資料）
2. `qa_tracker_mysql_data` - 0（空的）

## 建議

### 保留 `docker-vue-java-mysql_mysql_data`（推薦）

**原因：**
- 包含實際資料（206.1M）
- 作為備份保留，以防萬一
- 如果主機目錄出現問題，可以快速恢復

### 刪除 `qa_tracker_mysql_data`（推薦）

**原因：**
- 是空的（0 字節）
- 不再使用
- 可以釋放空間

## 檢查命令

執行以下命令確認 volumes 狀態：

```bash
# 檢查 docker-vue-java-mysql_mysql_data
docker run --rm -v docker-vue-java-mysql_mysql_data:/data alpine sh -c "du -sh /data && ls /data | head -5"

# 檢查 qa_tracker_mysql_data
docker run --rm -v qa_tracker_mysql_data:/data alpine sh -c "du -sh /data && ls /data | head -5"
```

## 清理步驟

### 步驟 1: 確認主機目錄資料正常

```bash
# 確認主機目錄有資料
du -sh /root/project/work/mysql
ls -la /root/project/work/mysql | head -10

# 確認 MySQL 正常運行
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;"
docker compose exec mysql mysql -u root -prootpassword qa_tracker -e "SELECT COUNT(*) FROM users;"
```

### 步驟 2: 刪除空的 volume

```bash
# 刪除空的 volume
docker volume rm qa_tracker_mysql_data
```

### 步驟 3: 決定是否保留 docker-vue-java-mysql_mysql_data

**選項 A: 保留作為備份（推薦）**
- 不執行任何操作
- 保留 volume 作為備份

**選項 B: 刪除以釋放空間**
```bash
# 確認資料已成功遷移後刪除
docker volume rm docker-vue-java-mysql_mysql_data
```

## 最終狀態

### 推薦的最終狀態

保留一個 volume 作為備份：
```bash
docker volume ls | grep mysql
# 應該只看到：
# local     docker-vue-java-mysql_mysql_data
```

### 完全清理（如果確定不需要備份）

```bash
# 刪除所有舊 volumes
docker volume rm docker-vue-java-mysql_mysql_data
docker volume rm qa_tracker_mysql_data

# 確認
docker volume ls | grep mysql
# 應該沒有輸出（所有 volumes 已刪除）
```

## 注意事項

1. **備份優先**：刪除前確保主機目錄資料完整
2. **測試運行**：刪除前讓系統運行一段時間，確認一切正常
3. **空間考慮**：`docker-vue-java-mysql_mysql_data` 佔用約 206MB，如果空間緊張可以刪除
4. **恢復能力**：保留 volume 可以在緊急情況下快速恢復

## 建議

**當前狀態（兩個 volumes）是合理的：**
- ✅ `docker-vue-java-mysql_mysql_data` - 保留作為備份
- ❌ `qa_tracker_mysql_data` - 可以刪除（空的）

**執行清理：**
```bash
# 只刪除空的 volume
docker volume rm qa_tracker_mysql_data

# 確認
docker volume ls | grep mysql
# 應該只看到 docker-vue-java-mysql_mysql_data
```

