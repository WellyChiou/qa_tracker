-- 修復崗位名稱編碼問題
-- 重新插入預設崗位數據，確保使用 UTF-8 編碼

USE church;

-- 刪除現有數據（如果編碼錯誤）
DELETE FROM position_persons;
DELETE FROM positions;

-- 重新插入預設崗位（使用 UTF-8 編碼）
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

-- 驗證數據
SELECT id, position_code, position_name, HEX(position_name) as hex_name FROM positions;

