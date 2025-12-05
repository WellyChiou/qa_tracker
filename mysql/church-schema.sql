-- Church 資料庫結構
-- 教會網站專用資料庫

-- 建立資料庫（如果不存在）
CREATE DATABASE IF NOT EXISTS church CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 授予 appuser 對 church 資料庫的權限
GRANT ALL PRIVILEGES ON church.* TO 'appuser'@'%';
FLUSH PRIVILEGES;

USE church;

-- 服事安排表
CREATE TABLE IF NOT EXISTS service_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    schedule_date DATE NOT NULL COMMENT '安排日期（開始日期）',
    version INT NOT NULL DEFAULT 1 COMMENT '版本號（同一天可以有多個版本）',
    start_date DATE NOT NULL COMMENT '日期範圍開始日期',
    end_date DATE NOT NULL COMMENT '日期範圍結束日期',
    schedule_data JSON NOT NULL COMMENT '安排表數據（JSON 格式）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    UNIQUE KEY uk_schedule_date_version (schedule_date, version),
    INDEX idx_schedule_date (schedule_date),
    INDEX idx_start_date (start_date),
    INDEX idx_end_date (end_date),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服事安排表';

-- 注意：position_config 表已移除，崗位配置現在使用 positions, persons, position_persons 表管理
-- 詳見 mysql/church-positions-schema.sql

