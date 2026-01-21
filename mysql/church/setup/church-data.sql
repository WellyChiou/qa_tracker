-- ============================================
-- Church 崗位和人員管理系統 - 初始數據腳本
-- ============================================
-- 此腳本用於插入初始的崗位和人員數據
-- 可以安全地多次執行（使用 ON DUPLICATE KEY UPDATE）
-- ============================================

USE church;

-- 設置字符集
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- 1. 插入人員數據
-- ============================================
INSERT INTO persons (person_name, display_name, is_active) VALUES
('世維', '世維', 1),
('佑庭', '佑庭', 1),
('朝翔', '朝翔', 1),
('君豪', '君豪', 1),
('家偉', '家偉', 1),
('斌浩', '斌浩', 1),
('桓瑜', '桓瑜', 1),
('阿皮', '阿皮', 1),
('佳佳', '佳佳', 1),
('鈞顯', '鈞顯', 1),
('曹格', '曹格', 1),
('榮一', '榮一', 1)
ON DUPLICATE KEY UPDATE 
    display_name = VALUES(display_name),
    is_active = VALUES(is_active);

-- ============================================
-- 2. 確保崗位已存在
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
-- 3. 插入崗位人員關聯數據
-- ============================================
-- 使用臨時表來簡化插入邏輯
CREATE TEMPORARY TABLE IF NOT EXISTS temp_position_persons (
    position_code VARCHAR(50),
    person_name VARCHAR(100),
    day_type VARCHAR(20),
    sort_order INT,
    include_in_auto_schedule TINYINT(1) DEFAULT 1
);

-- 清空臨時表（如果已存在數據）
TRUNCATE TABLE temp_position_persons;

-- 插入臨時數據
INSERT INTO temp_position_persons (position_code, person_name, day_type, sort_order, include_in_auto_schedule) VALUES
-- 電腦崗位
('computer', '世維', 'saturday', 1, 1),
('computer', '佑庭', 'saturday', 2, 1),
('computer', '朝翔', 'saturday', 3, 1),
('computer', '君豪', 'sunday', 1, 1),
('computer', '家偉', 'sunday', 2, 1),
('computer', '佑庭', 'sunday', 3, 1),
-- 混音崗位
('sound', '斌浩', 'saturday', 1, 1),
('sound', '世維', 'saturday', 2, 1),
('sound', '佑庭', 'saturday', 3, 1),
('sound', '桓瑜', 'saturday', 4, 1),
('sound', '朝翔', 'saturday', 5, 1),
('sound', '朝翔', 'sunday', 1, 1),
('sound', '君豪', 'sunday', 2, 1),
('sound', '家偉', 'sunday', 3, 1),
('sound', '佑庭', 'sunday', 4, 1),
-- 燈光崗位
('light', '阿皮', 'saturday', 1, 1),
('light', '佑庭', 'saturday', 2, 1),
('light', '桓瑜', 'saturday', 3, 1),
('light', '佳佳', 'sunday', 1, 1),
('light', '鈞顯', 'sunday', 2, 1),
('light', '曹格', 'sunday', 3, 1),
('light', '佑庭', 'sunday', 4, 1),
('light', '榮一', 'sunday', 5, 1),
-- 直播崗位（週六世維、週日君豪）
('live', '世維', 'saturday', 0, 1),
('live', '君豪', 'sunday', 0, 1);

-- 從臨時表插入到 position_persons
INSERT INTO position_persons (position_id, person_id, day_type, sort_order, include_in_auto_schedule)
SELECT 
    p.id as position_id,
    per.id as person_id,
    t.day_type,
    t.sort_order,
    t.include_in_auto_schedule
FROM temp_position_persons t
JOIN positions p ON p.position_code = t.position_code
JOIN persons per ON per.person_name = t.person_name
ON DUPLICATE KEY UPDATE 
    sort_order = VALUES(sort_order),
    include_in_auto_schedule = VALUES(include_in_auto_schedule),
    updated_at = NOW();

-- 清理臨時表
DROP TEMPORARY TABLE IF EXISTS temp_position_persons;

-- ============================================
-- 4. 驗證數據
-- ============================================
SELECT 
    p.position_name as '崗位',
    pp.day_type as '日期',
    GROUP_CONCAT(per.person_name ORDER BY pp.sort_order SEPARATOR ', ') as '人員',
    COUNT(*) as '人數'
FROM position_persons pp
JOIN positions p ON pp.position_id = p.id
JOIN persons per ON pp.person_id = per.id
GROUP BY p.position_name, pp.day_type
ORDER BY p.sort_order, pp.day_type;

-- ============================================
-- 完成
-- ============================================
SELECT 'Church 崗位和人員管理系統初始數據插入完成！' AS message;

