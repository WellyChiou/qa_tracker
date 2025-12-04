# Church 崗位和人員管理系統 - SQL 腳本說明

## 文件結構

### 主要腳本（推薦使用）

1. **`church-init.sql`** - 完整初始化腳本
   - 用於全新安裝
   - 包含所有最新的表結構和字段（包括 `include_in_auto_schedule`）
   - 執行前請確保 `church` 數據庫已創建

2. **`church-migrations.sql`** - 遷移腳本
   - 用於更新現有系統
   - 包含所有歷史修復和更新
   - 可以安全地多次執行

3. **`church-data.sql`** - 初始數據腳本
   - 用於插入初始的崗位和人員數據
   - 包含預設的崗位、人員和關聯數據
   - 可以安全地多次執行（使用 `ON DUPLICATE KEY UPDATE`）

## 使用方式

### 全新安裝

```bash
# 1. 創建數據庫（如果還沒有）
docker exec mysql_db mysql -u appuser -papppassword -e "CREATE DATABASE IF NOT EXISTS church CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 執行初始化腳本
docker exec mysql_db mysql -u appuser -papppassword church < mysql/church-init.sql

# 3. 插入初始數據（可選）
docker exec mysql_db mysql -u appuser -papppassword church < mysql/church-data.sql
```

### 更新現有系統

```bash
# 執行遷移腳本（會自動檢查並應用必要的更新）
docker exec mysql_db mysql -u appuser -papppassword church < mysql/church-migrations.sql
```

## 舊文件說明（已整合，可刪除）

以下文件已整合到上述主要腳本中，可以刪除：

- `church-positions-schema.sql` → 整合到 `church-init.sql`
- `add-include-in-auto-schedule.sql` → 整合到 `church-migrations.sql`
- `fix-positions-is-active.sql` → 整合到 `church-migrations.sql`
- `fix-position-encoding.sql` → 整合到 `church-migrations.sql`
- `migrate-position-data.sql` → 整合到 `church-data.sql`
- `add-live-position-persons.sql` → 整合到 `church-data.sql`
- `remove-position-config-table.sql` → 整合到 `church-migrations.sql`
- `fix-position-config-encoding.sql` → 已廢棄（舊表結構）

## 數據庫結構

### positions（崗位表）
- `id` - 主鍵
- `position_code` - 崗位代碼（如：computer, sound, light, live）
- `position_name` - 崗位名稱（如：電腦、混音、燈光、直播）
- `description` - 崗位描述
- `is_active` - 是否啟用
- `sort_order` - 排序順序

### persons（人員表）
- `id` - 主鍵
- `person_name` - 人員姓名
- `display_name` - 顯示名稱（可選）
- `phone` - 電話
- `email` - 電子郵件
- `notes` - 備註
- `is_active` - 是否啟用

### position_persons（崗位人員關聯表）
- `id` - 主鍵
- `position_id` - 崗位 ID
- `person_id` - 人員 ID
- `day_type` - 日期類型（saturday 或 sunday）
- `sort_order` - 排序順序
- `include_in_auto_schedule` - 是否參與自動分配（1=是，0=否）

## 注意事項

1. 所有腳本都使用 `utf8mb4` 字符集，確保正確支持中文
2. 遷移腳本使用條件檢查，可以安全地多次執行
3. 數據腳本使用 `ON DUPLICATE KEY UPDATE`，不會重複插入數據
4. 刪除 `position_config` 表前，請確保已遷移數據到新表結構

