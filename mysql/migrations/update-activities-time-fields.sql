-- 更新 activities 表：將 activity_time 改名為 start_time，並新增 end_time 和 activity_sessions
-- 執行日期：2026-01-XX

USE qa_tracker;

-- 步驟 1: 將 activity_time 改名為 start_time
ALTER TABLE `activities`
CHANGE COLUMN `activity_time` `start_time` VARCHAR(100) NULL COMMENT '活動開始時間';

-- 步驟 2: 新增 end_time 欄位
ALTER TABLE `activities`
ADD COLUMN `end_time` VARCHAR(100) NULL COMMENT '活動結束時間' AFTER `start_time`;

-- 步驟 3: 新增 activity_sessions 欄位（JSON 格式，用於存儲多個時間段）
-- 格式範例：[{"date": "2026-01-18", "start_time": "13:00", "end_time": "16:00"}, {"date": "2026-01-25", "start_time": "13:00", "end_time": "16:00"}]
ALTER TABLE `activities`
ADD COLUMN `activity_sessions` JSON NULL COMMENT '活動時間段（JSON 陣列，用於多個上課時間的課程）' AFTER `end_time`;

