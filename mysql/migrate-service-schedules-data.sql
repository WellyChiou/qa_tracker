-- 遷移 service_schedules_backup 資料到新的資料表結構
-- 執行前請確保：
-- 1. 已建立新的資料表結構（執行 church-schedule-redesign.sql）
-- 2. 已備份資料庫
-- 3. positions 表已有對應的崗位資料（computer, sound, light, live）

USE church;

-- 檢查備份表是否存在
SELECT COUNT(*) as backup_count FROM service_schedules_backup;

-- 開始遷移
-- 注意：此腳本使用 MySQL 的 JSON 函數來解析 schedule_data

-- 步驟 1: 遷移主表資料（service_schedules）
-- 使用 schedule_date + version 作為名稱
INSERT INTO service_schedules (name, created_at, updated_at)
SELECT 
    CONCAT(DATE_FORMAT(schedule_date, '%Y-%m-%d'), ' 第', version, '版') as name,
    created_at,
    updated_at
FROM service_schedules_backup
ORDER BY id;

-- 步驟 2: 遷移日期資料（service_schedule_dates）
-- 從 JSON 中提取所有唯一的日期
INSERT INTO service_schedule_dates (service_schedule_id, date, created_at, updated_at)
SELECT 
    ss.id as service_schedule_id,
    DATE(JSON_UNQUOTE(JSON_EXTRACT(schedule_data, CONCAT('$[', idx, '].date')))) as date,
    ss.created_at,
    ss.updated_at
FROM service_schedules_backup ssb
INNER JOIN service_schedules ss ON ss.name = CONCAT(DATE_FORMAT(ssb.schedule_date, '%Y-%m-%d'), ' 第', ssb.version, '版')
CROSS JOIN (
    SELECT 0 as idx UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION 
    SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION 
    SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION 
    SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION 
    SELECT 24 UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29
) as indices
WHERE JSON_EXTRACT(schedule_data, CONCAT('$[', idx, '].date')) IS NOT NULL
  AND JSON_EXTRACT(schedule_data, CONCAT('$[', idx, '].date')) != 'null'
GROUP BY ss.id, date;

-- 步驟 3: 遷移崗位配置（service_schedule_position_config）
-- 為每個日期每個崗位建立配置記錄（預設 person_count = 1）
INSERT INTO service_schedule_position_config (service_schedule_date_id, position_id, person_count, created_at, updated_at)
SELECT DISTINCT
    ssd.id as service_schedule_date_id,
    p.id as position_id,
    1 as person_count, -- 預設為 1，可以後續調整
    ssd.created_at,
    ssd.updated_at
FROM service_schedule_dates ssd
INNER JOIN service_schedules ss ON ssd.service_schedule_id = ss.id
INNER JOIN service_schedules_backup ssb ON ss.name = CONCAT(DATE_FORMAT(ssb.schedule_date, '%Y-%m-%d'), ' 第', ssb.version, '版')
CROSS JOIN positions p
WHERE p.position_code IN ('computer', 'sound', 'light', 'live')
  AND p.is_active = 1
  AND JSON_EXTRACT(ssb.schedule_data, CONCAT('$[', (
    SELECT idx FROM (
      SELECT 0 as idx UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION 
      SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION 
      SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION 
      SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION 
      SELECT 24 UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29
    ) as indices
    WHERE DATE(JSON_UNQUOTE(JSON_EXTRACT(ssb.schedule_data, CONCAT('$[', idx, '].date')))) = ssd.date
    LIMIT 1
  ), '].', p.position_code)) IS NOT NULL
  AND JSON_UNQUOTE(JSON_EXTRACT(ssb.schedule_data, CONCAT('$[', (
    SELECT idx FROM (
      SELECT 0 as idx UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION 
      SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION 
      SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION 
      SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION 
      SELECT 24 UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29
    ) as indices
    WHERE DATE(JSON_UNQUOTE(JSON_EXTRACT(ssb.schedule_data, CONCAT('$[', idx, '].date')))) = ssd.date
    LIMIT 1
  ), '].', p.position_code))) != '';

-- 步驟 4: 遷移人員分配（service_schedule_assignments）
-- 由於 SQL 處理複雜的 JSON 陣列較困難，建議使用存儲過程或應用程式邏輯
-- 以下是簡化版本（假設每個崗位只有一個人）

-- 注意：此部分需要根據實際資料結構調整
-- 如果一個崗位有多個人員，需要更複雜的邏輯

-- 先建立一個臨時表來存儲解析後的資料
CREATE TEMPORARY TABLE IF NOT EXISTS temp_schedule_assignments (
    schedule_id BIGINT,
    date_str VARCHAR(20),
    position_code VARCHAR(50),
    person_name VARCHAR(100),
    sort_order INT
);

-- 使用存儲過程來解析 JSON 並插入資料
DELIMITER $$

CREATE PROCEDURE IF NOT EXISTS migrate_assignments()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_id BIGINT;
    DECLARE v_schedule_data JSON;
    DECLARE v_schedule_date DATE;
    DECLARE v_version INT;
    DECLARE v_new_schedule_id BIGINT;
    DECLARE v_date_str VARCHAR(20);
    DECLARE v_date_obj DATE;
    DECLARE v_schedule_date_id BIGINT;
    DECLARE v_position_code VARCHAR(50);
    DECLARE v_position_id BIGINT;
    DECLARE v_person_name VARCHAR(100);
    DECLARE v_person_id BIGINT;
    DECLARE v_config_id BIGINT;
    DECLARE v_idx INT;
    DECLARE v_json_length INT;
    DECLARE v_day_of_week VARCHAR(10);
    
    DECLARE cur CURSOR FOR 
        SELECT id, schedule_data, schedule_date, version 
        FROM service_schedules_backup;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO v_id, v_schedule_data, v_schedule_date, v_version;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- 取得新的 schedule_id
        SELECT id INTO v_new_schedule_id 
        FROM service_schedules 
        WHERE name = CONCAT(DATE_FORMAT(v_schedule_date, '%Y-%m-%d'), ' 第', v_version, '版')
        LIMIT 1;
        
        -- 取得 JSON 陣列長度
        SET v_json_length = JSON_LENGTH(v_schedule_data);
        SET v_idx = 0;
        
        -- 遍歷 JSON 陣列
        WHILE v_idx < v_json_length DO
            SET v_date_str = JSON_UNQUOTE(JSON_EXTRACT(v_schedule_data, CONCAT('$[', v_idx, '].date')));
            SET v_date_obj = DATE(v_date_str);
            SET v_day_of_week = JSON_UNQUOTE(JSON_EXTRACT(v_schedule_data, CONCAT('$[', v_idx, '].dayOfWeek')));
            
            -- 取得 schedule_date_id
            SELECT id INTO v_schedule_date_id 
            FROM service_schedule_dates 
            WHERE service_schedule_id = v_new_schedule_id AND date = v_date_obj
            LIMIT 1;
            
            -- 處理每個崗位
            SET v_position_code = 'computer';
            WHILE v_position_code IN ('computer', 'sound', 'light', 'live') DO
                SET v_person_name = JSON_UNQUOTE(JSON_EXTRACT(v_schedule_data, CONCAT('$[', v_idx, '].', v_position_code)));
                
                IF v_person_name IS NOT NULL AND v_person_name != '' THEN
                    -- 取得 position_id
                    SELECT id INTO v_position_id 
                    FROM positions 
                    WHERE position_code = v_position_code AND is_active = 1
                    LIMIT 1;
                    
                    -- 取得或建立 position_config
                    SELECT id INTO v_config_id 
                    FROM service_schedule_position_config 
                    WHERE service_schedule_date_id = v_schedule_date_id AND position_id = v_position_id
                    LIMIT 1;
                    
                    IF v_config_id IS NULL THEN
                        -- 建立 position_config
                        INSERT INTO service_schedule_position_config 
                            (service_schedule_date_id, position_id, person_count, created_at, updated_at)
                        VALUES (v_schedule_date_id, v_position_id, 1, NOW(), NOW());
                        SET v_config_id = LAST_INSERT_ID();
                    END IF;
                    
                    -- 取得 person_id（根據 person_name 或 display_name）
                    SELECT id INTO v_person_id 
                    FROM persons 
                    WHERE (person_name = v_person_name OR display_name = v_person_name) AND is_active = 1
                    LIMIT 1;
                    
                    -- 如果人員存在，建立 assignment
                    IF v_person_id IS NOT NULL THEN
                        INSERT INTO service_schedule_assignments 
                            (service_schedule_position_config_id, person_id, sort_order, created_at, updated_at)
                        VALUES (v_config_id, v_person_id, 0, NOW(), NOW())
                        ON DUPLICATE KEY UPDATE person_id = v_person_id;
                    END IF;
                END IF;
                
                -- 下一個崗位
                CASE v_position_code
                    WHEN 'computer' THEN SET v_position_code = 'sound';
                    WHEN 'sound' THEN SET v_position_code = 'light';
                    WHEN 'light' THEN SET v_position_code = 'live';
                    ELSE SET v_position_code = '';
                END CASE;
            END WHILE;
            
            SET v_idx = v_idx + 1;
        END WHILE;
    END LOOP;
    
    CLOSE cur;
END$$

DELIMITER ;

-- 執行遷移
CALL migrate_assignments();

-- 刪除存儲過程
DROP PROCEDURE IF EXISTS migrate_assignments;

-- 驗證遷移結果
SELECT 
    (SELECT COUNT(*) FROM service_schedules_backup) as old_count,
    (SELECT COUNT(*) FROM service_schedules) as new_schedule_count,
    (SELECT COUNT(*) FROM service_schedule_dates) as new_date_count,
    (SELECT COUNT(*) FROM service_schedule_position_config) as new_config_count,
    (SELECT COUNT(*) FROM service_schedule_assignments) as new_assignment_count;

-- 顯示一些範例資料
SELECT '=== 範例：新的服事表 ===' as info;
SELECT id, name, created_at FROM service_schedules LIMIT 5;

SELECT '=== 範例：日期明細 ===' as info;
SELECT ssd.id, ss.name, ssd.date, ssd.day_of_week_label 
FROM service_schedule_dates ssd
INNER JOIN service_schedules ss ON ssd.service_schedule_id = ss.id
LIMIT 5;

SELECT '=== 範例：人員分配 ===' as info;
SELECT 
    ss.name as schedule_name,
    ssd.date,
    p.position_name,
    per.person_name,
    ssa.sort_order
FROM service_schedule_assignments ssa
INNER JOIN service_schedule_position_config sspc ON ssa.service_schedule_position_config_id = sspc.id
INNER JOIN service_schedule_dates ssd ON sspc.service_schedule_date_id = ssd.id
INNER JOIN service_schedules ss ON ssd.service_schedule_id = ss.id
INNER JOIN positions p ON sspc.position_id = p.id
LEFT JOIN persons per ON ssa.person_id = per.id
LIMIT 10;

