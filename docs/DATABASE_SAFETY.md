# 資料庫資料安全說明

## ✅ 資料不會被刪除

`deploy.sh` 腳本**不會刪除資料庫資料**，原因如下：

### 1. Docker Volume 機制

資料庫資料保存在 **Docker Volume** (`mysql_data`) 中，而不是容器內：

```yaml
volumes:
  - mysql_data:/var/lib/mysql  # 資料保存在 volume 中
```

### 2. 腳本中的操作

腳本只執行以下操作，**都不會刪除 volume**：

#### ✅ 安全的操作（不會刪除資料）

1. **`docker compose down`**（不含 `-v`）
   - 只停止和刪除容器
   - **保留所有 volumes**
   - ✅ 資料安全

2. **`docker rm -f mysql_db`**
   - 只刪除容器
   - **不影響 volume**
   - ✅ 資料安全

#### ❌ 危險的操作（會刪除資料）

1. **`docker compose down -v`**
   - 停止容器 + **刪除 volumes**
   - ❌ 會刪除資料
   - ⚠️ 腳本中**沒有使用**

2. **`docker volume rm mysql_data`**
   - 直接刪除 volume
   - ❌ 會刪除資料
   - ⚠️ 腳本中**沒有使用**

## 🔒 資料保護機制

### Volume 持久化

即使容器被刪除，volume 中的資料仍然存在：

```bash
# 容器被刪除
docker rm -f mysql_db

# 但 volume 還在
docker volume ls
# 會看到 mysql_data

# 重新創建容器時，資料會自動恢復
docker compose up -d
```

### 驗證資料安全

可以隨時檢查 volume 是否存在：

```bash
# 查看所有 volumes
docker volume ls

# 應該會看到
# docker-vue-java-mysql_mysql_data
```

## 📋 腳本操作說明

### deploy.sh 執行的步驟

1. ✅ `docker compose down` - 停止容器（保留 volume）
2. ✅ `docker rm -f` - 刪除容器（不影響 volume）
3. ✅ `docker compose up -d --build` - 重新創建容器（使用現有 volume）

### 資料流向

```
舊容器 (mysql_db) 
    ↓ 停止並刪除
Volume (mysql_data) ← 資料保留在這裡 ✅
    ↓ 重新創建容器
新容器 (mysql_db) 
    ↓ 掛載 volume
資料自動恢復 ✅
```

## ⚠️ 如果真的需要清空資料

只有在需要**完全重置**時才使用：

```bash
# ⚠️ 警告：這會清空所有資料庫資料！
docker compose down -v
```

## ✅ 總結

- ✅ **`deploy.sh` 不會刪除資料庫資料**
- ✅ **資料保存在 Docker Volume 中，持久化存儲**
- ✅ **即使容器被刪除，資料也會保留**
- ✅ **重新部署時，資料會自動恢復**

可以放心使用 `deploy.sh` 進行部署和更新！

