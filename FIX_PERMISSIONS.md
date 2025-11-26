# 🔐 修復 MySQL 權限問題

## ❓ 錯誤說明

**錯誤訊息：**
```
Access denied for user 'appuser'@'%' to database 'qa_tracker'
```

**原因：**
- `appuser` 使用者沒有權限訪問 `qa_tracker` 資料庫
- 需要授予權限

---

## ✅ 解決方案

### 方法 1: 使用 SQL 腳本（推薦）

在虛擬主機上執行：

```bash
# SSH 到虛擬主機
ssh root@38.54.89.136

# 進入專案目錄
cd /root/project/work/docker-vue-java-mysql

# 執行權限授予腳本
docker compose exec mysql mysql -u root -prootpassword < mysql/grant-permissions.sql
```

### 方法 2: 手動執行 SQL

```bash
# 進入 MySQL
docker compose exec mysql mysql -u root -prootpassword

# 在 MySQL 中執行
GRANT ALL PRIVILEGES ON qa_tracker.* TO 'appuser'@'%';
FLUSH PRIVILEGES;

# 驗證權限
SHOW GRANTS FOR 'appuser'@'%';

# 退出
EXIT;
```

### 方法 3: 一行命令

```bash
docker compose exec mysql mysql -u root -prootpassword -e "GRANT ALL PRIVILEGES ON qa_tracker.* TO 'appuser'@'%'; FLUSH PRIVILEGES;"
```

---

## 🔍 驗證修復

### 檢查權限

```bash
# 查看 appuser 的權限
docker compose exec mysql mysql -u root -prootpassword -e "SHOW GRANTS FOR 'appuser'@'%';"
```

應該會看到：
```
GRANT USAGE ON *.* TO `appuser`@`%`
GRANT ALL PRIVILEGES ON `qa_tracker`.* TO `appuser`@`%`
```

### 測試連線

```bash
# 使用 appuser 測試連線
docker compose exec mysql mysql -u appuser -papppassword qa_tracker -e "SELECT 1;"
```

如果成功，會顯示結果。

### 重新啟動後端

```bash
# 重新啟動後端
docker compose restart backend

# 查看日誌確認
docker compose logs -f backend
```

應該會看到：
```
Started HelloWorldApplication in X.XXX seconds
```

---

## 🎯 完整修復流程

```bash
# 1. 授予權限
docker compose exec mysql mysql -u root -prootpassword -e "GRANT ALL PRIVILEGES ON qa_tracker.* TO 'appuser'@'%'; FLUSH PRIVILEGES;"

# 2. 驗證權限
docker compose exec mysql mysql -u root -prootpassword -e "SHOW GRANTS FOR 'appuser'@'%';"

# 3. 測試連線
docker compose exec mysql mysql -u appuser -papppassword qa_tracker -e "SELECT 1;"

# 4. 重新啟動後端
docker compose restart backend

# 5. 查看日誌
docker compose logs -f backend
```

---

## 📝 如果還是不行

### 檢查使用者是否存在

```bash
docker compose exec mysql mysql -u root -prootpassword -e "SELECT user, host FROM mysql.user WHERE user = 'appuser';"
```

### 重新建立使用者（如果需要）

```bash
# 刪除舊使用者
docker compose exec mysql mysql -u root -prootpassword -e "DROP USER IF EXISTS 'appuser'@'%';"

# 建立新使用者
docker compose exec mysql mysql -u root -prootpassword -e "CREATE USER 'appuser'@'%' IDENTIFIED BY 'apppassword';"

# 授予權限
docker compose exec mysql mysql -u root -prootpassword -e "GRANT ALL PRIVILEGES ON qa_tracker.* TO 'appuser'@'%'; FLUSH PRIVILEGES;"
```

---

## ✅ 完成

授予權限後，後端應該可以正常連線資料庫了！

