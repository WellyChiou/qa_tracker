-- 代禱事項表格
-- 教會系統專用

USE church;

-- 代禱事項表
CREATE TABLE IF NOT EXISTS prayer_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    title VARCHAR(200) NOT NULL COMMENT '代禱事項標題',
    content TEXT COMMENT '代禱事項內容',
    category VARCHAR(50) COMMENT '分類（例如：個人、家庭、教會、社區等）',
    is_urgent TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否緊急（1=是，0=否）',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否啟用（1=是，0=否）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_category (category),
    INDEX idx_is_urgent (is_urgent),
    INDEX idx_is_active (is_active),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代禱事項表';

