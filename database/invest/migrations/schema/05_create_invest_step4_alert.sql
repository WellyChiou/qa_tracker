-- Step 4 schema for Invest domain
-- Scope: portfolio_alert_setting, portfolio_alert_event

USE invest;

CREATE TABLE IF NOT EXISTS portfolio_alert_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    stop_loss_price DECIMAL(18,4) NULL,
    alert_drop_percent DECIMAL(9,2) NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_portfolio_alert_setting_portfolio UNIQUE (portfolio_id),
    CONSTRAINT fk_portfolio_alert_setting_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id) ON DELETE CASCADE,
    INDEX idx_portfolio_alert_setting_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS portfolio_alert_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    trigger_type VARCHAR(30) NOT NULL,
    trigger_value DECIMAL(18,4) NOT NULL,
    message VARCHAR(500) NOT NULL,
    triggered_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_portfolio_alert_event_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id) ON DELETE CASCADE,
    INDEX idx_portfolio_alert_event_portfolio_triggered_at (portfolio_id, triggered_at),
    INDEX idx_portfolio_alert_event_trigger_type_time (trigger_type, triggered_at),
    INDEX idx_portfolio_alert_event_triggered_at (triggered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
