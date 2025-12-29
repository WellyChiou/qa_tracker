# SQL 文件組織說明

## 📁 文件結構

### 🎯 主要初始化文件（按順序執行）

#### 1. 個人系統（qa_tracker 資料庫）
- **`schema.sql`** - 個人系統完整資料庫結構
  - 包含：users, records, config, expenses, assets 等表
  - 用途：個人 QA Tracker 系統

#### 2. 教會系統（church 資料庫）

##### 基礎結構
- **`create-church-db.sql`** - 創建 church 資料庫
- **`church-schema.sql`** - 教會系統基礎表結構（service_schedules 等）

##### 安全系統
- **`church-security-tables.sql`** - 教會系統安全相關表
  - 包含：users, roles, permissions, role_permissions, user_roles, user_permissions, menu_items, url_permissions
  - 包含預設角色、權限、菜單數據

##### 崗位和人員管理
- **`church-init.sql`** - 崗位和人員管理系統初始化
  - 包含：positions, persons, position_persons 表結構
  - **注意**：persons 表已包含 `member_no` 和 `birthday` 欄位（用於簽到系統）
- **`church-data.sql`** - 崗位和人員初始數據（可選）

##### 資料清理工具
- **`cleanup-duplicate-url-permissions.sql`** - 清理 url_permissions 表中的重複資料
  - 檢查基於 `url_pattern` 和 `http_method` 的重複記錄
  - 移除重複資料（保留 id 最小的記錄）
  - 使用前請先備份資料庫

##### 簽到系統
- **`checkin-system-complete-setup.sql`** ⭐ **推薦使用** - 簽到系統完整配置（整合所有配置）
  - 包含：member_no 和 birthday 欄位檢查、URL 權限、菜單配置
  - 可一次性執行完成所有配置
  - 使用 `INSERT IGNORE` 可安全重複執行
  - 適用於全新安裝或現有系統更新
- **`add-member-no-to-persons.sql`** ⚠️ **已整合** - 為 persons 表添加 member_no 欄位
  - **狀態**：已整合到 `church-init.sql` 和 `checkin-system-complete-setup.sql`
  - 此檔案保留僅作為歷史記錄，新安裝請使用整合版本
- **`add-birthday-to-persons.sql`** ⚠️ **已整合** - 為 persons 表添加 birthday 欄位
  - **狀態**：已整合到 `church-init.sql` 和 `checkin-system-complete-setup.sql`
  - 此檔案保留僅作為歷史記錄，新安裝請使用整合版本
- **`add-checkin-url-permissions.sql`** - 簽到系統 URL 權限配置（包含所有 API 權限）
  - 公開 API（3 個）
  - 場次管理 API（5 個）
  - 場次查詢和統計 API（7 個，包含 CSV 和 Excel 匯出）
  - 補登管理 API（5 個，包含 CSV 和 Excel 匯出）
  - 總計：20 個 URL 權限配置
- **`add-checkin-session-management-permissions.sql`** - 場次管理 CRUD 操作的 URL 權限配置（可選）
  - 此文件已包含在 `add-checkin-url-permissions.sql` 中
- **`add-checkin-menu-items.sql`** - 簽到系統後台菜單項目
- **`update-checkin-menu-items.sql`** - 更新簽到系統菜單結構（將主菜單改為父菜單）
- **`update-checkin-menu-sessions-url.sql`** - 更新「管理場次」菜單的 URL

**詳細說明請參考**：`mysql/README_CHECKIN_SQL.md`

### 🔄 遷移和更新文件

- **`church-migrations.sql`** - 教會系統遷移腳本
  - 用於更新現有系統
  - 包含所有歷史修復和更新
  - 可安全地多次執行

### 👤 用戶管理文件

- **`church-admin-setup.sql`** - 教會系統管理員帳號設定（整合版）
- **`personal-admin-setup.sql`** - 個人系統管理員帳號設定（整合版）

### 🔍 檢查和診斷文件

- **`check-and-update-users-table.sql`** - 檢查和更新用戶表結構
- **`../check-frontend-menus.sql`** - 檢查前台菜單配置（根目錄）

### 📝 文檔文件

- **`README.md`** - 本文件，包含所有 SQL 文件的說明和使用指南
- **`CHECK_AND_CREATE.md`** - 檢查和創建資料庫的詳細指南
- **`MIGRATION_GUIDE.md`** - 服事表資料遷移指南
- **`diagnostics/README.md`** - 診斷腳本說明文檔

## 🗑️ 已整合/可移除的文件

以下文件已整合到主要文件中，可以安全移除：

### 崗位和人員相關（已整合到 church-init.sql 和 church-migrations.sql）
- ⚠️ `add-member-no-to-persons.sql` → 已整合到 `church-init.sql` 和 `checkin-system-complete-setup.sql`（保留作為歷史記錄）
- ⚠️ `add-birthday-to-persons.sql` → 已整合到 `church-init.sql` 和 `checkin-system-complete-setup.sql`（保留作為歷史記錄）
- ❌ `church-positions-schema.sql` → 已整合到 `church-init.sql`
- ❌ `add-include-in-auto-schedule.sql` → 已整合到 `church-migrations.sql`
- ❌ `fix-positions-is-active.sql` → 已整合到 `church-migrations.sql`
- ❌ `fix-position-encoding.sql` → 已整合到 `church-migrations.sql`
- ❌ `migrate-position-data.sql` → 已整合到 `church-data.sql`
- ❌ `add-live-position-persons.sql` → 已整合到 `church-data.sql`
- ❌ `remove-position-config-table.sql` → 已整合到 `church-migrations.sql`
- ❌ `remove-position-config-column.sql` → 已整合到 `church-migrations.sql`
- ❌ `fix-position-config-encoding.sql` → 已廢棄（舊表結構）
- ❌ `init-position-config.sql` → 已廢棄（舊表結構）
- ❌ `add-allow-duplicate-to-positions.sql` → 已整合到 `church-migrations.sql`

### 服事安排相關（已整合或過時）
- ❌ `church-schedule-redesign.sql` → 已整合到 `church-schema.sql`
- ❌ `migrate-service-schedules.sql` → 已整合到 `church-migrations.sql`
- ❌ `migrate-service-schedules-simple.sql` → 已整合到 `church-migrations.sql`
- ❌ `migrate-service-schedules-data.sql` → 已整合到 `church-migrations.sql`
- ❌ `fix-service-schedules-table.sql` → 已整合到 `church-migrations.sql`
- ❌ `fix-service-schedules-table-structure.sql` → 已整合到 `church-migrations.sql`
- ❌ `remove-schedule-data-column.sql` → 已整合到 `church-migrations.sql`

### 安全系統相關（已整合）
- ❌ `add-security-tables-simple.sql` → 已整合到 `church-security-tables.sql`
- ❌ `add-church-api-permission.sql` → 已整合到 `church-security-tables.sql`
- ❌ `grant-church-permissions.sql` → 已整合到 `church-security-tables.sql`
- ❌ `grant-permissions.sql` → 個人系統用，保留

### 菜單相關（已整合）
- ❌ `remove-line-groups-menu.sql` → 已整合到 `church-migrations.sql`
- ❌ `update-menu-urls-to-vue.sql` → 已整合到 `church-migrations.sql`

### LINE Bot 相關（功能擴展，保留）
- ✅ `add-line-bot-config.sql` - 添加 LINE Bot 配置
- ✅ `add-line-user-id-column.sql` - 添加 LINE 用戶 ID 欄位

### 定時任務相關（功能擴展，保留）
- ✅ `add-scheduled-jobs-url-permissions.sql` - 添加定時任務 URL 權限

### 診斷和檢查文件（已整合到 diagnostics 目錄）
- ✅ `diagnostics/check-frontend-menus.sql` - 檢查前台菜單
- ✅ `diagnostics/check_config.sql` - 檢查配置
- ✅ `diagnostics/check_members.sql` - 檢查成員
- ✅ `diagnostics/check_old_jobs.sql` - 檢查舊任務
- ✅ `diagnostics/diagnose_scheduled_jobs.sql` - 診斷定時任務
- ✅ `diagnostics/cleanup_orphaned_executions.sql` - 清理孤立執行記錄
- ✅ `diagnostics/remove_old_jobs.sql` - 移除舊任務
- ✅ `diagnostics/run-all-checks.sql` - 整合所有診斷檢查
- ✅ `diagnostics/README.md` - 診斷腳本說明文檔

## 🚀 使用指南

### 全新安裝教會系統

```bash
# 1. 創建資料庫
mysql -u root -p < mysql/create-church-db.sql

# 2. 創建基礎表結構
mysql -u root -p church < mysql/church-schema.sql

# 3. 創建安全系統表
mysql -u root -p church < mysql/church-security-tables.sql

# 4. 創建崗位和人員管理表
mysql -u root -p church < mysql/church-init.sql

# 5. 為 persons 表添加 member_no 欄位（簽到系統需要）
mysql -u root -p church < mysql/add-member-no-to-persons.sql

# 6. 插入初始數據（可選）
mysql -u root -p church < mysql/church-data.sql

# 7. 配置簽到系統 URL 權限
mysql -u root -p church < mysql/add-checkin-url-permissions.sql

# 8. 添加簽到系統後台菜單
mysql -u root -p church < mysql/add-checkin-menu-items.sql

# 9. 創建管理員帳號
mysql -u root -p church < mysql/church-admin-setup.sql
```

### 更新現有系統

```bash
# 執行遷移腳本（會自動檢查並應用必要的更新）
mysql -u root -p church < mysql/church-migrations.sql
```

### 檢查系統狀態

```bash
# 檢查前台菜單
mysql -u root -p church < check-frontend-menus.sql

# 檢查用戶表
mysql -u root -p church < mysql/check-and-update-users-table.sql
```

## 📌 注意事項

1. **執行順序很重要**：請按照上述順序執行初始化文件
2. **備份資料庫**：執行任何 SQL 文件前，請先備份資料庫
3. **遷移腳本**：`church-migrations.sql` 可以安全地多次執行
4. **數據腳本**：使用 `ON DUPLICATE KEY UPDATE`，不會重複插入數據


