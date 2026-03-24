-- Step 1 schema for Invest domain
-- Scope: stock, stock_price_daily, portfolio

USE invest;

CREATE TABLE IF NOT EXISTS stock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    market VARCHAR(20) NOT NULL,
    ticker VARCHAR(20) NOT NULL,
    name VARCHAR(120) NOT NULL,
    industry VARCHAR(120) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_stock_market_ticker UNIQUE (market, ticker),
    INDEX idx_stock_ticker (ticker),
    INDEX idx_stock_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_price_daily (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_id BIGINT NOT NULL,
    trade_date DATE NOT NULL,
    open_price DECIMAL(18,4) NOT NULL,
    high_price DECIMAL(18,4) NOT NULL,
    low_price DECIMAL(18,4) NOT NULL,
    close_price DECIMAL(18,4) NOT NULL,
    volume BIGINT NOT NULL,
    change_amount DECIMAL(18,4) NULL,
    change_percent DECIMAL(9,4) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_stock_price_daily_stock_date UNIQUE (stock_id, trade_date),
    CONSTRAINT fk_stock_price_daily_stock FOREIGN KEY (stock_id) REFERENCES stock(id),
    INDEX idx_stock_price_daily_trade_date (trade_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS portfolio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    stock_id BIGINT NOT NULL,
    avg_cost DECIMAL(18,4) NOT NULL,
    quantity DECIMAL(18,4) NOT NULL,
    total_cost DECIMAL(18,2) NOT NULL,
    note VARCHAR(500) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_portfolio_stock FOREIGN KEY (stock_id) REFERENCES stock(id),
    INDEX idx_portfolio_user_id (user_id),
    INDEX idx_portfolio_stock_id (stock_id),
    INDEX idx_portfolio_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
