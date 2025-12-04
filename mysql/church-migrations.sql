-- ============================================
-- Church 崗位和人員管理系統 - 遷移腳本
-- ============================================
-- 此腳本用於更新現有系統，包含所有歷史修復和更新
-- 可以安全地多次執行（使用 IF NOT EXISTS 和 ON DUPLICATE KEY UPDATE）
-- ============================================

USE church;

-- 設置字符集
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- 遷移 1: 修復 positions 表的 is_active 欄位默認值
-- ============================================
-- 如果表已存在但 is_active 欄位沒有默認值，則添加
ALTER TABLE positions 
MODIFY COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否啟用（1=是，0=否）';

-- 如果表中有數據但 is_active 為 NULL，設置為 1
UPDATE positions SET is_active = 1 WHERE is_active IS NULL;

-- ============================================
-- 遷移 2: 為 position_persons 表添加 include_in_auto_schedule 字段
-- ============================================
-- 使用存儲過程來檢查並添加字段（兼容所有 MySQL 版本）

DELIMITER $$

DROP PROCEDURE IF EXISTS add_include_in_auto_schedule_column$$

CREATE PROCEDURE add_include_in_auto_schedule_column()
BEGIN
    DECLARE column_exists INT DEFAULT 0;
    
    -- 檢查字段是否存在
    SELECT COUNT(*) INTO column_exists
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'church'
    AND TABLE_NAME = 'position_persons'
    AND COLUMN_NAME = 'include_in_auto_schedule';
    
    -- 如果不存在則添加
    IF column_exists = 0 THEN
        ALTER TABLE position_persons 
        ADD COLUMN include_in_auto_schedule TINYINT(1) NOT NULL DEFAULT 1 
        COMMENT '是否參與自動分配（1=是，0=否，只在編輯模式下顯示）' 
        AFTER sort_order;
    END IF;
END$$

DELIMITER ;

-- 執行存儲過程
CALL add_include_in_auto_schedule_column();

-- 刪除臨時存儲過程
DROP PROCEDURE IF EXISTS add_include_in_auto_schedule_column;

-- 為現有數據設置默認值為 1（如果字段已存在）
UPDATE position_persons 
SET include_in_auto_schedule = 1 
WHERE include_in_auto_schedule IS NULL;

-- 添加索引（使用存儲過程檢查）
DELIMITER $$

DROP PROCEDURE IF EXISTS add_include_in_auto_schedule_index$$

CREATE PROCEDURE add_include_in_auto_schedule_index()
BEGIN
    DECLARE index_exists INT DEFAULT 0;
    
    -- 檢查索引是否存在
    SELECT COUNT(*) INTO index_exists
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = 'church'
    AND TABLE_NAME = 'position_persons'
    AND INDEX_NAME = 'idx_include_in_auto_schedule';
    
    -- 如果不存在則添加
    IF index_exists = 0 THEN
        CREATE INDEX idx_include_in_auto_schedule ON position_persons(include_in_auto_schedule);
    END IF;
END$$

DELIMITER ;

-- 執行存儲過程
CALL add_include_in_auto_schedule_index();

-- 刪除臨時存儲過程
DROP PROCEDURE IF EXISTS add_include_in_auto_schedule_index;

-- ============================================
-- 遷移 3: 更新預設崗位數據（確保編碼正確）
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
-- 遷移 4: 刪除舊的 position_config 表（如果存在）
-- ============================================
-- 注意：執行此操作前請確保已遷移數據到新表結構
-- 如果還有數據需要保留，請先執行 migrate-position-data.sql
DROP TABLE IF EXISTS position_config;

-- ============================================
-- 完成
-- ============================================
SELECT 'Church 崗位和人員管理系統遷移完成！' AS message;

