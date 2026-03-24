-- V2 Phase 2 Slice B schema
-- Scope:
-- 1) opportunity signal (watch/observe only)
-- 2) explainable signal reasons

USE invest;

CREATE TABLE IF NOT EXISTS opportunity_signal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    stock_id BIGINT NOT NULL,
    trade_date DATE NOT NULL,
    signal_key VARCHAR(120) NOT NULL,
    signal_type VARCHAR(50) NOT NULL,
    signal_score INT NOT NULL,
    recommendation_code VARCHAR(60) NOT NULL,
    condition_text VARCHAR(1000) NOT NULL,
    summary_text VARCHAR(1000) NOT NULL,
    disclaimer_text VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    source_snapshot_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_opportunity_signal_user FOREIGN KEY (user_id) REFERENCES users(uid),
    CONSTRAINT fk_opportunity_signal_stock FOREIGN KEY (stock_id) REFERENCES stock(id),
    CONSTRAINT fk_opportunity_signal_snapshot FOREIGN KEY (source_snapshot_id) REFERENCES strength_snapshot(id) ON DELETE SET NULL,
    UNIQUE KEY uk_opportunity_signal_user_stock_key (user_id, stock_id, signal_key),
    INDEX idx_opportunity_signal_user_trade (user_id, trade_date),
    INDEX idx_opportunity_signal_user_status (user_id, status),
    INDEX idx_opportunity_signal_type (signal_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS opportunity_signal_reason (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    signal_id BIGINT NOT NULL,
    reason_title VARCHAR(200) NOT NULL,
    reason_detail VARCHAR(1000) NOT NULL,
    score_impact INT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_opportunity_signal_reason_signal FOREIGN KEY (signal_id) REFERENCES opportunity_signal(id) ON DELETE CASCADE,
    INDEX idx_opportunity_signal_reason_signal_sort (signal_id, sort_order),
    INDEX idx_opportunity_signal_reason_signal_score (signal_id, score_impact)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
