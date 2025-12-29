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

-- 主日信息表
CREATE TABLE IF NOT EXISTS sunday_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    service_date DATE NOT NULL COMMENT '主日日期',
    service_type VARCHAR(20) NOT NULL DEFAULT 'SUNDAY' COMMENT '服務類型：SATURDAY（週六晚崇）或 SUNDAY（主日）',
    image_url VARCHAR(500) COMMENT 'DM圖片URL',
    title VARCHAR(200) COMMENT '標題/講題',
    scripture VARCHAR(500) COMMENT '經文',
    speaker VARCHAR(100) COMMENT '講員',
    content TEXT COMMENT '內容/解析文字',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否啟用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_service_date (service_date),
    INDEX idx_service_type (service_type),
    INDEX idx_is_active (is_active),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主日信息表';

-- 注意：position_config 表已移除，崗位配置現在使用 positions, persons, position_persons 表管理
-- 詳見 mysql/church-positions-schema.sql

-- =====================
-- 教會簽到系統資料表
-- =====================

-- 注意：簽到系統現在使用 persons 表，不再使用獨立的 members 表
-- persons 表的 member_no 和 birthday 欄位已在 mysql/church-init.sql 中定義
-- 此 members 表保留僅為了向後兼容，新系統請使用 persons 表

CREATE TABLE IF NOT EXISTS members (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  member_no VARCHAR(32) NOT NULL UNIQUE,
  name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='會員主檔（已廢棄，請使用 persons 表）';

CREATE TABLE IF NOT EXISTS sessions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_type VARCHAR(20),
  title VARCHAR(100),
  session_date DATE,
  open_at DATETIME,
  close_at DATETIME,
  status VARCHAR(20),
  session_code VARCHAR(64) UNIQUE,
  INDEX idx_session_date (session_date),
  INDEX idx_session_code (session_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='場次表';

CREATE TABLE IF NOT EXISTS session_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_id BIGINT NOT NULL,
  token VARCHAR(16) NOT NULL,
  expires_at DATETIME NOT NULL,
  INDEX idx_token (session_id, expires_at),
  INDEX idx_token_value (session_id, token, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='場次短效 token 表';

CREATE TABLE IF NOT EXISTS checkins (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  checked_in_at DATETIME NOT NULL,
  ip VARCHAR(45),
  user_agent VARCHAR(255),

  manual BOOLEAN DEFAULT FALSE,
  manual_note VARCHAR(255),
  manual_by VARCHAR(64),

  canceled BOOLEAN DEFAULT FALSE,
  canceled_at DATETIME,
  canceled_by VARCHAR(64),
  canceled_note VARCHAR(255),

  UNIQUE KEY uk_session_member (session_id, member_id),
  INDEX idx_session_checked (session_id, checked_in_at),
  INDEX idx_manual (manual, canceled, checked_in_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='簽到記錄表';

-- =====================
-- 小組管理系統資料表
-- =====================

-- 小組表
CREATE TABLE IF NOT EXISTS groups (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
  group_name VARCHAR(100) NOT NULL UNIQUE COMMENT '小組名稱',
  description TEXT COMMENT '小組描述',
  is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否啟用（1=是，0=否）',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
  INDEX idx_group_name (group_name),
  INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小組表';

-- 小組人員關聯表
CREATE TABLE IF NOT EXISTS group_persons (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
  group_id BIGINT NOT NULL COMMENT '小組 ID',
  person_id BIGINT NOT NULL COMMENT '人員 ID',
  joined_at DATE NOT NULL COMMENT '加入時間（用於計算出席率時過濾場次）',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
  FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
  FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE,
  UNIQUE KEY uk_group_person (group_id, person_id),
  INDEX idx_group_id (group_id),
  INDEX idx_person_id (person_id),
  INDEX idx_joined_at (joined_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小組人員關聯表';

-- 場次小組關聯表（支援聯合小組）
CREATE TABLE IF NOT EXISTS session_groups (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
  session_id BIGINT NOT NULL COMMENT '場次 ID',
  group_id BIGINT NOT NULL COMMENT '小組 ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
  FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE,
  FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
  UNIQUE KEY uk_session_group (session_id, group_id),
  INDEX idx_session_id (session_id),
  INDEX idx_group_id (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='場次小組關聯表';

-- ============================================
-- 資料庫結構變更（Migration）
-- ============================================

-- 1. 添加 LINE Bot 相關字段到 users 表（如果不存在）
SET @dbname = DATABASE();
SET @tablename = "users";
SET @columnname = "line_user_id";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " VARCHAR(50) UNIQUE COMMENT 'LINE 用戶 ID，用於 LINE Bot 綁定'")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加索引（如果不存在）
SET @indexname = "idx_line_user_id";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (index_name = @indexname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD INDEX ", @indexname, " (line_user_id)")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 2. 為 persons 表添加 group_id 欄位（如果不存在）
SET @tablename = "persons";
SET @columnname = "group_id";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " BIGINT COMMENT '所屬小組 ID（用於快速查詢，主要關係通過 group_persons 表維護）'")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加外鍵（如果不存在）
SET @constraintname = "fk_persons_group_id";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (constraint_name = @constraintname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD CONSTRAINT ", @constraintname, " FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE SET NULL")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加索引（如果不存在）
SET @indexname = "idx_group_id";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (index_name = @indexname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD INDEX ", @indexname, " (", @columnname, ")")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 3. 為 service_schedules 表添加 year 欄位和唯一約束（如果不存在）
SET @columnname = "year";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = 'service_schedules')
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  "ALTER TABLE service_schedules ADD COLUMN year INT COMMENT '年度（例如：2024）' AFTER name"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 為現有資料填充年度
UPDATE service_schedules ss
LEFT JOIN (
    SELECT service_schedule_id, MIN(date) as min_date
    FROM service_schedule_dates
    GROUP BY service_schedule_id
) ssd ON ssd.service_schedule_id = ss.id
SET ss.year = COALESCE(
    YEAR(ssd.min_date),
    YEAR(ss.created_at)
)
WHERE ss.year IS NULL;

-- 如果還有 NULL 值，使用當前年份
UPDATE service_schedules 
SET year = YEAR(CURDATE())
WHERE year IS NULL;

-- 設置 year 欄位為 NOT NULL（如果欄位存在且允許 NULL）
SET @preparedStatement = (SELECT IF(
  (
    SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = 'service_schedules')
      AND (table_schema = @dbname)
      AND (column_name = 'year')
  ) = 'YES',
  "ALTER TABLE service_schedules MODIFY COLUMN year INT NOT NULL COMMENT '年度（例如：2024）'",
  "SELECT 1"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加唯一約束，確保每個年度只有一個版本（如果不存在）
SET @constraintname = "uk_year";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE
      (table_name = 'service_schedules')
      AND (table_schema = @dbname)
      AND (constraint_name = @constraintname)
  ) > 0,
  "SELECT 1",
  "ALTER TABLE service_schedules ADD UNIQUE KEY uk_year (year)"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加索引以提升查詢效能（如果不存在）
SET @indexname = "idx_year";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (table_name = 'service_schedules')
      AND (table_schema = @dbname)
      AND (index_name = @indexname)
  ) > 0,
  "SELECT 1",
  "ALTER TABLE service_schedules ADD INDEX idx_year (year)"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

