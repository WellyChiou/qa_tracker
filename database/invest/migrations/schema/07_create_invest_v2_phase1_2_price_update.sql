-- V2 Phase 1-2 schema
-- Scope:
-- 1) add batch linkage and quality metadata for stock_price_daily
-- 2) extend price_update_job_log for scheduler run mode + batch id

USE invest;

SET @ddl := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'invest'
          AND TABLE_NAME = 'stock_price_daily'
          AND COLUMN_NAME = 'update_batch_id'
    ),
    'SELECT 1',
    'ALTER TABLE stock_price_daily ADD COLUMN update_batch_id VARCHAR(64) NULL AFTER latency_type'
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
          AND COLUMN_NAME = 'data_quality'
    ),
    'SELECT 1',
    'ALTER TABLE stock_price_daily ADD COLUMN data_quality VARCHAR(20) NULL AFTER update_batch_id'
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
          AND INDEX_NAME = 'idx_stock_price_daily_update_batch_id'
    ),
    'SELECT 1',
    'ALTER TABLE stock_price_daily ADD INDEX idx_stock_price_daily_update_batch_id (update_batch_id)'
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
          AND INDEX_NAME = 'idx_stock_price_daily_data_quality'
    ),
    'SELECT 1',
    'ALTER TABLE stock_price_daily ADD INDEX idx_stock_price_daily_data_quality (data_quality)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'invest'
          AND TABLE_NAME = 'price_update_job_log'
          AND COLUMN_NAME = 'batch_id'
    ),
    'SELECT 1',
    'ALTER TABLE price_update_job_log ADD COLUMN batch_id VARCHAR(64) NULL AFTER job_name'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'invest'
          AND TABLE_NAME = 'price_update_job_log'
          AND COLUMN_NAME = 'run_mode'
    ),
    'SELECT 1',
    'ALTER TABLE price_update_job_log ADD COLUMN run_mode VARCHAR(30) NOT NULL DEFAULT ''MANUAL'' AFTER user_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE price_update_job_log
SET batch_id = CONCAT('legacy-', id)
WHERE batch_id IS NULL;

ALTER TABLE price_update_job_log
    MODIFY COLUMN batch_id VARCHAR(64) NOT NULL;

SET @ddl := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = 'invest'
          AND TABLE_NAME = 'price_update_job_log'
          AND INDEX_NAME = 'uk_price_update_job_log_batch_id'
    ),
    'SELECT 1',
    'ALTER TABLE price_update_job_log ADD UNIQUE INDEX uk_price_update_job_log_batch_id (batch_id)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = 'invest'
          AND TABLE_NAME = 'price_update_job_log'
          AND INDEX_NAME = 'idx_price_update_job_log_run_mode'
    ),
    'SELECT 1',
    'ALTER TABLE price_update_job_log ADD INDEX idx_price_update_job_log_run_mode (run_mode)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 第一版資料品質規則：僅以日曆天判定 STALE，不使用交易日曆。
UPDATE stock_price_daily
SET data_quality = CASE
    WHEN TIMESTAMPDIFF(DAY, trade_date, CURDATE()) > 3 THEN 'STALE'
    ELSE 'GOOD'
END
WHERE data_quality IS NULL;
