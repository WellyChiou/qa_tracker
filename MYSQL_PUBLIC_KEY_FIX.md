# 🔑 MySQL "Public Key Retrieval is not allowed" 錯誤解決方案

## ❓ 什麼是這個錯誤？

**錯誤訊息：**
```
Public Key Retrieval is not allowed
```

這是 MySQL 8.0 的安全機制。當使用 `caching_sha2_password` 認證方式時，某些客戶端工具需要額外的設定才能連線。

---

## 🔍 原因說明

### MySQL 8.0 的認證方式

MySQL 8.0 預設使用 `caching_sha2_password` 認證方式，這比舊版的 `mysql_native_password` 更安全，但需要：

1. **SSL 連線**，或
2. **允許 Public Key Retrieval**（用於非 SSL 連線）

某些工具（如 MySQL Workbench、DBeaver）預設不允許 Public Key Retrieval，因此會出現這個錯誤。

---

## ✅ 解決方案

### 方法 1: 在連線字串中添加參數（推薦）

在連線字串中添加 `allowPublicKeyRetrieval=true`：

```
jdbc:mysql://38.54.89.136:3306/qa_tracker?allowPublicKeyRetrieval=true&useSSL=false
```

**各種工具的設定方式：**

#### MySQL Workbench

1. 建立連線時，點擊「Advanced」
2. 在「Others」標籤中，添加：
   ```
   allowPublicKeyRetrieval=true
   ```

#### DBeaver

1. 編輯連線設定
2. 在「Driver properties」中添加：
   - Key: `allowPublicKeyRetrieval`
   - Value: `true`

#### TablePlus

1. 編輯連線
2. 在「Advanced」中添加參數：
   ```
   allowPublicKeyRetrieval=true
   ```

#### VS Code MySQL 擴充功能

在連線設定中添加：
```json
{
  "host": "38.54.89.136",
  "port": 3306,
  "user": "appuser",
  "password": "apppassword",
  "database": "qa_tracker",
  "allowPublicKeyRetrieval": true
}
```

---

### 方法 2: 修改 MySQL 認證方式（永久解決）

如果方法 1 不行，可以將使用者改為使用舊版認證方式：

#### 修改 appuser 使用者

```bash
# SSH 到虛擬主機
ssh root@38.54.89.136

# 進入 MySQL 容器
docker compose exec mysql mysql -u root -prootpassword

# 在 MySQL 中執行
ALTER USER 'appuser'@'%' IDENTIFIED WITH mysql_native_password BY 'apppassword';
FLUSH PRIVILEGES;
```

#### 修改 root 使用者

```bash
# SSH 到虛擬主機
ssh root@38.54.89.136

# 進入 MySQL 容器
docker compose exec mysql mysql -u root -prootpassword

# 在 MySQL 中執行
ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY 'rootpassword';
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'rootpassword';
FLUSH PRIVILEGES;
```

**注意：** 修改 root 後，需要重新連線才能生效。

#### 修改所有使用者（一次性解決）

如果想一次修改所有使用者：

```sql
-- 在 MySQL 中執行
ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY 'rootpassword';
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'rootpassword';
ALTER USER 'appuser'@'%' IDENTIFIED WITH mysql_native_password BY 'apppassword';
FLUSH PRIVILEGES;
```

這樣就不需要 `allowPublicKeyRetrieval` 參數了。

---

### 方法 3: 使用 SSL 連線（最安全）

如果工具支援 SSL，可以啟用 SSL 連線：

```
jdbc:mysql://38.54.89.136:3306/qa_tracker?useSSL=true&requireSSL=true
```

---

## 🛠️ 各工具詳細設定

### MySQL Workbench

**步驟：**
1. 開啟 MySQL Workbench
2. 點擊「+」新增連線
3. 填入基本資訊：
   - Connection Name: `QA Tracker`
   - Hostname: `38.54.89.136`
   - Port: `3306`
   - Username: `appuser`
   - Password: `apppassword`
   - Default Schema: `qa_tracker`

4. 點擊「Advanced」標籤
5. 在「Others」欄位中添加：
   ```
   allowPublicKeyRetrieval=true
   ```

6. 點擊「Test Connection」測試
7. 如果成功，點擊「OK」儲存

---

### DBeaver

**步驟：**
1. 開啟 DBeaver
2. 新增連線 → 選擇 MySQL
3. 填入基本資訊：
   - Host: `38.54.89.136`
   - Port: `3306`
   - Database: `qa_tracker`
   - Username: `appuser`
   - Password: `apppassword`

4. 點擊「Driver properties」標籤
5. 點擊「Add Property」
6. 添加：
   - Property name: `allowPublicKeyRetrieval`
   - Property value: `true`

7. 點擊「Test Connection」
8. 如果成功，點擊「Finish」

---

### TablePlus

**步驟：**
1. 開啟 TablePlus
2. 新增連線 → MySQL
3. 填入基本資訊
4. 點擊「Advanced」標籤
5. 在「Parameters」中添加：
   ```
   allowPublicKeyRetrieval=true
   ```

6. 測試連線

---

### phpMyAdmin

如果使用 phpMyAdmin，通常不需要額外設定，因為它會自動處理。

---

### 命令列 (mysql client)

```bash
# 使用參數連線
mysql -h 38.54.89.136 -P 3306 -u appuser -papppassword \
  --default-auth=mysql_native_password qa_tracker

# 或使用連線字串
mysql -h 38.54.89.136 -P 3306 -u appuser -papppassword \
  --ssl-mode=DISABLED --allow-public-key-retrieval qa_tracker
```

---

## 🔒 安全性說明

### `allowPublicKeyRetrieval=true` 安全嗎？

**短期使用：** ✅ 可以接受
- 用於開發和測試環境
- 如果使用私有網路或 VPN

**生產環境：** ⚠️ 建議使用 SSL
- 啟用 SSL 連線更安全
- 或使用 SSH 隧道

### 最佳實踐

1. **開發環境**：使用 `allowPublicKeyRetrieval=true` 即可
2. **生產環境**：使用 SSL 或 SSH 隧道
3. **長期方案**：考慮改用 `mysql_native_password`（方法 2）

---

## 🐛 其他相關錯誤

### 錯誤 1: "Access denied for user"

如果同時出現這個錯誤，可能是：
- 使用者名稱或密碼錯誤
- 使用者沒有遠端連線權限

**解決方案：**
```sql
-- 檢查使用者權限
SELECT user, host FROM mysql.user WHERE user = 'appuser';

-- 如果沒有 '%' 權限，需要添加
GRANT ALL PRIVILEGES ON qa_tracker.* TO 'appuser'@'%';
FLUSH PRIVILEGES;
```

### 錯誤 2: "Can't connect to MySQL server"

可能是：
- 防火牆未開放 3306 端口
- MySQL 容器未運行

**解決方案：**
```bash
# 檢查容器狀態
docker compose ps mysql

# 檢查端口
sudo ss -tulpn | grep 3306
```

---

## 📝 完整連線字串範例

### JDBC (Java)
```
jdbc:mysql://38.54.89.136:3306/qa_tracker?allowPublicKeyRetrieval=true&useSSL=false&characterEncoding=utf8mb4
```

### 標準 MySQL
```
mysql://appuser:apppassword@38.54.89.136:3306/qa_tracker?allowPublicKeyRetrieval=true
```

### Python (mysql-connector-python)
```python
import mysql.connector

config = {
    'host': '38.54.89.136',
    'port': 3306,
    'user': 'appuser',
    'password': 'apppassword',
    'database': 'qa_tracker',
    'allow_public_key_retrieval': True
}

conn = mysql.connector.connect(**config)
```

---

## ✅ 快速檢查清單

遇到 "Public Key Retrieval is not allowed" 時：

- [ ] 在連線設定中添加 `allowPublicKeyRetrieval=true`
- [ ] 確認使用者名稱和密碼正確
- [ ] 確認 MySQL 容器正在運行
- [ ] 確認防火牆已開放 3306 端口
- [ ] 如果還是不行，嘗試方法 2（修改認證方式）

---

## 🎯 總結

**最簡單的解決方法：**

在您的連線工具中添加參數：
```
allowPublicKeyRetrieval=true
```

這樣就可以正常連線了！

如果還有問題，請告訴我您使用的是哪個工具，我可以提供更詳細的設定步驟。

