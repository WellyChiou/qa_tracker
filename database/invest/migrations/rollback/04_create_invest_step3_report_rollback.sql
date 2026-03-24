-- Rollback for Step 3 report schema

USE invest;

DROP TABLE IF EXISTS scheduler_job_log;
DROP TABLE IF EXISTS daily_report;
DROP TABLE IF EXISTS portfolio_daily_snapshot;
