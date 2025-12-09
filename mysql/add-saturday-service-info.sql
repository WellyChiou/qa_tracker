-- ============================================
-- 添加禮拜六晚崇聚會資訊到 church_info 表
-- ============================================
-- 此腳本用於添加禮拜六晚崇聚會的資訊
-- 可以安全地多次執行（使用 INSERT IGNORE）
-- ============================================

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 插入禮拜六晚崇聚會資訊
INSERT IGNORE INTO church_info (info_key, info_value, display_order) VALUES
('home_saturday_service_time', '晚崇聚會：每週六晚上 7:00', 14),
('home_saturday_service_location', '教會大堂', 15);

-- 如果已經存在，則更新
UPDATE church_info 
SET 
    info_value = CASE 
        WHEN info_key = 'home_saturday_service_time' THEN '晚崇聚會：每週六晚上 7:00'
        WHEN info_key = 'home_saturday_service_location' THEN '教會大堂'
        ELSE info_value
    END,
    display_order = CASE 
        WHEN info_key = 'home_saturday_service_time' THEN 14
        WHEN info_key = 'home_saturday_service_location' THEN 15
        ELSE display_order
    END,
    updated_at = NOW()
WHERE info_key IN ('home_saturday_service_time', 'home_saturday_service_location');

-- 顯示結果
SELECT '禮拜六晚崇聚會資訊已添加/更新' AS message;
SELECT info_key, info_value, display_order, is_active
FROM church_info
WHERE info_key IN ('home_saturday_service_time', 'home_saturday_service_location')
ORDER BY display_order;
