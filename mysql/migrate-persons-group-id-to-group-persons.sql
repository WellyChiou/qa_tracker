-- 數據遷移：將現有 persons.group_id 數據遷移到 group_persons 表
-- 確保 joined_at 日期正確，設置 is_active = 1

USE church;

-- 1. 為 group_persons 表添加歷史記錄支援欄位（如果還沒有添加）
-- 注意：這應該已經在 add-group-person-history-fields.sql 中執行過了
-- 這裡只是為了確保完整性

-- 2. 將現有 persons.group_id 的數據遷移到 group_persons 表
INSERT IGNORE INTO group_persons (group_id, person_id, joined_at, is_active, created_at, updated_at)
SELECT 
    p.group_id,
    p.id,
    COALESCE(p.created_at, CURDATE()) AS joined_at, -- 如果沒有 created_at，使用當前日期
    1 AS is_active, -- 所有現有記錄都設為活躍
    NOW() AS created_at,
    NOW() AS updated_at
FROM persons p
WHERE p.group_id IS NOT NULL
  AND NOT EXISTS (
      -- 避免重複插入（如果已經存在關聯）
      SELECT 1 FROM group_persons gp 
      WHERE gp.group_id = p.group_id 
        AND gp.person_id = p.id
  );

-- 3. 確保所有現有記錄的 is_active 為 1
UPDATE group_persons 
SET is_active = 1 
WHERE is_active IS NULL OR is_active = 0;

-- 4. 顯示遷移結果
SELECT 
    COUNT(*) AS total_migrated,
    COUNT(DISTINCT person_id) AS unique_persons,
    COUNT(DISTINCT group_id) AS unique_groups
FROM group_persons
WHERE is_active = 1;

