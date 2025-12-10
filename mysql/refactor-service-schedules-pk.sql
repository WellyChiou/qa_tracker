-- 重構 service_schedules 表：移除 id 和 name，將 year 設為主鍵
-- 同時更新所有外鍵關聯

USE church;

-- 步驟1：為 service_schedule_dates 表添加新欄位 service_schedule_year
ALTER TABLE service_schedule_dates 
ADD COLUMN service_schedule_year INT COMMENT '服事表年度（外鍵）' AFTER service_schedule_id;

-- 步驟2：填充 service_schedule_year 欄位（從 service_schedules 表獲取對應的 year）
UPDATE service_schedule_dates ssd
INNER JOIN service_schedules ss ON ssd.service_schedule_id = ss.id
SET ssd.service_schedule_year = ss.year;

-- 步驟3：確保所有記錄都有 year 值（如果沒有，設置為當前年份）
UPDATE service_schedule_dates 
SET service_schedule_year = YEAR(CURDATE())
WHERE service_schedule_year IS NULL;

-- 步驟4：設置 service_schedule_year 為 NOT NULL
ALTER TABLE service_schedule_dates 
MODIFY COLUMN service_schedule_year INT NOT NULL COMMENT '服事表年度（外鍵）';

-- 步驟5：刪除舊的外鍵約束（如果存在）
-- 注意：需要先查找外鍵名稱
SET @fk_name = (
    SELECT CONSTRAINT_NAME 
    FROM information_schema.KEY_COLUMN_USAGE 
    WHERE TABLE_SCHEMA = 'church' 
    AND TABLE_NAME = 'service_schedule_dates' 
    AND COLUMN_NAME = 'service_schedule_id'
    AND REFERENCED_TABLE_NAME = 'service_schedules'
    LIMIT 1
);

SET @sql = IF(@fk_name IS NOT NULL, 
    CONCAT('ALTER TABLE service_schedule_dates DROP FOREIGN KEY ', @fk_name), 
    'SELECT "No foreign key found"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 步驟6：刪除舊的 service_schedule_id 欄位
ALTER TABLE service_schedule_dates 
DROP COLUMN service_schedule_id;

-- 步驟7：添加新的外鍵約束（引用 service_schedules.year）
ALTER TABLE service_schedule_dates 
ADD CONSTRAINT fk_service_schedule_dates_year 
FOREIGN KEY (service_schedule_year) 
REFERENCES service_schedules(year) 
ON DELETE CASCADE 
ON UPDATE CASCADE;

-- 步驟8：為新外鍵添加索引
ALTER TABLE service_schedule_dates 
ADD INDEX idx_service_schedule_year (service_schedule_year);

-- 步驟9：移除 service_schedules 表的 name 欄位
ALTER TABLE service_schedules 
DROP COLUMN name;
