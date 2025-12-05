-- 遷移現有的 service_schedules 資料到新結構
-- 注意：此腳本會將舊的 JSON 資料轉換為新的關聯表結構
-- 執行前請先備份資料庫

USE church;

-- 檢查是否有舊資料需要遷移
SELECT COUNT(*) as old_count FROM service_schedules WHERE schedule_data IS NOT NULL;

-- 如果需要遷移，可以執行以下步驟：
-- 1. 解析 schedule_data JSON
-- 2. 為每個服事表建立新的記錄（使用 schedule_date + version 作為名稱）
-- 3. 建立 service_schedule_dates 記錄
-- 4. 建立 service_schedule_assignments 記錄

-- 注意：由於舊資料結構較複雜，建議手動遷移或使用應用程式邏輯遷移

