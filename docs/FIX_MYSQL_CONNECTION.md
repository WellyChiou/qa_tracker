# MySQL 連接問題解決方案：Public Key Retrieval is not allowed

## 問題說明

當使用外部工具（MySQL Workbench、DBeaver、Navicat、DataGrip 等）連接到 MySQL 8.0 時，可能會遇到以下錯誤：
```
Public Key Retrieval is not allowed
```

這是因為 MySQL 8.0 默認使用 `caching_sha2_password` 認證插件，需要額外的連接參數。

## 解決方案

### 方案 1: 在連接字符串中添加參數（推薦）

在您的資料庫連接工具中，連接字符串需要包含以下參數：

```
jdbc:mysql://your-server-ip:3306/database_name?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&useUnicode=true
```

**各工具配置方法：**

#### MySQL Workbench
1. 創建新連接
2. 在 **Advanced** 標籤頁
3. 在 **Others** 欄位添加：
   ```
   allowPublicKeyRetrieval=true;useSSL=false
   ```

#### DBeaver
1. 創建新連接 → MySQL
2. 在 **Driver properties** 標籤頁
3. 添加以下屬性：
   - `allowPublicKeyRetrieval` = `true`
   - `useSSL` = `false`

#### Navicat
1. 創建新連接 → MySQL
2. 在 **高級** 標籤頁
3. 在 **連接參數** 中添加：
   ```
   allowPublicKeyRetrieval=true;useSSL=false
   ```

#### DataGrip / IntelliJ IDEA
1. 創建新 Data Source → MySQL
2. 在 **Advanced** 標籤頁
3. 添加以下 VM options：
   ```
   -DallowPublicKeyRetrieval=true
   ```
4. 或在 URL 中添加參數：
   ```
   jdbc:mysql://host:3306/db?allowPublicKeyRetrieval=true&useSSL=false
   ```

#### 命令行 mysql 客戶端
```bash
mysql -h your-server-ip -P 3306 -u appuser -p --ssl-mode=DISABLED --get-server-public-key
```

### 方案 2: 修改 MySQL 用戶認證方式（不推薦，但可以解決）

如果方案 1 無法解決，可以將用戶認證方式改為 `mysql_native_password`：

```bash
# 連接到 MySQL
docker compose exec mysql mysql -u root -prootpassword

# 修改 appuser 的認證方式
ALTER USER 'appuser'@'%' IDENTIFIED WITH mysql_native_password BY 'apppassword';
ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY 'rootpassword';
FLUSH PRIVILEGES;

# 驗證
SELECT user, host, plugin FROM mysql.user WHERE user = 'appuser';
```

**注意：** 這會降低安全性，不推薦在生產環境使用。

### 方案 3: 創建新的用戶使用 mysql_native_password（推薦替代方案）

如果不想修改現有用戶，可以創建一個專門用於外部工具連接的用戶：

```bash
# 連接到 MySQL
docker compose exec mysql mysql -u root -prootpassword

# 創建新用戶（使用 mysql_native_password）
CREATE USER 'appuser_tool'@'%' IDENTIFIED WITH mysql_native_password BY 'your_password_here';
GRANT ALL PRIVILEGES ON qa_tracker.* TO 'appuser_tool'@'%';
GRANT ALL PRIVILEGES ON church.* TO 'appuser_tool'@'%';
FLUSH PRIVILEGES;
```

然後在外部工具中使用 `appuser_tool` 用戶連接。

## 連接參數說明

### 必須的參數

- `allowPublicKeyRetrieval=true` - 允許客戶端從服務器獲取公鑰（解決 Public Key Retrieval 錯誤）
- `useSSL=false` - 禁用 SSL（因為我們使用內部網絡，不需要 SSL）

### 可選但推薦的參數

- `characterEncoding=utf8` - 字符編碼
- `useUnicode=true` - 使用 Unicode
- `serverTimezone=Asia/Taipei` - 時區設置（如果需要）

## 完整連接字符串示例

```
jdbc:mysql://your-server-ip:3306/qa_tracker?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&useUnicode=true&serverTimezone=Asia/Taipei
```

## 驗證連接

使用命令行測試連接：

```bash
# 測試連接（從服務器外部）
mysql -h your-server-ip -P 3306 -u appuser -papppassword --ssl-mode=DISABLED --get-server-public-key -e "SHOW DATABASES;"

# 測試連接（從 Docker 容器內）
docker compose exec mysql mysql -u appuser -papppassword -e "SHOW DATABASES;"
```

## 常見問題

### Q: 為什麼重新掛載後會出現這個問題？

A: 重新掛載 MySQL 資料目錄後，MySQL 會重新初始化，可能會重新生成 SSL 證書或重置某些認證設置。這導致客戶端需要重新配置連接參數。

### Q: 後端應用為什麼沒有這個問題？

A: 後端應用的連接字符串已經包含了 `allowPublicKeyRetrieval=true` 參數（在 `application.properties` 和 `docker-compose.yml` 中），所以不會出現這個問題。

### Q: 使用 SSL 連接可以嗎？

A: 可以，但需要：
1. 在連接字符串中設置 `useSSL=true`
2. 配置 SSL 證書
3. 確保客戶端工具支持 SSL

對於內部網絡連接，通常不需要 SSL。

## 快速修復腳本

如果需要批量修改用戶認證方式：

```bash
#!/bin/bash
# 修改所有用戶為 mysql_native_password

docker compose exec mysql mysql -u root -prootpassword <<EOF
ALTER USER 'appuser'@'%' IDENTIFIED WITH mysql_native_password BY 'apppassword';
FLUSH PRIVILEGES;
SELECT user, host, plugin FROM mysql.user WHERE user IN ('appuser', 'root');
EOF
```

**警告：** 執行前請確認這不會影響您的安全策略。

