-- 修正 service_schedules 表結構
-- 如果表已存在但結構不對（有舊欄位），執行此腳本重建表

USE church;

-- 步驟 1: 備份現有資料（如果有的話）
CREATE TABLE IF NOT EXISTS service_schedules_temp_backup AS 
SELECT * FROM service_schedules WHERE 1=0; -- 只建立結構，不複製資料

-- 如果有資料需要保留，取消註解下面這行
-- INSERT INTO service_schedules_temp_backup SELECT * FROM service_schedules;

-- 步驟 2: 刪除相關表（注意：這會刪除所有資料！）
-- 如果確定要重建，取消註解以下行
/*
DROP TABLE IF EXISTS service_schedule_assignments;
DROP TABLE IF EXISTS service_schedule_position_config;
DROP TABLE IF EXISTS service_schedule_dates;
DROP TABLE IF EXISTS service_schedules;
*/

-- 步驟 3: 重新建立正確的表結構
-- 執行 church-schedule-redesign.sql 中的表定義

-- 或者直接執行以下 SQL：

-- 建立新的 service_schedules 表（主表）
CREATE TABLE IF NOT EXISTS service_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    name VARCHAR(255) NOT NULL COMMENT '使用者自訂義名稱',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_name (name),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服事安排表（主表）';

-- 檢查表結構
SELECT '檢查新的表結構...' as step;
DESCRIBE service_schedules;

SELECT '表結構修正完成！' as message;

