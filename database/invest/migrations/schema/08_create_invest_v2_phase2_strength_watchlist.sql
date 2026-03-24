-- V2 Phase 2 Slice A schema
-- Scope:
-- 1) watchlist (minimal, default-first)
-- 2) strength snapshot + explainable factors

USE invest;

CREATE TABLE IF NOT EXISTS watchlist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_watchlist_user FOREIGN KEY (user_id) REFERENCES users(uid),
    INDEX idx_watchlist_user_active (user_id, is_active),
    INDEX idx_watchlist_user_default (user_id, is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS watchlist_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    watchlist_id BIGINT NOT NULL,
    stock_id BIGINT NOT NULL,
    note VARCHAR(255) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_watchlist_item_watchlist FOREIGN KEY (watchlist_id) REFERENCES watchlist(id),
    CONSTRAINT fk_watchlist_item_stock FOREIGN KEY (stock_id) REFERENCES stock(id),
    UNIQUE KEY uk_watchlist_item_stock (watchlist_id, stock_id),
    INDEX idx_watchlist_item_watchlist_active (watchlist_id, is_active),
    INDEX idx_watchlist_item_stock (stock_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS strength_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    stock_id BIGINT NOT NULL,
    trade_date DATE NOT NULL,
    universe_type VARCHAR(20) NOT NULL,
    watchlist_id BIGINT NULL,
    watch_scope_id BIGINT NOT NULL DEFAULT 0,
    price_close DECIMAL(18,4) NOT NULL,
    price_trade_date DATE NOT NULL,
    strength_score INT NULL,
    strength_level VARCHAR(20) NULL,
    recommendation_code VARCHAR(60) NULL,
    summary_text VARCHAR(1000) NULL,
    data_quality VARCHAR(20) NOT NULL,
    computed_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_strength_snapshot_user FOREIGN KEY (user_id) REFERENCES users(uid),
    CONSTRAINT fk_strength_snapshot_stock FOREIGN KEY (stock_id) REFERENCES stock(id),
    CONSTRAINT fk_strength_snapshot_watchlist FOREIGN KEY (watchlist_id) REFERENCES watchlist(id),
    UNIQUE KEY uk_strength_snapshot_scope (user_id, stock_id, trade_date, universe_type, watch_scope_id),
    INDEX idx_strength_snapshot_user_trade (user_id, trade_date),
    INDEX idx_strength_snapshot_user_score (user_id, strength_score),
    INDEX idx_strength_snapshot_stock_trade (stock_id, trade_date),
    INDEX idx_strength_snapshot_watchlist (watchlist_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS strength_snapshot_factor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_id BIGINT NOT NULL,
    factor_code VARCHAR(50) NOT NULL,
    factor_name VARCHAR(100) NOT NULL,
    raw_value_text VARCHAR(255) NULL,
    threshold_text VARCHAR(255) NULL,
    score_contribution INT NOT NULL,
    explanation_text VARCHAR(1000) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_strength_factor_snapshot FOREIGN KEY (snapshot_id) REFERENCES strength_snapshot(id) ON DELETE CASCADE,
    INDEX idx_strength_factor_snapshot_sort (snapshot_id, sort_order),
    INDEX idx_strength_factor_snapshot_score (snapshot_id, score_contribution)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
