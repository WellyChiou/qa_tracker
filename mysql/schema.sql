-- QA Tracker 資料庫結構
-- 從 Firebase Firestore 遷移到 MySQL

-- 建立資料庫（如果不存在）
CREATE DATABASE IF NOT EXISTS qa_tracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE qa_tracker;

-- 1. 使用者表（對應 Firestore users collection）
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

