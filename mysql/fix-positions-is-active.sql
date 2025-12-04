-- 修復 positions 表的 is_active 欄位默認值問題

USE church;

-- 修改 is_active 欄位，添加默認值
ALTER TABLE positions 
MODIFY COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否啟用（1=是，0=否）';

-- 如果表中有數據但 is_active 為 NULL，設置為 1
UPDATE positions SET is_active = 1 WHERE is_active IS NULL;

-- 重新插入預設崗位（包含 is_active 值）
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
SELECT id, position_code, position_name, is_active FROM positions;

