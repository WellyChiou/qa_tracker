-- 服事表重新設計
-- 新的資料結構：
-- 1. service_schedules: 主表，只記錄使用者自訂義名稱
-- 2. service_schedule_dates: 明細表，記錄每個服事表的所有日期
-- 3. service_schedule_assignments: 關聯表，記錄每個日期每個崗位的人員分配

USE church;

-- 1. 修改 service_schedules 表（主表）
-- 先備份現有資料（如果需要）
-- CREATE TABLE service_schedules_backup AS SELECT * FROM service_schedules;

-- 刪除舊表（如果存在且需要重新建立）
-- DROP TABLE IF EXISTS service_schedule_assignments;
-- DROP TABLE IF EXISTS service_schedule_dates;
-- DROP TABLE IF EXISTS service_schedules;

-- 建立新的 service_schedules 表（主表）
CREATE TABLE IF NOT EXISTS service_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    name VARCHAR(255) NOT NULL COMMENT '使用者自訂義名稱',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_name (name),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服事安排表（主表）';

-- 2. 建立 service_schedule_dates 表（明細表 - 記錄日期）
-- 注意：day_of_week 使用 GENERATED COLUMN 自動計算，符合第三正規化
-- 如果 MySQL 版本 < 5.7，請使用下方註解掉的版本（手動維護 day_of_week）
CREATE TABLE IF NOT EXISTS service_schedule_dates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    service_schedule_id BIGINT NOT NULL COMMENT '服事表 ID',
    date DATE NOT NULL COMMENT '日期',
    -- MySQL 5.7+ 版本：使用 GENERATED COLUMN（符合第三正規化）
    -- day_of_week 自動從 date 計算，保證一致性
    day_of_week TINYINT GENERATED ALWAYS AS (DAYOFWEEK(date)) STORED COMMENT '星期幾（1=週日, 2=週一, ..., 7=週六）',
    day_of_week_label VARCHAR(10) GENERATED ALWAYS AS (
        CASE DAYOFWEEK(date)
            WHEN 1 THEN '日'
            WHEN 7 THEN '六'
            ELSE ''
        END
    ) STORED COMMENT '星期幾標籤：六（週六）或 日（週日）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    FOREIGN KEY (service_schedule_id) REFERENCES service_schedules(id) ON DELETE CASCADE,
    UNIQUE KEY uk_schedule_date (service_schedule_id, date),
    INDEX idx_service_schedule_id (service_schedule_id),
    INDEX idx_date (date),
    INDEX idx_day_of_week (day_of_week),
    INDEX idx_day_of_week_label (day_of_week_label)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服事表日期明細';

-- 如果 MySQL 版本 < 5.7，請使用以下版本（移除 GENERATED ALWAYS AS）：
-- CREATE TABLE IF NOT EXISTS service_schedule_dates (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
--     service_schedule_id BIGINT NOT NULL COMMENT '服事表 ID',
--     date DATE NOT NULL COMMENT '日期',
--     day_of_week TINYINT NOT NULL COMMENT '星期幾（1=週日, 2=週一, ..., 7=週六）',
--     day_of_week_label VARCHAR(10) NOT NULL COMMENT '星期幾標籤：六（週六）或 日（週日）',
--     created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
--     updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
--     FOREIGN KEY (service_schedule_id) REFERENCES service_schedules(id) ON DELETE CASCADE,
--     UNIQUE KEY uk_schedule_date (service_schedule_id, date),
--     INDEX idx_service_schedule_id (service_schedule_id),
--     INDEX idx_date (date),
--     INDEX idx_day_of_week (day_of_week),
--     INDEX idx_day_of_week_label (day_of_week_label),
--     -- 使用 CHECK 約束確保 day_of_week 與 date 一致（MySQL 8.0.16+）
--     CONSTRAINT chk_day_of_week CHECK (day_of_week = DAYOFWEEK(date))
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服事表日期明細';

-- 3. 建立 service_schedule_position_config 表（崗位配置表 - 記錄每個日期每個崗位需要多少人）
CREATE TABLE IF NOT EXISTS service_schedule_position_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    service_schedule_date_id BIGINT NOT NULL COMMENT '服事表日期 ID',
    position_id BIGINT NOT NULL COMMENT '崗位 ID',
    person_count INT DEFAULT 1 COMMENT '此崗位設定了多少人員（預設為 1）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    FOREIGN KEY (service_schedule_date_id) REFERENCES service_schedule_dates(id) ON DELETE CASCADE,
    FOREIGN KEY (position_id) REFERENCES positions(id) ON DELETE CASCADE,
    UNIQUE KEY uk_schedule_date_position (service_schedule_date_id, position_id),
    INDEX idx_schedule_date_id (service_schedule_date_id),
    INDEX idx_position_id (position_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服事表崗位配置';

-- 4. 建立 service_schedule_assignments 表（關聯表 - 記錄人員分配）
CREATE TABLE IF NOT EXISTS service_schedule_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    service_schedule_position_config_id BIGINT NOT NULL COMMENT '服事表崗位配置 ID（取代 service_schedule_date_id 和 position_id）',
    person_id BIGINT COMMENT '人員 ID（可為 NULL，表示該崗位未分配人員）',
    sort_order INT DEFAULT 0 COMMENT '排序順序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    FOREIGN KEY (service_schedule_position_config_id) REFERENCES service_schedule_position_config(id) ON DELETE CASCADE,
    FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE SET NULL,
    INDEX idx_position_config_id (service_schedule_position_config_id),
    INDEX idx_person_id (person_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服事表人員分配';

