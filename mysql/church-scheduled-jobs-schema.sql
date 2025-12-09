-- 教會資料庫排程管理表結構
-- 用於教會後台的定時任務管理

USE church;

-- 1. 定時任務表
CREATE TABLE IF NOT EXISTS scheduled_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    job_name VARCHAR(200) NOT NULL COMMENT '任務名稱',
    job_class VARCHAR(500) NOT NULL COMMENT '任務類別（完整類名）',
    cron_expression VARCHAR(100) NOT NULL COMMENT 'Cron 表達式',
    description VARCHAR(1000) COMMENT '任務描述',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否啟用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_enabled (enabled),
    INDEX idx_job_class (job_class)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定時任務表';

-- 2. 任務執行記錄表
CREATE TABLE IF NOT EXISTS job_executions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    job_id BIGINT NOT NULL COMMENT '任務 ID',
    status VARCHAR(20) NOT NULL COMMENT '執行狀態：PENDING, RUNNING, SUCCESS, FAILED',
    started_at DATETIME COMMENT '開始執行時間',
    completed_at DATETIME COMMENT '完成時間',
    result_message TEXT COMMENT '執行結果訊息',
    error_message TEXT COMMENT '錯誤訊息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    INDEX idx_job_id (job_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (job_id) REFERENCES scheduled_jobs(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任務執行記錄表';

