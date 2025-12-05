# 服事表資料遷移指南

## 概述

此指南說明如何將舊的 `service_schedules_backup` 資料遷移到新的資料表結構。

## 新舊結構對照

### 舊結構
- `service_schedules`: id, schedule_date, version, start_date, end_date, schedule_data (JSON), created_at, updated_at
- `schedule_data` 是 JSON 陣列，包含日期和人員分配

### 新結構
- `service_schedules`: id, name, created_at, updated_at
- `service_schedule_dates`: id, service_schedule_id, date, day_of_week (自動計算), created_at, updated_at
- `service_schedule_position_config`: id, service_schedule_date_id, position_id, person_count, created_at, updated_at
- `service_schedule_assignments`: id, service_schedule_position_config_id, person_id, sort_order, created_at, updated_at

## 遷移步驟

### 1. 前置準備

```sql
-- 確認備份表存在
USE church;
SELECT COUNT(*) FROM service_schedules_backup;

-- 確認新表已建立
SHOW TABLES LIKE 'service_schedules';
SHOW TABLES LIKE 'service_schedule_dates';
SHOW TABLES LIKE 'service_schedule_position_config';
SHOW TABLES LIKE 'service_schedule_assignments';

-- 確認崗位資料存在
SELECT * FROM positions WHERE position_code IN ('computer', 'sound', 'light', 'live');
```

### 2. 執行遷移

**選項 A：使用簡化版腳本（推薦）**

```bash
mysql -uroot -prootpassword church < mysql/migrate-service-schedules-simple.sql
```

**選項 B：使用完整版腳本**

```bash
mysql -uroot -prootpassword church < mysql/migrate-service-schedules-data.sql
```

### 3. 驗證遷移結果

```sql
-- 檢查資料數量
SELECT 
    (SELECT COUNT(*) FROM service_schedules_backup) as old_count,
    (SELECT COUNT(*) FROM service_schedules) as new_schedule_count,
    (SELECT COUNT(*) FROM service_schedule_dates) as new_date_count,
    (SELECT COUNT(*) FROM service_schedule_position_config) as new_config_count,
    (SELECT COUNT(*) FROM service_schedule_assignments) as new_assignment_count;

-- 檢查範例資料
SELECT * FROM service_schedules LIMIT 5;
SELECT * FROM service_schedule_dates LIMIT 5;
SELECT * FROM service_schedule_assignments LIMIT 5;
```

## 注意事項

1. **人員名稱匹配**：
   - 遷移腳本會根據 `person_name` 或 `display_name` 匹配人員
   - 如果找不到對應的人員，該分配會被跳過
   - 建議先檢查 `persons` 表中的人員名稱是否與 JSON 中的名稱一致

2. **崗位代碼**：
   - 確保 `positions` 表中有 `computer`, `sound`, `light`, `live` 這些崗位
   - 崗位代碼必須完全匹配

3. **日期格式**：
   - JSON 中的日期格式應為 `YYYY-MM-DD`
   - `day_of_week` 會自動從 `date` 計算（GENERATED COLUMN）

4. **資料完整性**：
   - 遷移後請檢查是否有遺漏的資料
   - 特別注意人員分配是否正確

## 疑難排解

### 問題 1：找不到對應的人員

```sql
-- 檢查 JSON 中的人員名稱
SELECT 
    JSON_UNQUOTE(JSON_EXTRACT(schedule_data, CONCAT('$[0].computer'))) as computer_person,
    JSON_UNQUOTE(JSON_EXTRACT(schedule_data, CONCAT('$[0].sound'))) as sound_person
FROM service_schedules_backup
LIMIT 5;

-- 檢查 persons 表中的人員
SELECT person_name, display_name FROM persons WHERE is_active = 1;
```

### 問題 2：崗位配置未建立

```sql
-- 檢查崗位是否存在
SELECT * FROM positions WHERE position_code IN ('computer', 'sound', 'light', 'live');

-- 檢查崗位配置
SELECT * FROM service_schedule_position_config LIMIT 10;
```

### 問題 3：日期未遷移

```sql
-- 檢查 JSON 中的日期格式
SELECT 
    JSON_UNQUOTE(JSON_EXTRACT(schedule_data, CONCAT('$[0].date'))) as date_str
FROM service_schedules_backup
LIMIT 5;

-- 檢查已遷移的日期
SELECT * FROM service_schedule_dates LIMIT 10;
```

## 回滾方案

如果遷移失敗，可以刪除新資料並重新開始：

```sql
-- 刪除新資料（注意：這會刪除所有新資料！）
DELETE FROM service_schedule_assignments;
DELETE FROM service_schedule_position_config;
DELETE FROM service_schedule_dates;
DELETE FROM service_schedules;

-- 重新執行遷移腳本
```

## 後續處理

遷移完成後，建議：

1. 檢查資料完整性
2. 更新應用程式使用新的 API
3. 測試新功能
4. 確認無誤後，可以考慮刪除 `service_schedules_backup` 表（建議先保留一段時間）

