-- Rollback for Step 2 risk schema

USE invest;

DROP TABLE IF EXISTS portfolio_risk_reason;
DROP TABLE IF EXISTS portfolio_risk_result;
DROP TABLE IF EXISTS risk_rule;
