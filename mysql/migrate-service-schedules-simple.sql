-- 簡化版：遷移 service_schedules_backup 資料到新的資料表結構
-- 此版本假設 schedule_data 是 JSON 陣列，每個元素包含 date, computer, sound, light, live 等欄位
-- 
-- 執行步驟：
-- 1. 確保已執行 church-schedule-redesign.sql 建立新表
-- 2. 確保 positions 表有對應的崗位（computer, sound, light, live）
-- 3. 執行此腳本

USE church;

-- 檢查備份表
SELECT '檢查備份表...' as step;
SELECT COUNT(*) as backup_count FROM service_schedules_backup;

-- 檢查表結構
SELECT '檢查表結構...' as step;
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'church' 
  AND TABLE_NAME = 'service_schedules'
ORDER BY ORDINAL_POSITION;

-- 如果表有舊欄位（schedule_date, version, start_date, end_date, schedule_data），
-- 請先執行 fix-service-schedules-table.sql 重建表結構
-- 或者手動執行以下 SQL：
/*
-- 備份現有資料
CREATE TABLE IF NOT EXISTS service_schedules_old_backup AS SELECT * FROM service_schedules;

-- 刪除舊表
DROP TABLE IF EXISTS service_schedule_assignments;
DROP TABLE IF EXISTS service_schedule_position_config;
DROP TABLE IF EXISTS service_schedule_dates;
DROP TABLE IF EXISTS service_schedules;

-- 然後執行 church-schedule-redesign.sql 重新建立表
*/

-- 步驟 1: 遷移主表（service_schedules）
-- 使用 schedule_date + version 作為名稱
SELECT '步驟 1: 遷移主表...' as step;

-- 檢查表結構是否正確（應該只有 id, name, created_at, updated_at）
SET @column_count = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'church' 
      AND TABLE_NAME = 'service_schedules'
);

SET @old_column_count = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'church' 
      AND TABLE_NAME = 'service_schedules' 
      AND COLUMN_NAME IN ('schedule_date', 'version', 'start_date', 'end_date', 'schedule_data')
);

-- 如果表結構不正確，顯示錯誤訊息
SELECT 
    CASE 
        WHEN @old_column_count > 0 THEN 
            CONCAT('錯誤：表結構不正確，發現 ', @old_column_count, ' 個舊欄位。請先執行 fix-service-schedules-table.sql 重建表結構。')
        WHEN @column_count = 0 THEN 
            '錯誤：service_schedules 表不存在。請先執行 church-schedule-redesign.sql 建立表。'
        WHEN @column_count != 4 THEN 
            CONCAT('警告：表結構可能不正確，預期 4 個欄位，實際 ', @column_count, ' 個。')
        ELSE '表結構正確，開始遷移資料...'
    END as structure_check;

-- 如果表結構正確，執行插入
-- 注意：如果表結構不對，此 INSERT 會失敗
INSERT INTO service_schedules (name, created_at, updated_at)
SELECT 
    CONCAT(DATE_FORMAT(schedule_date, '%Y-%m-%d'), ' 第', version, '版') as name,
    created_at,
    updated_at
FROM service_schedules_backup
WHERE NOT EXISTS (
    SELECT 1 FROM service_schedules 
    WHERE name = CONCAT(DATE_FORMAT(schedule_date, '%Y-%m-%d'), ' 第', version, '版')
)
ORDER BY id;

SELECT '主表遷移完成，共 ', COUNT(*) as count FROM service_schedules;

-- 步驟 2: 使用存儲過程遷移日期和人員分配
-- 由於 JSON 解析較複雜，使用存儲過程處理

DELIMITER $$

DROP PROCEDURE IF EXISTS migrate_schedule_data$$

CREATE PROCEDURE migrate_schedule_data()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_old_id BIGINT;
    DECLARE v_schedule_data JSON;
    DECLARE v_schedule_date DATE;
    DECLARE v_version INT;
    DECLARE v_created_at DATETIME;
    DECLARE v_new_schedule_id BIGINT;
    DECLARE v_schedule_name VARCHAR(255);
    
    DECLARE v_json_length INT;
    DECLARE v_idx INT;
    DECLARE v_date_str VARCHAR(20);
    DECLARE v_date_obj DATE;
    DECLARE v_schedule_date_id BIGINT;
    DECLARE v_day_of_week_str VARCHAR(10);
    
    DECLARE v_position_code VARCHAR(50);
    DECLARE v_position_id BIGINT;
    DECLARE v_person_name VARCHAR(100);
    DECLARE v_person_id BIGINT;
    DECLARE v_config_id BIGINT;
    DECLARE v_sort_order INT;
    
    DECLARE cur CURSOR FOR 
        SELECT id, schedule_data, schedule_date, version, created_at
        FROM service_schedules_backup
        ORDER BY id;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO v_old_id, v_schedule_data, v_schedule_date, v_version, v_created_at;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- 建立服事表名稱
        SET v_schedule_name = CONCAT(DATE_FORMAT(v_schedule_date, '%Y-%m-%d'), ' 第', v_version, '版');
        
        -- 取得新的 schedule_id
        SELECT id INTO v_new_schedule_id 
        FROM service_schedules 
        WHERE name = v_schedule_name
        LIMIT 1;
        
        IF v_new_schedule_id IS NULL THEN
            -- 如果找不到，建立新的
            INSERT INTO service_schedules (name, created_at, updated_at)
            VALUES (v_schedule_name, v_created_at, v_created_at);
            SET v_new_schedule_id = LAST_INSERT_ID();
        END IF;
        
        -- 取得 JSON 陣列長度
        SET v_json_length = JSON_LENGTH(v_schedule_data);
        SET v_idx = 0;
        
        -- 遍歷 JSON 陣列的每個日期
        WHILE v_idx < v_json_length DO
            -- 取得日期
            SET v_date_str = JSON_UNQUOTE(JSON_EXTRACT(v_schedule_data, CONCAT('$[', v_idx, '].date')));
            
            IF v_date_str IS NOT NULL AND v_date_str != '' AND v_date_str != 'null' THEN
                SET v_date_obj = DATE(v_date_str);
                SET v_day_of_week_str = JSON_UNQUOTE(JSON_EXTRACT(v_schedule_data, CONCAT('$[', v_idx, '].dayOfWeek')));
                
                -- 取得或建立 schedule_date
                SELECT id INTO v_schedule_date_id 
                FROM service_schedule_dates 
                WHERE service_schedule_id = v_new_schedule_id AND date = v_date_obj
                LIMIT 1;
                
                IF v_schedule_date_id IS NULL THEN
                    -- 建立新的 schedule_date（day_of_week 會自動計算）
                    INSERT INTO service_schedule_dates 
                        (service_schedule_id, date, created_at, updated_at)
                    VALUES (v_new_schedule_id, v_date_obj, v_created_at, v_created_at);
                    SET v_schedule_date_id = LAST_INSERT_ID();
                END IF;
                
                -- 處理每個崗位：computer, sound, light, live
                SET v_position_code = 'computer';
                SET v_sort_order = 0;
                
                position_loop: LOOP
                    -- 取得該崗位的人員名稱
                    SET v_person_name = JSON_UNQUOTE(JSON_EXTRACT(v_schedule_data, CONCAT('$[', v_idx, '].', v_position_code)));
                    
                    IF v_person_name IS NOT NULL AND v_person_name != '' AND v_person_name != 'null' THEN
                        -- 取得 position_id
                        SELECT id INTO v_position_id 
                        FROM positions 
                        WHERE position_code = v_position_code AND is_active = 1
                        LIMIT 1;
                        
                        IF v_position_id IS NOT NULL THEN
                            -- 取得或建立 position_config
                            SELECT id INTO v_config_id 
                            FROM service_schedule_position_config 
                            WHERE service_schedule_date_id = v_schedule_date_id 
                              AND position_id = v_position_id
                            LIMIT 1;
                            
                            IF v_config_id IS NULL THEN
                                -- 建立 position_config
                                INSERT INTO service_schedule_position_config 
                                    (service_schedule_date_id, position_id, person_count, created_at, updated_at)
                                VALUES (v_schedule_date_id, v_position_id, 1, v_created_at, v_created_at);
                                SET v_config_id = LAST_INSERT_ID();
                            END IF;
                            
                            -- 嘗試根據 person_name 或 display_name 找到 person_id
                            SELECT id INTO v_person_id 
                            FROM persons 
                            WHERE (person_name = v_person_name OR display_name = v_person_name) 
                              AND is_active = 1
                            LIMIT 1;
                            
                            -- 如果找到人員，建立 assignment
                            IF v_person_id IS NOT NULL THEN
                                INSERT INTO service_schedule_assignments 
                                    (service_schedule_position_config_id, person_id, sort_order, created_at, updated_at)
                                VALUES (v_config_id, v_person_id, v_sort_order, v_created_at, v_created_at);
                                SET v_sort_order = v_sort_order + 1;
                            END IF;
                        END IF;
                    END IF;
                    
                    -- 下一個崗位
                    CASE v_position_code
                        WHEN 'computer' THEN SET v_position_code = 'sound';
                        WHEN 'sound' THEN SET v_position_code = 'light';
                        WHEN 'light' THEN SET v_position_code = 'live';
                        ELSE LEAVE position_loop;
                    END CASE;
                END LOOP;
            END IF;
            
            SET v_idx = v_idx + 1;
        END WHILE;
    END LOOP;
    
    CLOSE cur;
END$$

DELIMITER ;

-- 執行遷移
SELECT '步驟 2: 開始遷移日期和人員分配...' as step;
CALL migrate_schedule_data();

-- 清理
DROP PROCEDURE IF EXISTS migrate_schedule_data;

-- 驗證結果
SELECT '=== 遷移完成，驗證結果 ===' as info;
SELECT 
    (SELECT COUNT(*) FROM service_schedules_backup) as old_count,
    (SELECT COUNT(*) FROM service_schedules) as new_schedule_count,
    (SELECT COUNT(*) FROM service_schedule_dates) as new_date_count,
    (SELECT COUNT(*) FROM service_schedule_position_config) as new_config_count,
    (SELECT COUNT(*) FROM service_schedule_assignments) as new_assignment_count;

-- 顯示範例資料
SELECT '=== 範例：新的服事表 ===' as info;
SELECT id, name, created_at FROM service_schedules ORDER BY created_at DESC LIMIT 5;

SELECT '=== 範例：日期明細 ===' as info;
SELECT 
    ssd.id, 
    ss.name as schedule_name, 
    ssd.date, 
    ssd.day_of_week_label as day_of_week
FROM service_schedule_dates ssd
INNER JOIN service_schedules ss ON ssd.service_schedule_id = ss.id
ORDER BY ssd.date DESC
LIMIT 5;

SELECT '=== 範例：人員分配 ===' as info;
SELECT 
    ss.name as schedule_name,
    ssd.date,
    p.position_name,
    COALESCE(per.person_name, per.display_name, '未分配') as person_name,
    ssa.sort_order
FROM service_schedule_assignments ssa
INNER JOIN service_schedule_position_config sspc ON ssa.service_schedule_position_config_id = sspc.id
INNER JOIN service_schedule_dates ssd ON sspc.service_schedule_date_id = ssd.id
INNER JOIN service_schedules ss ON ssd.service_schedule_id = ss.id
INNER JOIN positions p ON sspc.position_id = p.id
LEFT JOIN persons per ON ssa.person_id = per.id
ORDER BY ssd.date DESC, p.sort_order, ssa.sort_order
LIMIT 10;

