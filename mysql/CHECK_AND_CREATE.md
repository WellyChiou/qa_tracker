# 📊 檢查和建立資料庫/資料表

## 🔍 先檢查是否已存在

### 在 IDE 中執行以下 SQL 檢查：

```sql
-- 1. 檢查資料庫是否存在
SHOW DATABASES;

-- 應該會看到 qa_tracker 資料庫
```

如果看到 `qa_tracker`，資料庫已存在。如果沒有，需要建立。

```sql
-- 2. 使用資料庫
USE qa_tracker;

-- 3. 檢查資料表是否存在
SHOW TABLES;

-- 應該會看到：
-- - users
-- - records
-- - config
```

---

## ✅ 如果資料庫和資料表已存在

恭喜！您可以直接開始使用了。

可以檢查一下資料表結構：

```sql
-- 查看 records 表結構
DESCRIBE records;

-- 查看 users 表結構
DESCRIBE users;

-- 查看 config 表結構
DESCRIBE config;
```

---

## 🆕 如果需要建立資料庫和資料表

### 方法 1: 在 IDE 中直接執行 SQL（推薦）

1. 打開 `mysql/schema.sql` 檔案
2. 複製全部內容
3. 在 IDE 的 SQL 編輯器中貼上
4. 執行整個腳本

### 方法 2: 使用命令列執行

```bash
# SSH 到虛擬主機
ssh root@38.54.89.136

# 執行 SQL 腳本
cd /root/project/work/docker-vue-java-mysql
docker compose exec -T mysql mysql -u root -prootpassword < mysql/schema.sql
```

### 方法 3: 在 IDE 中逐步執行

如果方法 1 有問題，可以逐步執行：

#### 步驟 1: 建立資料庫

```sql
CREATE DATABASE IF NOT EXISTS qa_tracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE qa_tracker;
```

#### 步驟 2: 建立 users 表

```sql
CREATE TABLE IF NOT EXISTS users (
    uid VARCHAR(128) PRIMARY KEY COMMENT 'Firebase UID',
    email VARCHAR(255) COMMENT '電子郵件',
    display_name VARCHAR(255) COMMENT '顯示名稱',
    photo_url TEXT COMMENT '頭像 URL',
    provider_id VARCHAR(50) COMMENT '登入提供者',
    last_login_at DATETIME COMMENT '最後登入時間',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='使用者表';
```

#### 步驟 3: 建立 records 表

```sql
CREATE TABLE IF NOT EXISTS records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    firebase_id VARCHAR(128) UNIQUE COMMENT 'Firebase 原始 ID（遷移用）',
    issue_number INT COMMENT 'Issue 編號',
    issue_link TEXT COMMENT 'Issue 連結',
    status TINYINT DEFAULT 1 COMMENT '狀態：0=執行中止, 1=執行中, 2=完成',
    category TINYINT COMMENT '類型：1=BUG, 2=改善, 3=優化, 4=模組, 5=QA',
    feature TEXT COMMENT '功能描述',
    memo TEXT COMMENT '備註',
    test_plan VARCHAR(1) DEFAULT '0' COMMENT 'Test Plan：0=否, 1=是',
    bug_found TINYINT DEFAULT 0 COMMENT '發現 BUG：0=否, 1=是',
    optimization_points INT DEFAULT 0 COMMENT '可優化項目數',
    verify_failed TINYINT DEFAULT 0 COMMENT '驗證失敗：0=否, 1=是',
    test_cases INT DEFAULT 0 COMMENT '測試案例數',
    file_count INT DEFAULT 0 COMMENT '檔案數量',
    test_start_date DATE COMMENT '開始測試日期',
    eta_date DATE COMMENT '預計交付日期',
    completed_at DATE COMMENT '完成日期',
    created_by_uid VARCHAR(128) COMMENT '建立者 UID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_by_uid VARCHAR(128) COMMENT '更新者 UID',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_issue_number (issue_number),
    INDEX idx_status (status),
    INDEX idx_category (category),
    INDEX idx_created_by (created_by_uid),
    INDEX idx_created_at (created_at),
    INDEX idx_test_start_date (test_start_date),
    INDEX idx_completed_at (completed_at),
    FULLTEXT idx_feature_memo (feature, memo),
    FOREIGN KEY (created_by_uid) REFERENCES users(uid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作記錄表';
```

#### 步驟 4: 建立 config 表

```sql
CREATE TABLE IF NOT EXISTS config (
    config_key VARCHAR(100) PRIMARY KEY COMMENT '設定鍵',
    config_value TEXT COMMENT '設定值（JSON 格式）',
    description VARCHAR(255) COMMENT '說明',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系統設定表';

-- 插入預設設定
INSERT INTO config (config_key, config_value, description) 
VALUES ('gitlab_token', '', 'GitLab API Token')
ON DUPLICATE KEY UPDATE config_value = config_value;
```

---

## 🔍 驗證建立結果

執行以下 SQL 確認：

```sql
-- 確認資料庫存在
SHOW DATABASES LIKE 'qa_tracker';

-- 確認資料表存在
USE qa_tracker;
SHOW TABLES;

-- 應該會看到：
-- config
-- records
-- users

-- 檢查資料表結構
DESCRIBE records;
DESCRIBE users;
DESCRIBE config;
```

---

## ⚠️ 注意事項

### 1. 使用 IF NOT EXISTS

所有 `CREATE` 語句都使用了 `IF NOT EXISTS`，所以：
- ✅ 如果資料表已存在，不會報錯
- ✅ 可以安全地重複執行
- ✅ 不會刪除現有資料

### 2. 外鍵約束

`records` 表有外鍵約束指向 `users` 表：
- 建議先建立 `users` 表，再建立 `records` 表
- 如果 `users` 表為空，外鍵會設為 NULL（使用 `ON DELETE SET NULL`）

### 3. 資料庫編碼

使用 `utf8mb4` 編碼，支援完整的 Unicode 字符（包括 emoji）。

---

## 🎯 快速檢查清單

- [ ] 資料庫 `qa_tracker` 是否存在？
- [ ] 資料表 `users` 是否存在？
- [ ] 資料表 `records` 是否存在？
- [ ] 資料表 `config` 是否存在？
- [ ] 資料表結構是否正確？

如果以上都確認無誤，就可以開始匯入資料了！

---

## 💡 提示

如果資料庫和資料表都不存在，最簡單的方法是：

1. 在 IDE 中打開 `mysql/schema.sql`
2. 全選複製
3. 在 IDE 的 SQL 編輯器中貼上並執行

這樣就會一次性建立所有需要的資料庫和資料表。

