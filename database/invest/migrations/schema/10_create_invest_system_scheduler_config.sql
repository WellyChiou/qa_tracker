-- Step: System Scheduler management config (adapter/facade overlay)
-- Scope: 提供排程管理頁新增/編輯/刪除/啟用/停用所需最小主檔

USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS system_scheduled_job_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_code VARCHAR(80) NOT NULL,
    job_name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    schedule_type VARCHAR(30) NOT NULL,
    schedule_expression VARCHAR(255) NULL,
    allow_run_now TINYINT(1) NOT NULL DEFAULT 1,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_system_scheduled_job_config_job_code (job_code),
    INDEX idx_system_scheduled_job_config_active (is_active),
    INDEX idx_system_scheduled_job_config_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
