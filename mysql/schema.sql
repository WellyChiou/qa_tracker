-- QA Tracker 資料庫結構
-- 從 Firebase Firestore 遷移到 MySQL

-- 建立資料庫（如果不存在）
CREATE DATABASE IF NOT EXISTS qa_tracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE qa_tracker;

-- 1. 使用者表（對應 Firestore users collection）
CREATE TABLE IF NOT EXISTS users (
    uid VARCHAR(128) PRIMARY KEY COMMENT 'Firebase UID',
    email VARCHAR(255) UNIQUE COMMENT '電子郵件',
    username VARCHAR(100) UNIQUE COMMENT '用戶名（用於登入）',
    password VARCHAR(255) COMMENT '加密後的密碼',
    display_name VARCHAR(255) COMMENT '顯示名稱',
    photo_url TEXT COMMENT '頭像 URL',
    provider_id VARCHAR(50) COMMENT '登入提供者（如 local, firebase, google 等）',
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '帳號是否啟用',
    is_account_non_locked BOOLEAN NOT NULL DEFAULT TRUE COMMENT '帳號是否未鎖定',
    last_login_at DATETIME COMMENT '最後登入時間',
    line_user_id VARCHAR(50) UNIQUE COMMENT 'LINE 用戶 ID，用於 LINE Bot 綁定',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_email (email),
    INDEX idx_username (username),
    INDEX idx_line_user_id (line_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='使用者表';

-- 2. 記錄表（對應 Firestore records collection）
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

-- 3. 設定表（對應 Firestore config collection）
CREATE TABLE IF NOT EXISTS config (
    config_key VARCHAR(100) PRIMARY KEY COMMENT '設定鍵',
    config_value TEXT COMMENT '設定值（JSON 格式）',
    description VARCHAR(255) COMMENT '說明',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系統設定表';

-- 插入預設設定（GitLab Token）
INSERT INTO config (config_key, config_value, description) 
VALUES ('gitlab_token', '', 'GitLab API Token')
ON DUPLICATE KEY UPDATE config_value = config_value;

-- 4. 記帳記錄表（對應 Firestore expenses collection）
CREATE TABLE IF NOT EXISTS expenses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    firebase_id VARCHAR(128) UNIQUE COMMENT 'Firebase 原始 ID（遷移用）',
    date DATE NOT NULL COMMENT '日期',
    member VARCHAR(50) NOT NULL COMMENT '家庭成員：爸爸、媽媽、孩子、其他',
    type VARCHAR(20) NOT NULL COMMENT '類型：收入、支出',
    main_category VARCHAR(50) COMMENT '類別：食、衣、住、行、育、樂、醫療、其他支出、薪資、投資等',
    sub_category VARCHAR(100) COMMENT '細項',
    amount DECIMAL(15, 2) NOT NULL COMMENT '金額',
    currency VARCHAR(10) DEFAULT 'TWD' COMMENT '幣別：TWD、USD、EUR、JPY、CNY',
    exchange_rate DECIMAL(10, 4) DEFAULT 1.0000 COMMENT '匯率',
    description TEXT COMMENT '描述',
    created_by_uid VARCHAR(128) COMMENT '建立者 UID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_by_uid VARCHAR(128) COMMENT '更新者 UID',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_date (date),
    INDEX idx_member (member),
    INDEX idx_type (type),
    INDEX idx_main_category (main_category),
    INDEX idx_created_at (created_at),
    INDEX idx_date_type (date, type),
    FOREIGN KEY (created_by_uid) REFERENCES users(uid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='記帳記錄表';

-- 5. 資產表（對應 Firestore assets collection）
CREATE TABLE IF NOT EXISTS assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    firebase_id VARCHAR(128) UNIQUE COMMENT 'Firebase 原始 ID（遷移用）',
    stock_code VARCHAR(50) COMMENT '股票代碼',
    asset_type VARCHAR(50) COMMENT '資產類型：股票、ETF、基金等',
    name VARCHAR(200) COMMENT '資產名稱',
    currency VARCHAR(10) DEFAULT 'TWD' COMMENT '幣別：TWD、USD、EUR、JPY、CNY',
    quantity DECIMAL(15, 4) DEFAULT 0 COMMENT '數量',
    cost DECIMAL(15, 2) DEFAULT 0 COMMENT '成本',
    unit_price DECIMAL(15, 4) COMMENT '單價（成本/數量）',
    current_price DECIMAL(15, 4) DEFAULT 0 COMMENT '當前價格',
    member VARCHAR(50) COMMENT '家庭成員',
    category VARCHAR(50) COMMENT '類別',
    order_index INT DEFAULT 0 COMMENT '排序順序',
    created_by_uid VARCHAR(128) COMMENT '建立者 UID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_by_uid VARCHAR(128) COMMENT '更新者 UID',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_stock_code (stock_code),
    INDEX idx_member (member),
    INDEX idx_order (order_index),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (created_by_uid) REFERENCES users(uid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='資產表';

-- 6. 匯率表（對應 Firestore exchangeRates / dailyExchangeRates collection）
CREATE TABLE IF NOT EXISTS exchange_rates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    date DATE NOT NULL COMMENT '匯率日期',
    usd_rate DECIMAL(10, 4) COMMENT '美元匯率',
    eur_rate DECIMAL(10, 4) COMMENT '歐元匯率',
    jpy_rate DECIMAL(10, 4) COMMENT '日圓匯率',
    cny_rate DECIMAL(10, 4) COMMENT '人民幣匯率',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    UNIQUE KEY uk_date (date),
    INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='匯率表';

-- 7. 定時任務表
CREATE TABLE IF NOT EXISTS scheduled_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    job_name VARCHAR(200) NOT NULL COMMENT '任務名稱',
    job_class VARCHAR(500) NOT NULL COMMENT '任務類別（完整類名）',
    cron_expression VARCHAR(100) NOT NULL COMMENT 'Cron 表達式',
    description VARCHAR(1000) COMMENT '任務描述',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否啟用：0=停用, 1=啟用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_enabled (enabled),
    INDEX idx_job_name (job_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定時任務表';

-- 插入預設的匯率補足任務
INSERT INTO scheduled_jobs (job_name, job_class, cron_expression, description, enabled) 
VALUES ('自動補足匯率', 'com.example.helloworld.scheduler.ExchangeRateScheduler$AutoFillExchangeRatesJob', '0 0 7 * * ?', '每天早上 7:00 自動補足最近 7 天的匯率', 1)
ON DUPLICATE KEY UPDATE job_name = job_name;

-- 插入 LINE Bot 每日費用提醒任務（晚上 8 點）
INSERT INTO scheduled_jobs (job_name, job_class, cron_expression, description, enabled) 
VALUES ('LINE Bot 每日費用提醒', 'com.example.helloworld.scheduler.DailyExpenseReminderScheduler$SendDailyExpenseReminderJob', '0 0 20 * * ?', '每天晚上 20:00 檢查用戶是否已記錄今日費用，如果沒有則發送提醒通知', 1)
ON DUPLICATE KEY UPDATE job_name = job_name;

-- 插入 LINE Bot 每日費用檢查與統計任務（晚上 9 點）
INSERT INTO scheduled_jobs (job_name, job_class, cron_expression, description, enabled) 
VALUES ('LINE Bot 每日費用檢查與統計', 'com.example.helloworld.scheduler.DailyExpenseReminderScheduler$CheckAndNotifyDailyExpenseJob', '0 0 21 * * ?', '每天晚上 21:00 檢查用戶是否已記錄今日費用，如果沒有則發送提醒，如果有則發送統計報告（包含個人和群組）', 1)
ON DUPLICATE KEY UPDATE job_name = job_name;

-- 8. Job 執行記錄表
CREATE TABLE IF NOT EXISTS job_executions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    job_id BIGINT NOT NULL COMMENT 'Job ID',
    status VARCHAR(20) NOT NULL COMMENT '狀態：PENDING, RUNNING, SUCCESS, FAILED',
    started_at DATETIME COMMENT '開始時間',
    completed_at DATETIME COMMENT '完成時間',
    error_message TEXT COMMENT '錯誤訊息',
    result_message TEXT COMMENT '結果訊息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    INDEX idx_job_id (job_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (job_id) REFERENCES scheduled_jobs(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Job 執行記錄表';

-- 9. LINE 群組表
CREATE TABLE IF NOT EXISTS line_groups (
    group_id VARCHAR(100) PRIMARY KEY COMMENT 'LINE 群組 ID',
    group_name VARCHAR(255) COMMENT '群組名稱',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否啟用通知',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LINE 群組表（個人網站用）';

-- 教會後台 LINE 群組表（與個人網站獨立）
CREATE TABLE IF NOT EXISTS church_line_groups (
    group_id VARCHAR(100) PRIMARY KEY COMMENT 'LINE 群組 ID',
    group_name VARCHAR(255) COMMENT '群組名稱',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否啟用通知',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LINE 群組表（教會後台用）';

