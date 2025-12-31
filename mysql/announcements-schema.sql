-- 公告表格
-- 教會系統專用

USE church;

-- 公告表
CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    title VARCHAR(200) NOT NULL COMMENT '公告標題',
    content TEXT COMMENT '公告內容',
    category VARCHAR(50) COMMENT '分類（例如：一般、重要、活動、通知等）',
    publish_date DATE COMMENT '發布日期',
    expire_date DATE COMMENT '到期日期（NULL 表示永不過期）',
    is_pinned TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置頂（1=是，0=否）',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否啟用（1=是，0=否）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_category (category),
    INDEX idx_publish_date (publish_date),
    INDEX idx_expire_date (expire_date),
    INDEX idx_is_pinned (is_pinned),
    INDEX idx_is_active (is_active),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';

