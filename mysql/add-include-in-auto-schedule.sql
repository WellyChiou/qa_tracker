-- 為 position_persons 表添加 include_in_auto_schedule 字段
USE church;

-- 添加字段，默認值為 1（參與自動分配）
ALTER TABLE position_persons 
ADD COLUMN include_in_auto_schedule TINYINT(1) NOT NULL DEFAULT 1 
COMMENT '是否參與自動分配（1=是，0=否，只在編輯模式下顯示）' 
AFTER sort_order;

-- 為現有數據設置默認值為 1
UPDATE position_persons 
SET include_in_auto_schedule = 1 
WHERE include_in_auto_schedule IS NULL;

-- 添加索引以便查詢
CREATE INDEX idx_include_in_auto_schedule ON position_persons(include_in_auto_schedule);

