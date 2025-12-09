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
- **`church-data.sql`** - 崗位和人員初始數據（可選）

### 🔄 遷移和更新文件

- **`church-migrations.sql`** - 教會系統遷移腳本
  - 用於更新現有系統
  - 包含所有歷史修復和更新
  - 可安全地多次執行

### 👤 用戶管理文件

- **`create-church-admin.sql`** - 創建教會管理員帳號
- **`update-church-admin-password.sql`** - 更新教會管理員密碼
- **`create-default-admin-simple.sql`** - 創建簡單管理員（個人系統）
- **`create-default-admin-v2.sql`** - 創建管理員 v2（個人系統）

### 🔍 檢查和診斷文件

- **`check-and-update-users-table.sql`** - 檢查和更新用戶表結構
- **`../check-frontend-menus.sql`** - 檢查前台菜單配置（根目錄）

### 📝 文檔文件

- **`README_CHURCH_SQL.md`** - 教會 SQL 文件詳細說明
- **`README_POSITION_CONFIG.md`** - 崗位配置說明
- **`MIGRATION_GUIDE.md`** - 遷移指南
- **`UPDATE_MENU_URLS.md`** - 菜單 URL 更新說明
- **`CHECK_AND_CREATE.md`** - 檢查和創建指南

## 🗑️ 已整合/可移除的文件

以下文件已整合到主要文件中，可以安全移除：

### 崗位和人員相關（已整合到 church-init.sql 和 church-migrations.sql）
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

### 根目錄的檢查文件（保留）
- ✅ `check-frontend-menus.sql` - 檢查前台菜單
- ✅ `check_config.sql` - 檢查配置
- ✅ `check_members.sql` - 檢查成員
- ✅ `check_old_jobs.sql` - 檢查舊任務
- ✅ `diagnose_scheduled_jobs.sql` - 診斷定時任務
- ✅ `cleanup_orphaned_executions.sql` - 清理孤立執行記錄
- ✅ `remove_old_jobs.sql` - 移除舊任務

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

# 5. 插入初始數據（可選）
mysql -u root -p church < mysql/church-data.sql

# 6. 創建管理員帳號
mysql -u root -p church < mysql/create-church-admin.sql
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

