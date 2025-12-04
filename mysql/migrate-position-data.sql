-- 遷移崗位人員配置數據到新的表結構
-- 根據 fix-position-config-encoding.sql 中的配置生成

USE church;

-- 設置會話字符集為 utf8mb4
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 1. 先插入所有人員（去重）
-- 從配置中提取的所有人員名稱
INSERT INTO persons (person_name, is_active) VALUES
('世維', 1),
('佑庭', 1),
('朝翔', 1),
('君豪', 1),
('家偉', 1),
('斌浩', 1),
('桓瑜', 1),
('阿皮', 1),
('佳佳', 1),
('鈞顯', 1),
('曹格', 1),
('榮一', 1)
ON DUPLICATE KEY UPDATE person_name = VALUES(person_name);

-- 2. 確保崗位已存在（如果還沒有）
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

-- 3. 清空現有的崗位人員關聯（可選，如果需要重新建立）
-- DELETE FROM position_persons;

-- 4. 插入崗位人員關聯
-- 使用臨時表來簡化插入邏輯
CREATE TEMPORARY TABLE IF NOT EXISTS temp_position_persons (
    position_code VARCHAR(50),
    person_name VARCHAR(100),
    day_type VARCHAR(20),
    sort_order INT
);

-- 插入臨時數據
INSERT INTO temp_position_persons (position_code, person_name, day_type, sort_order) VALUES
-- 電腦崗位
('computer', '世維', 'saturday', 1),
('computer', '佑庭', 'saturday', 2),
('computer', '朝翔', 'saturday', 3),
('computer', '君豪', 'sunday', 1),
('computer', '家偉', 'sunday', 2),
('computer', '佑庭', 'sunday', 3),
-- 混音崗位
('sound', '斌浩', 'saturday', 1),
('sound', '世維', 'saturday', 2),
('sound', '佑庭', 'saturday', 3),
('sound', '桓瑜', 'saturday', 4),
('sound', '朝翔', 'saturday', 5),
('sound', '朝翔', 'sunday', 1),
('sound', '君豪', 'sunday', 2),
('sound', '家偉', 'sunday', 3),
('sound', '佑庭', 'sunday', 4),
-- 燈光崗位
('light', '阿皮', 'saturday', 1),
('light', '佑庭', 'saturday', 2),
('light', '桓瑜', 'saturday', 3),
('light', '佳佳', 'sunday', 1),
('light', '鈞顯', 'sunday', 2),
('light', '曹格', 'sunday', 3),
('light', '佑庭', 'sunday', 4),
('light', '榮一', 'sunday', 5);

-- 從臨時表插入到 position_persons
INSERT INTO position_persons (position_id, person_id, day_type, sort_order)
SELECT 
    p.id as position_id,
    per.id as person_id,
    t.day_type,
    t.sort_order
FROM temp_position_persons t
JOIN positions p ON p.position_code = t.position_code
JOIN persons per ON per.person_name = t.person_name
ON DUPLICATE KEY UPDATE sort_order = VALUES(sort_order);

-- 清理臨時表
DROP TEMPORARY TABLE IF EXISTS temp_position_persons;

-- 5. 驗證數據
SELECT 
    p.position_name as '崗位',
    pp.day_type as '日期',
    GROUP_CONCAT(per.person_name ORDER BY pp.sort_order SEPARATOR ', ') as '人員'
FROM position_persons pp
JOIN positions p ON pp.position_id = p.id
JOIN persons per ON pp.person_id = per.id
GROUP BY p.position_name, pp.day_type
ORDER BY p.sort_order, pp.day_type;

