-- 為直播崗位添加人員：週六世維、週日君豪
USE church;

-- 1. 確保直播崗位存在
INSERT INTO positions (position_code, position_name, description, sort_order, is_active)
VALUES ('live', '直播', '負責直播操作', 4, 1)
ON DUPLICATE KEY UPDATE
    position_name = VALUES(position_name),
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    is_active = VALUES(is_active);

-- 2. 確保人員存在（如果不存在則創建）
-- 世維
INSERT INTO persons (person_name, display_name, is_active)
SELECT '世維', '世維', 1
WHERE NOT EXISTS (SELECT 1 FROM persons WHERE person_name = '世維');

-- 君豪
INSERT INTO persons (person_name, display_name, is_active)
SELECT '君豪', '君豪', 1
WHERE NOT EXISTS (SELECT 1 FROM persons WHERE person_name = '君豪');

-- 3. 獲取崗位和人員的 ID（用於後續插入）
SET @live_position_id = (SELECT id FROM positions WHERE position_code = 'live');
SET @shivei_person_id = (SELECT id FROM persons WHERE person_name = '世維');
SET @junhao_person_id = (SELECT id FROM persons WHERE person_name = '君豪');

-- 4. 添加崗位人員關聯
-- 週六：世維
INSERT INTO position_persons (position_id, person_id, day_type, sort_order)
VALUES (@live_position_id, @shivei_person_id, 'saturday', 0)
ON DUPLICATE KEY UPDATE
    sort_order = VALUES(sort_order),
    updated_at = NOW();

-- 週日：君豪
INSERT INTO position_persons (position_id, person_id, day_type, sort_order)
VALUES (@live_position_id, @junhao_person_id, 'sunday', 0)
ON DUPLICATE KEY UPDATE
    sort_order = VALUES(sort_order),
    updated_at = NOW();

-- 5. 顯示插入結果
SELECT 
    pp.id,
    p.position_name AS '崗位',
    per.person_name AS '人員',
    pp.day_type AS '日期類型',
    pp.sort_order AS '排序'
FROM position_persons pp
JOIN positions p ON pp.position_id = p.id
JOIN persons per ON pp.person_id = per.id
WHERE p.position_code = 'live'
ORDER BY pp.day_type, pp.sort_order;

