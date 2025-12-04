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
    position_config JSON COMMENT '崗位人員配置（JSON 格式）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    UNIQUE KEY uk_schedule_date_version (schedule_date, version),
    INDEX idx_schedule_date (schedule_date),
    INDEX idx_start_date (start_date),
    INDEX idx_end_date (end_date),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服事安排表';

-- 崗位人員配置表（默認配置）
CREATE TABLE IF NOT EXISTS position_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    config_name VARCHAR(100) NOT NULL DEFAULT 'default' COMMENT '配置名稱（用於支持多套配置）',
    config_data JSON NOT NULL COMMENT '崗位人員配置（JSON 格式）',
    is_default TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否為默認配置（1=是，0=否）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    UNIQUE KEY uk_config_name (config_name),
    INDEX idx_is_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='崗位人員配置表';

