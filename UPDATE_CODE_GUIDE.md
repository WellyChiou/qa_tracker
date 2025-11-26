# 🔄 更新程式碼指南

## ❓ 更新程式碼需要先執行 `docker compose down` 嗎？

### ✅ **答案：不需要！**

更新程式碼時，**不需要**先執行 `docker compose down`。

---

## 🎯 正確的更新流程

### 方法 1: 直接重新構建（推薦）

```bash
# 直接重新構建並啟動（最簡單）
docker compose up -d --build
```

**這會：**
- ✅ 重新構建修改過的容器
- ✅ 自動停止舊容器
- ✅ 啟動新容器
- ✅ **不會清空資料庫資料**

### 方法 2: 只重新構建特定服務

```bash
# 只重新構建後端
docker compose up -d --build backend

# 只重新構建前端
docker compose up -d --build frontend
```

---

## ⚠️ `docker compose down` 的說明

### `docker compose down` **不會清空資料**

```bash
# ✅ 安全：只停止容器，保留資料
docker compose down

# ⚠️ 危險：停止容器並刪除 volumes（會清空資料）
docker compose down -v
```

**重要區別：**
- `docker compose down` → 只停止容器，**資料保留**
- `docker compose down -v` → 停止容器並刪除 volumes，**資料會清空**

---

## 📋 什麼時候需要 `docker compose down`？

### 需要 `down` 的情況

1. **修改了 docker-compose.yml 的配置**
   ```bash
   # 例如：修改了端口映射、環境變數等
   docker compose down
   docker compose up -d --build
   ```

2. **需要完全重新啟動所有服務**
   ```bash
   # 當服務出現奇怪問題時
   docker compose down
   docker compose up -d
   ```

3. **修改了網路配置**
   ```bash
   docker compose down
   docker compose up -d
   ```

### 不需要 `down` 的情況

1. **只修改了程式碼**（Java、HTML、JS）
   ```bash
   # 直接重新構建即可
   docker compose up -d --build backend
   ```

2. **只修改了配置檔案**（application.properties）
   ```bash
   # 直接重新構建即可
   docker compose up -d --build backend
   ```

3. **只修改了 Dockerfile**
   ```bash
   # 直接重新構建即可
   docker compose up -d --build
   ```

---

## 🔄 完整的更新流程

### 場景 1: 修改了 Java 程式碼

```bash
# 1. 修改程式碼（例如 RecordController.java）

# 2. 重新構建後端（不需要 down）
docker compose up -d --build backend

# 3. 查看日誌確認
docker compose logs -f backend
```

### 場景 2: 修改了前端 HTML/JS

```bash
# 1. 修改程式碼（例如 tracker.html）

# 2. 重新構建前端（不需要 down）
docker compose up -d --build frontend

# 3. 刷新瀏覽器測試
```

### 場景 3: 修改了 docker-compose.yml

```bash
# 1. 修改 docker-compose.yml

# 2. 需要先停止（但不會清空資料）
docker compose down

# 3. 重新啟動
docker compose up -d --build
```

### 場景 4: 修改了資料庫結構（schema.sql）

```bash
# 1. 修改 schema.sql

# 2. 如果資料庫已存在，需要手動執行 SQL
# 或重新建立資料庫（會清空資料，需先備份）

# 3. 如果資料庫不存在，直接啟動即可
docker compose up -d --build
```

---

## 🛡️ 資料保護確認

### 確認資料不會被清空

```bash
# 檢查 volume 是否存在
docker volume ls | grep mysql_data

# 應該會看到：
# docker-vue-java-mysql_mysql_data
```

只要這個 volume 存在，資料就安全。

### 測試：執行 down 後資料是否還在

```bash
# 1. 先檢查資料
docker compose exec mysql mysql -u appuser -papppassword qa_tracker -e "SELECT COUNT(*) FROM records;"

# 2. 執行 down（不加 -v）
docker compose down

# 3. 重新啟動
docker compose up -d

# 4. 再次檢查資料（應該還是一樣的數量）
docker compose exec mysql mysql -u appuser -papppassword qa_tracker -e "SELECT COUNT(*) FROM records;"
```

---

## 📝 快速參考

### ✅ 安全操作（不會清空資料）

```bash
# 更新程式碼
docker compose up -d --build

# 重啟服務
docker compose restart

# 停止服務（保留資料）
docker compose down

# 停止後重新啟動
docker compose down
docker compose up -d
```

### ⚠️ 危險操作（會清空資料）

```bash
# 停止並刪除 volumes（會清空資料！）
docker compose down -v

# 手動刪除 volume（會清空資料！）
docker volume rm docker-vue-java-mysql_mysql_data
```

---

## 🎯 實際使用範例

### 日常開發流程

```bash
# 1. 修改程式碼
# （編輯 Java 檔案、HTML 檔案等）

# 2. 重新構建（不需要 down）
docker compose up -d --build backend

# 3. 查看日誌
docker compose logs -f backend

# 4. 測試功能
curl http://localhost:8080/api/records
```

### 如果需要完全重新啟動

```bash
# 1. 停止所有服務（保留資料）
docker compose down

# 2. 重新啟動
docker compose up -d --build

# 3. 驗證資料還在
docker compose exec mysql mysql -u appuser -papppassword qa_tracker -e "SELECT COUNT(*) FROM records;"
```

---

## 💡 最佳實踐

### 推薦流程

1. **修改程式碼後：**
   ```bash
   docker compose up -d --build [service-name]
   ```

2. **修改配置後：**
   ```bash
   docker compose up -d --build
   ```

3. **遇到奇怪問題時：**
   ```bash
   docker compose down
   docker compose up -d --build
   ```

4. **需要清空資料時（謹慎！）：**
   ```bash
   # 先備份！
   docker compose exec mysql mysqldump -u appuser -papppassword qa_tracker > backup.sql
   
   # 然後才清空
   docker compose down -v
   docker compose up -d --build
   ```

---

## ✅ 總結

### 更新程式碼時：

- ✅ **不需要** `docker compose down`
- ✅ 直接執行 `docker compose up -d --build` 即可
- ✅ 資料不會被清空（只要不加 `-v` 參數）

### 記住：

- `docker compose down` → 安全，只停止容器
- `docker compose down -v` → 危險，會刪除資料

---

**結論：更新程式碼時，直接執行 `docker compose up -d --build` 即可，不需要先執行 `down`！** 🚀

