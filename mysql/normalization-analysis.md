# 資料庫正規化分析

## 正規化檢查結果

### ✅ 第一正規化（1NF）- 通過
所有表的欄位都是原子值，沒有重複群組。

### ✅ 第二正規化（2NF）- 通過
所有非主鍵欄位都完全依賴於主鍵。

### ⚠️ 第三正規化（3NF）- 有一個問題

#### 問題：`service_schedule_dates.day_of_week` 違反 3NF

**問題說明：**
- `day_of_week` 可以從 `date` 欄位計算出來（遞移依賴）
- 違反第三正規化：非主鍵欄位不應該依賴於其他非主鍵欄位

**影響：**
- 如果 `date` 和 `day_of_week` 不一致，會造成資料不一致
- 需要額外的驗證邏輯確保一致性

**建議：**
1. **選項 A（推薦）**：移除 `day_of_week` 欄位，在應用層計算
   - 優點：符合正規化，避免資料不一致
   - 缺點：每次查詢需要計算（但效能影響很小）

2. **選項 B**：保留 `day_of_week` 作為計算欄位（Computed Column）
   - MySQL 5.7+ 支援 GENERATED COLUMN
   - 優點：自動計算，保證一致性，查詢效能好
   - 缺點：需要 MySQL 5.7+

3. **選項 C**：保留 `day_of_week`，但加入 CHECK 約束
   - 優點：保留欄位，查詢效能好
   - 缺點：需要額外的驗證邏輯

## 其他設計檢查

### ✅ 外鍵約束
所有外鍵關係都正確定義，使用 CASCADE 或 SET NULL 適當處理。

### ✅ 唯一約束
適當使用 UNIQUE KEY 防止重複資料：
- `service_schedule_dates`: (service_schedule_id, date)
- `service_schedule_position_config`: (service_schedule_date_id, position_id)
- `position_persons`: (position_id, person_id, day_type)

### ✅ 索引設計
適當的索引設計，包含：
- 主鍵索引
- 外鍵索引
- 查詢常用欄位索引

## 建議修正

建議使用 **選項 B（GENERATED COLUMN）**，既符合正規化又保持查詢效能。

