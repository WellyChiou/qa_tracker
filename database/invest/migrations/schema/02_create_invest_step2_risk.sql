-- Step 2 schema for Invest domain (risk analysis only)
-- Scope: risk_rule, portfolio_risk_result, portfolio_risk_reason

USE invest;

CREATE TABLE IF NOT EXISTS risk_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_code VARCHAR(60) NOT NULL,
    rule_name VARCHAR(120) NOT NULL,
    rule_type VARCHAR(60) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    score_weight DECIMAL(8,4) NOT NULL DEFAULT 1.0000,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    description VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_risk_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS portfolio_risk_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    trade_date DATE NOT NULL,
    risk_score INT NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    recommendation VARCHAR(30) NOT NULL,
    summary_text VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_portfolio_risk_result_portfolio_trade_date UNIQUE (portfolio_id, trade_date),
    CONSTRAINT fk_portfolio_risk_result_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id),
    INDEX idx_portfolio_risk_result_trade_date (trade_date),
    INDEX idx_portfolio_risk_result_risk_level (risk_level),
    INDEX idx_portfolio_risk_result_recommendation (recommendation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS portfolio_risk_reason (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    risk_result_id BIGINT NOT NULL,
    rule_code VARCHAR(60) NOT NULL,
    reason_title VARCHAR(200) NOT NULL,
    reason_detail VARCHAR(1000) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    score_impact INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_portfolio_risk_reason_result FOREIGN KEY (risk_result_id) REFERENCES portfolio_risk_result(id) ON DELETE CASCADE,
    INDEX idx_portfolio_risk_reason_risk_result_id (risk_result_id),
    INDEX idx_portfolio_risk_reason_rule_code (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
