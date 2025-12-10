-- 為 service_schedules 表添加 year 欄位和唯一約束
-- 確保每個年度只能有一個版本的服事表

USE church;

-- 1. 添加 year 欄位（允許 NULL，以便為現有資料填充）
ALTER TABLE service_schedules 
ADD COLUMN year INT COMMENT '年度（例如：2024）' AFTER name;

-- 2. 為現有資料填充年度
-- 從 dates 關聯表中提取最早的日期作為年度依據
-- 如果沒有 dates，則使用 created_at 的年份
UPDATE service_schedules ss
LEFT JOIN (
    SELECT service_schedule_id, MIN(date) as min_date
    FROM service_schedule_dates
    GROUP BY service_schedule_id
) ssd ON ssd.service_schedule_id = ss.id
SET ss.year = COALESCE(
    YEAR(ssd.min_date),  -- 從日期明細表中提取最早日期的年份
    YEAR(ss.created_at)  -- 如果沒有日期明細，使用建立時間的年份
)
WHERE ss.year IS NULL;

-- 如果還有 NULL 值（沒有 dates 也沒有 created_at），使用當前年份
UPDATE service_schedules 
SET year = YEAR(CURDATE())
WHERE year IS NULL;

-- 3. 設置 year 欄位為 NOT NULL
ALTER TABLE service_schedules 
MODIFY COLUMN year INT NOT NULL COMMENT '年度（例如：2024）';

-- 4. 添加唯一約束，確保每個年度只有一個版本
ALTER TABLE service_schedules 
ADD UNIQUE KEY uk_year (year);

-- 5. 添加索引以提升查詢效能
ALTER TABLE service_schedules 
ADD INDEX idx_year (year);

-- 顯示結果
SELECT 'year 欄位和唯一約束已添加完成' AS message;
SELECT COUNT(*) AS total_schedules, 
       COUNT(DISTINCT year) AS distinct_years,
       GROUP_CONCAT(DISTINCT year ORDER BY year) AS years
FROM service_schedules;

