# 強制刪除正在使用的 Volume

## 問題

當嘗試刪除 volume 時出現錯誤：
```
Error response from daemon: remove qa_tracker_mysql_data: volume is in use
```

這表示有容器正在使用這個 volume。

## 解決步驟

### 步驟 1: 找出使用該 volume 的容器

```bash
# 查找使用該 volume 的容器
docker ps -a --filter volume=qa_tracker_mysql_data

# 或者查看所有容器
docker ps -a
```

### 步驟 2: 停止並刪除相關容器

```bash
# 停止所有容器（如果還在運行）
docker compose down

# 或者只停止特定容器
docker stop <container_id>
docker rm <container_id>
```

### 步驟 3: 強制刪除 volume

```bash
# 方法 1: 先停止所有容器再刪除
docker compose down
docker volume rm qa_tracker_mysql_data

# 方法 2: 使用 --force 參數（如果支持）
docker volume rm --force qa_tracker_mysql_data

# 方法 3: 找出並刪除使用該 volume 的容器
docker ps -a --filter volume=qa_tracker_mysql_data --format "{{.ID}}" | xargs docker rm -f
docker volume rm qa_tracker_mysql_data
```

## 快速解決方案

執行以下命令：

```bash
# 1. 停止所有容器
docker compose down

# 2. 查找並刪除使用該 volume 的容器
docker ps -a --filter volume=qa_tracker_mysql_data --format "{{.ID}}" | xargs -r docker rm -f

# 3. 刪除 volume
docker volume rm qa_tracker_mysql_data

# 4. 重新啟動服務（如果需要）
docker compose up -d
```

## 檢查容器狀態

```bash
# 查看所有容器
docker ps -a

# 查看所有 volumes
docker volume ls

# 查看 volume 詳細信息（找出哪些容器在使用）
docker volume inspect qa_tracker_mysql_data
```

## 注意事項

1. **停止容器前確認**：確保沒有重要服務正在運行
2. **資料安全**：刪除前確認資料已遷移到主機目錄
3. **重新啟動**：刪除後可能需要重新啟動服務

