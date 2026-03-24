-- Step 3 schema for Invest domain
-- Scope: portfolio_daily_snapshot, daily_report, scheduler_job_log

USE invest;

CREATE TABLE IF NOT EXISTS portfolio_daily_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    trade_date DATE NOT NULL,
    close_price DECIMAL(18,4) NOT NULL,
    market_value DECIMAL(18,2) NOT NULL,
    unrealized_profit_loss DECIMAL(18,2) NOT NULL,
    unrealized_profit_loss_percent DECIMAL(9,2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_portfolio_daily_snapshot_portfolio_trade_date UNIQUE (portfolio_id, trade_date),
    CONSTRAINT fk_portfolio_daily_snapshot_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id) ON DELETE CASCADE,
    INDEX idx_portfolio_daily_snapshot_trade_date (trade_date),
    INDEX idx_portfolio_daily_snapshot_portfolio_id (portfolio_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS daily_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    report_date DATE NOT NULL,
    report_type VARCHAR(60) NOT NULL,
    summary_json LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_daily_report_user_date_type UNIQUE (user_id, report_date, report_type),
    CONSTRAINT fk_daily_report_user FOREIGN KEY (user_id) REFERENCES users(uid) ON DELETE CASCADE,
    INDEX idx_daily_report_user_id (user_id),
    INDEX idx_daily_report_report_date (report_date),
    INDEX idx_daily_report_report_type_status (report_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS scheduler_job_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(100) NOT NULL,
    run_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at DATETIME NOT NULL,
    finished_at DATETIME NULL,
    message VARCHAR(1000) NULL,
    INDEX idx_scheduler_job_log_job_name_run_date (job_name, run_date),
    INDEX idx_scheduler_job_log_status (status),
    INDEX idx_scheduler_job_log_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
