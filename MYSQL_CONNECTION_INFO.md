# 🔌 MySQL 連線資訊

## 📋 連線參數

### 基本資訊

| 項目 | 數值 |
|------|------|
| **主機地址** | `38.54.89.136` (您的虛擬主機 IP) |
| **端口** | `3306` |
| **資料庫名稱** | `qa_tracker` |
| **使用者名稱** | `appuser` |
| **密碼** | `apppassword` |
| **Root 使用者** | `root` |
| **Root 密碼** | `rootpassword` |

---

## 🛠️ 使用各種工具連線

### 1. MySQL Workbench（圖形化工具）

**連線設定：**
```
Connection Name: QA Tracker
Hostname: 38.54.89.136
Port: 3306
Username: appuser
Password: apppassword
Default Schema: qa_tracker
```

**步驟：**
1. 打開 MySQL Workbench
2. 點擊「+」新增連線
3. 填入上述資訊
4. 點擊「Test Connection」測試
5. 點擊「OK」儲存

---

### 2. DBeaver（通用資料庫工具）

**連線設定：**
```
Database: MySQL
Host: 38.54.89.136
Port: 3306
Database: qa_tracker
Username: appuser
Password: apppassword
```

**步驟：**
1. 打開 DBeaver
2. 新增連線 → 選擇 MySQL
3. 填入上述資訊
4. 測試連線
5. 完成

---

### 3. phpMyAdmin（網頁版）

如果您的虛擬主機有 phpMyAdmin：

```
伺服器: 38.54.89.136:3306
使用者名稱: appuser
密碼: apppassword
```

---

### 4. 命令列（MySQL Client）

**在本地電腦連線：**

```bash
# 如果已安裝 MySQL Client
mysql -h 38.54.89.136 -P 3306 -u appuser -papppassword qa_tracker

# 或使用 root（如果需要完整權限）
mysql -h 38.54.89.136 -P 3306 -u root -prootpassword qa_tracker
```

**在虛擬主機上連線：**

```bash
# SSH 到虛擬主機後
docker compose exec mysql mysql -u appuser -papppassword qa_tracker

# 或使用 root
docker compose exec mysql mysql -u root -prootpassword qa_tracker
```

---

### 5. VS Code 擴充功能

**使用 MySQL 擴充功能：**

1. 安裝擴充功能：`MySQL` (by Jun Han)
2. 新增連線：
   - Host: `38.54.89.136`
   - Port: `3306`
   - User: `appuser`
   - Password: `apppassword`
   - Database: `qa_tracker`

---

### 6. TablePlus（Mac/Windows）

**連線設定：**
```
Name: QA Tracker
Host: 38.54.89.136
Port: 3306
User: appuser
Password: apppassword
Database: qa_tracker
```

---

## 🔒 安全注意事項

### ⚠️ 重要提醒

1. **防火牆設定**
   - 預設 MySQL 端口 3306 已對外開放
   - **建議**：只允許特定 IP 訪問，或使用 SSH 隧道

2. **密碼強度**
   - 目前使用的是預設密碼
   - **建議**：在生產環境中更改為更強的密碼

3. **SSH 隧道（更安全）**

   如果不想直接暴露 3306 端口，可以使用 SSH 隧道：

   ```bash
   # 在本地電腦執行
   ssh -L 3307:localhost:3306 root@38.54.89.136
   
   # 然後在工具中連線到 localhost:3307
   ```

---

## 🔧 修改密碼（可選）

如果需要修改密碼：

### 修改 appuser 密碼

```bash
# SSH 到虛擬主機
ssh root@38.54.89.136

# 進入 MySQL 容器
docker compose exec mysql mysql -u root -prootpassword

# 在 MySQL 中執行
ALTER USER 'appuser'@'%' IDENTIFIED BY '新密碼';
FLUSH PRIVILEGES;

# 同時需要更新 docker-compose.yml 和 application.properties
```

### 修改 root 密碼

```bash
# 在 MySQL 中執行
ALTER USER 'root'@'%' IDENTIFIED BY '新密碼';
FLUSH PRIVILEGES;
```

---

## 📊 常用查詢

### 檢查連線

```sql
-- 檢查資料庫
SHOW DATABASES;

-- 使用資料庫
USE qa_tracker;

-- 檢查資料表
SHOW TABLES;

-- 檢查記錄數
SELECT COUNT(*) FROM records;
SELECT COUNT(*) FROM users;
```

### 查看資料

```sql
-- 查看前 10 筆記錄
SELECT * FROM records LIMIT 10;

-- 查看特定 Issue
SELECT * FROM records WHERE issue_number = 1234;

-- 查看執行中的記錄
SELECT * FROM records WHERE status = 1;
```

---

## 🐛 連線問題排除

### 問題 1: 無法連線

**可能原因：**
- 防火牆未開放 3306 端口
- MySQL 容器未運行
- 網路連線問題

**解決方案：**
```bash
# 檢查 MySQL 容器狀態
docker compose ps mysql

# 檢查端口是否開放
sudo ss -tulpn | grep 3306

# 檢查防火牆
sudo ufw status
```

### 問題 2: 認證失敗

**可能原因：**
- 使用者名稱或密碼錯誤
- 使用者沒有遠端連線權限

**解決方案：**
```bash
# 檢查使用者權限
docker compose exec mysql mysql -u root -prootpassword -e "SELECT user, host FROM mysql.user;"
```

### 問題 3: 找不到資料庫

**可能原因：**
- 資料庫名稱錯誤
- 資料庫未建立

**解決方案：**
```bash
# 檢查資料庫是否存在
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;"
```

---

## 📝 連線字串格式

### JDBC 連線字串（Java）

```
jdbc:mysql://38.54.89.136:3306/qa_tracker?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4
```

### 標準連線字串

```
mysql://appuser:apppassword@38.54.89.136:3306/qa_tracker
```

---

## ✅ 快速測試連線

### 使用 curl 測試（如果支援）

```bash
# 測試端口是否開放
telnet 38.54.89.136 3306

# 或使用 nc
nc -zv 38.54.89.136 3306
```

### 使用 MySQL Client 測試

```bash
mysql -h 38.54.89.136 -P 3306 -u appuser -papppassword -e "SELECT 1;"
```

如果成功，會顯示：
```
+---+
| 1 |
+---+
| 1 |
+---+
```

---

## 🎯 總結

**最常用的連線資訊：**

```
主機: 38.54.89.136
端口: 3306
使用者: appuser
密碼: apppassword
資料庫: qa_tracker
```

**Root 連線（如果需要完整權限）：**

```
主機: 38.54.89.136
端口: 3306
使用者: root
密碼: rootpassword
資料庫: qa_tracker
```

祝您使用順利！🚀

