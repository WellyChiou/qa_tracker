-- ============================================
-- Church 崗位和人員管理系統 - 完整初始化腳本
-- ============================================
-- 此腳本用於全新安裝，包含所有最新的表結構和字段
-- 執行前請確保 church 數據庫已創建
-- ============================================

USE church;

-- 設置字符集
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- 1. 崗位表
-- ============================================
CREATE TABLE IF NOT EXISTS positions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    position_code VARCHAR(50) UNIQUE NOT NULL COMMENT '崗位代碼（如：computer, sound, light, live）',
    position_name VARCHAR(100) NOT NULL COMMENT '崗位名稱（如：電腦、混音、燈光、直播）',
    description VARCHAR(255) COMMENT '崗位描述',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否啟用（1=是，0=否）',
    sort_order INT DEFAULT 0 COMMENT '排序順序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_position_code (position_code),
    INDEX idx_is_active (is_active),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='崗位表';

-- ============================================
-- 2. 人員表
-- ============================================
CREATE TABLE IF NOT EXISTS persons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    person_name VARCHAR(100) NOT NULL COMMENT '人員姓名',
    display_name VARCHAR(100) COMMENT '顯示名稱（可選）',
    member_no VARCHAR(32) UNIQUE COMMENT '會員編號（用於簽到系統）',
    birthday DATE COMMENT '生日（非必填）',
    phone VARCHAR(20) COMMENT '電話',
    email VARCHAR(255) COMMENT '電子郵件',
    notes TEXT COMMENT '備註',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否啟用（1=是，0=否）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_person_name (person_name),
    INDEX idx_member_no (member_no),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人員表';

-- ============================================
-- 3. 崗位人員關聯表
-- ============================================
-- 記錄每個崗位在不同日期（週六/週日）有哪些人員
-- 包含 include_in_auto_schedule 字段，用於控制是否參與自動分配
CREATE TABLE IF NOT EXISTS position_persons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    position_id BIGINT NOT NULL COMMENT '崗位 ID',
    person_id BIGINT NOT NULL COMMENT '人員 ID',
    day_type VARCHAR(20) NOT NULL COMMENT '日期類型：saturday（週六）或 sunday（週日）',
    sort_order INT DEFAULT 0 COMMENT '排序順序',
    include_in_auto_schedule TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否參與自動分配（1=是，0=否，只在編輯模式下顯示）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    FOREIGN KEY (position_id) REFERENCES positions(id) ON DELETE CASCADE,
    FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE,
    UNIQUE KEY uk_position_person_day (position_id, person_id, day_type),
    INDEX idx_position_id (position_id),
    INDEX idx_person_id (person_id),
    INDEX idx_day_type (day_type),
    INDEX idx_include_in_auto_schedule (include_in_auto_schedule)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='崗位人員關聯表';

-- ============================================
-- 4. 插入預設崗位
-- ============================================
INSERT INTO positions (position_code, position_name, description, sort_order, is_active) VALUES
('computer', '電腦', '負責電腦操作', 1, 1),
('sound', '混音', '負責音控混音', 2, 1),
('light', '燈光', '負責燈光控制', 3, 1),
('live', '直播', '負責直播操作', 4, 1)
ON DUPLICATE KEY UPDATE 
    position_name = VALUES(position_name),
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    is_active = VALUES(is_active);

-- ============================================
-- 完成
-- ============================================
SELECT 'Church 崗位和人員管理系統初始化完成！' AS message;

