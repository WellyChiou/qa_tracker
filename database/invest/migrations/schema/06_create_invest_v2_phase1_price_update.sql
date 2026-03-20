-- V2 Phase 1-1 schema
-- Scope:
-- 1) extend stock_price_daily with minimal source metadata
-- 2) add price_update_job_log / price_update_job_detail

USE invest;

SET @ddl := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'invest'
          AND TABLE_NAME = 'stock_price_daily'
          AND COLUMN_NAME = 'data_source'
    ),
    'SELECT 1',
    'ALTER TABLE stock_price_daily ADD COLUMN data_source VARCHAR(50) NULL AFTER change_percent'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'invest'
          AND TABLE_NAME = 'stock_price_daily'
          AND COLUMN_NAME = 'fetched_at'
    ),
    'SELECT 1',
    'ALTER TABLE stock_price_daily ADD COLUMN fetched_at DATETIME NULL AFTER data_source'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'invest'
          AND TABLE_NAME = 'stock_price_daily'
          AND COLUMN_NAME = 'latency_type'
    ),
    'SELECT 1',
    'ALTER TABLE stock_price_daily ADD COLUMN latency_type VARCHAR(20) NULL AFTER fetched_at'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = 'invest'
          AND TABLE_NAME = 'stock_price_daily'
          AND INDEX_NAME = 'idx_stock_price_daily_fetched_at'
    ),
    'SELECT 1',
    'ALTER TABLE stock_price_daily ADD INDEX idx_stock_price_daily_fetched_at (fetched_at)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = 'invest'
          AND TABLE_NAME = 'stock_price_daily'
          AND INDEX_NAME = 'idx_stock_price_daily_data_source'
    ),
    'SELECT 1',
    'ALTER TABLE stock_price_daily ADD INDEX idx_stock_price_daily_data_source (data_source)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS price_update_job_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(100) NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_count INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    fail_count INT NOT NULL DEFAULT 0,
    started_at DATETIME NOT NULL,
    finished_at DATETIME NULL,
    message VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_price_update_job_log_user FOREIGN KEY (user_id) REFERENCES users(uid),
    INDEX idx_price_update_job_log_user_started_at (user_id, started_at),
    INDEX idx_price_update_job_log_status (status),
    INDEX idx_price_update_job_log_job_name (job_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS price_update_job_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_log_id BIGINT NOT NULL,
    stock_id BIGINT NOT NULL,
    ticker VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    reason VARCHAR(500) NULL,
    trade_date DATE NULL,
    fetched_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_price_update_job_detail_job_log FOREIGN KEY (job_log_id) REFERENCES price_update_job_log(id) ON DELETE CASCADE,
    CONSTRAINT fk_price_update_job_detail_stock FOREIGN KEY (stock_id) REFERENCES stock(id),
    INDEX idx_price_update_job_detail_job_log (job_log_id),
    INDEX idx_price_update_job_detail_stock (stock_id),
    INDEX idx_price_update_job_detail_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
