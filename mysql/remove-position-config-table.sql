-- 刪除 position_config 表（已由新的 positions, persons, position_persons 表取代）
-- 執行此 SQL 前請確保已遷移數據到新表結構

USE church;

-- 刪除 position_config 表
DROP TABLE IF EXISTS position_config;

