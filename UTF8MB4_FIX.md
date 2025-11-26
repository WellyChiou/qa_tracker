# 🔧 UTF8MB4 編碼錯誤修復

## ❓ 錯誤說明

**錯誤訊息：**
```
Unsupported character encoding 'utf8mb4'
java.io.UnsupportedEncodingException: utf8mb4
```

**原因：**
- JDBC 連線字串中使用了 `characterEncoding=utf8mb4`
- Java JDBC 驅動不支援 `utf8mb4` 作為 characterEncoding 參數
- 應該使用 `utf8` 或移除此參數（MySQL 8.0 預設就是 utf8mb4）

---

## ✅ 已修正的檔案

### 1. application.properties

**修改前：**
```properties
spring.datasource.url=...&characterEncoding=utf8mb4
```

**修改後：**
```properties
spring.datasource.url=...&characterEncoding=utf8&useUnicode=true
```

### 2. docker-compose.yml

**修改前：**
```yaml
SPRING_DATASOURCE_URL: ...&characterEncoding=utf8mb4
```

**修改後：**
```yaml
SPRING_DATASOURCE_URL: ...&characterEncoding=utf8&useUnicode=true
```

### 3. Hibernate Dialect

**修改前：**
```properties
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

**修改後：**
```properties
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

---

## 🚀 重新啟動服務

在虛擬主機上執行：

```bash
# 停止後端
docker compose stop backend

# 重新構建並啟動
docker compose up -d --build backend

# 查看日誌確認是否成功
docker compose logs -f backend
```

應該會看到：
```
Started HelloWorldApplication in X.XXX seconds
```

---

## 🔍 驗證修復

### 檢查後端是否正常啟動

```bash
# 查看容器狀態
docker compose ps backend

# 應該顯示 Up 狀態

# 測試 API
curl http://localhost:8080/api/records/stats/in-progress
```

### 檢查資料庫連線

後端日誌中不應該再出現 `Unsupported character encoding` 錯誤。

---

## 📝 技術說明

### 為什麼不能用 utf8mb4？

- Java JDBC 驅動的 `characterEncoding` 參數只支援標準的 Java 字符集名稱
- `utf8mb4` 是 MySQL 特有的字符集名稱，不是標準 Java 字符集
- 使用 `utf8` + `useUnicode=true` 可以達到相同效果

### 資料庫字符集

雖然連線字串使用 `utf8`，但資料庫本身仍然使用 `utf8mb4`：
- 資料庫建立時使用：`CHARACTER SET utf8mb4`
- 資料表建立時使用：`CHARSET=utf8mb4`
- 這不會影響資料儲存，資料仍然以 utf8mb4 儲存

---

## ✅ 修復完成

重新啟動後端後，應該可以正常連線資料庫了！

