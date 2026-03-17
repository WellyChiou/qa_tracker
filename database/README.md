# SQL 檔案組織說明

## 📁 檔案結構

SQL 檔案已按資料庫和功能分類組織，結構如下：

```
mysql/
├── personal/                    # 個人系統 (qa_tracker 資料庫)
│   ├── schema/
│   │   └── schema.sql          # 主結構檔案
│   ├── setup/
│   │   └── personal-admin-setup.sql  # 初始管理員設定
│   └── permissions/
│       └── personal-permissions.sql  # 整合所有個人系統權限設定
├── church/                      # 教會系統 (church 資料庫)
│   ├── schema/                  # 主結構檔案
│   │   ├── church-schema.sql   # 主結構（包含 prayer_requests 和 announcements 表）
│   │   ├── church-security-tables.sql
│   │   ├── church-init.sql
│   │   ├── system-settings-schema.sql
│   │   └── church-scheduled-jobs-schema.sql
│   ├── setup/                   # 初始設定檔案
│   │   ├── create-church-db.sql
│   │   ├── church-admin-setup.sql
│   │   └── church-data.sql
│   ├── permissions/             # 初始權限設定（整合版）
│   │   └── church-permissions.sql
│   ├── menus/                   # 初始菜單設定（整合版）
│   │   └── church-menus.sql
│   └── features/                # 功能完整安裝腳本
│       └── checkin-system-complete-setup.sql
└── diagnostics/                 # 診斷工具
    └── [現有檔案]
```

## 🎯 主要初始化檔案（按順序執行）

### 個人系統（qa_tracker 資料庫）

1. **`personal/schema/schema.sql`** - 個人系統完整資料庫結構
   - 包含：users, records, config, expenses, assets 等表
   - 用途：個人 QA Tracker 系統

2. **`personal/setup/personal-admin-setup.sql`** - 個人系統管理員帳號設定

3. **`personal/permissions/personal-permissions.sql`** - 個人系統權限設定（整合版）
   - 整合了：資料庫權限授予、定時任務 URL 權限

### 教會系統（church 資料庫）

#### 基礎結構

1. **`church/setup/create-church-db.sql`** - 創建 church 資料庫

2. **`church/schema/church-schema.sql`** - 教會系統基礎表結構
   - 包含：service_schedules, sunday_messages, groups, sessions, checkins 等
   - **已整合**：prayer_requests 和 announcements 表結構

3. **`church/schema/church-security-tables.sql`** - 教會系統安全相關表
   - 包含：users, roles, permissions, role_permissions, user_roles, user_permissions, menu_items, url_permissions
   - 包含預設角色、權限、菜單數據

4. **`church/schema/church-init.sql`** - 崗位和人員管理系統初始化
   - 包含：positions, persons, position_persons 表結構
   - **注意**：persons 表已包含 `member_no` 和 `birthday` 欄位（用於簽到系統）

5. **`church/schema/system-settings-schema.sql`** - 系統設定表結構

6. **`church/schema/church-scheduled-jobs-schema.sql`** - 定時任務表結構

#### 初始設定

7. **`church/setup/church-admin-setup.sql`** - 教會系統管理員帳號設定

8. **`church/setup/church-data.sql`** - 崗位和人員初始數據（可選）

#### 權限和菜單配置

9. **`church/permissions/church-permissions.sql`** - 教會系統權限設定（整合版）
   - 整合了：代禱事項權限、公告權限、小組公開 API 權限

10. **`church/menus/church-menus.sql`** - 教會系統菜單設定（整合版）
    - 整合了：前台菜單（小組介紹、資訊服務、最新消息、代禱事項）
    - 整合了：後台管理菜單（代禱事項管理、公告管理）

#### 功能完整安裝

11. **`church/features/checkin-system-complete-setup.sql`** - 簽到系統完整配置
    - 包含：member_no 和 birthday 欄位檢查、URL 權限、菜單配置
    - 可一次性執行完成所有配置

## 🚀 使用指南

### 全新安裝個人系統

```bash
# 1. 創建資料庫結構
mysql -u root -p < mysql/personal/schema/schema.sql

# 2. 創建管理員帳號
mysql -u root -p qa_tracker < mysql/personal/setup/personal-admin-setup.sql

# 3. 配置權限
mysql -u root -p qa_tracker < mysql/personal/permissions/personal-permissions.sql
```

### 全新安裝教會系統

```bash
# 1. 創建資料庫
mysql -u root -p < mysql/church/setup/create-church-db.sql

# 2. 創建基礎表結構
mysql -u root -p church < mysql/church/schema/church-schema.sql

# 3. 創建安全系統表
mysql -u root -p church < mysql/church/schema/church-security-tables.sql

# 4. 創建崗位和人員管理表
mysql -u root -p church < mysql/church/schema/church-init.sql

# 5. 創建系統設定表
mysql -u root -p church < mysql/church/schema/system-settings-schema.sql

# 6. 創建定時任務表
mysql -u root -p church < mysql/church/schema/church-scheduled-jobs-schema.sql

# 7. 配置權限
mysql -u root -p church < mysql/church/permissions/church-permissions.sql

# 8. 配置菜單
mysql -u root -p church < mysql/church/menus/church-menus.sql

# 9. 創建管理員帳號
mysql -u root -p church < mysql/church/setup/church-admin-setup.sql

# 10. （可選）添加初始數據
mysql -u root -p church < mysql/church/setup/church-data.sql

# 11. （可選）配置簽到系統
mysql -u root -p church < mysql/church/features/checkin-system-complete-setup.sql
```

### Docker Compose 自動初始化

使用 Docker Compose 時，以下檔案會自動執行（在容器首次啟動時）：

- `mysql/personal/schema/schema.sql` → `/docker-entrypoint-initdb.d/01-schema.sql`
- `mysql/church/schema/church-schema.sql` → `/docker-entrypoint-initdb.d/02-church-schema.sql`

其他檔案需要手動執行或通過應用程式初始化。

## 📝 檔案整合說明

為了減少檔案數量並提高可維護性，以下檔案已整合：

### 整合的 Permissions 檔案

- **`church/permissions/church-permissions.sql`** 整合了：
  - 代禱事項權限配置
  - 公告權限配置
  - 小組公開 API 權限配置

- **`personal/permissions/personal-permissions.sql`** 整合了：
  - 資料庫權限授予
  - 定時任務 URL 權限

### 整合的 Schema 檔案

- **`church/schema/church-schema.sql`** 已整合：
  - 代禱事項表結構（prayer_requests）
  - 公告表結構（announcements）

### 整合的 Menus 檔案

- **`church/menus/church-menus.sql`** 整合了：
  - 前台菜單配置
  - 後台管理菜單配置

## 🔍 檢查和診斷

診斷工具位於 `diagnostics/` 目錄，包含：

- 各種檢查和診斷 SQL 腳本
- 清理工具
- 系統狀態檢查

詳細說明請參考 `diagnostics/README.md`

## 📌 注意事項

1. **執行順序很重要**：請按照上述順序執行初始化檔案
2. **備份資料庫**：執行任何 SQL 檔案前，請先備份資料庫
3. **整合檔案**：所有整合檔案使用 `INSERT IGNORE`，可以安全地重複執行
4. **後台菜單 URL 格式**：後台菜單的 URL 不包含 `/admin` 前綴，因為後台路由的 base path 是 `/church-admin/`

## 🗑️ 已移除的檔案

以下檔案已整合到其他檔案中，不再需要：

- `migrations/` 資料夾（歷史遷移檔案，已刪除）
- 所有 `add-*.sql`、`migrate-*.sql` 等歷史變更檔案（已刪除）
- 已整合的 permissions、schema、menus 檔案（已整合到對應的整合檔案中）
