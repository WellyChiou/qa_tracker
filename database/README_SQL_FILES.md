# SQL 文件說明

## 權限系統相關 SQL 文件

根據 `權限系統架構說明.md` 文件，以下是所有相關資料表的 SQL 文件對照：

### 核心權限系統資料表

| 資料表名稱 | SQL 文件 | 說明 |
|-----------|---------|------|
| `users` | `church-security-tables.sql` (第 7-22 行) | 用戶表 |
| `roles` | `church-security-tables.sql` (第 25-32 行) | 角色表 |
| `permissions` | `church-security-tables.sql` (第 35-46 行) | 權限表 |
| `user_roles` | `church-security-tables.sql` (第 49-58 行) | 用戶-角色關聯表 |
| `role_permissions` | `church-security-tables.sql` (第 61-70 行) | 角色-權限關聯表 |
| `user_permissions` | `church-security-tables.sql` (第 73-82 行) | 用戶-權限關聯表 |
| `url_permissions` | `church-security-tables.sql` (第 107-122 行) | URL 權限表 |
| `menu_items` | `church-security-tables.sql` (第 85-104 行) | 菜單項表（架構說明中未明確提到，但系統中有使用） |

### 系統設定相關資料表

| 資料表名稱 | SQL 文件 | 說明 |
|-----------|---------|------|
| `system_settings` | `system-settings-schema.sql` | 系統參數設定表 |

### 補充 SQL 文件（整合版）

| SQL 文件 | 用途 | 說明 |
|---------|------|------|
| `add-all-api-permissions.sql` | 添加所有 API 權限配置 | 整合了所有 API 的 URL 權限配置（教會後台管理、文件上傳、前台公開、主日信息、系統維護等） |
| `add-all-menus.sql` | 添加所有菜單配置 | 整合了所有菜單項的配置（主日信息、排程管理、系統維護） |
| `add-all-scheduled-jobs.sql` | 添加所有排程任務 | 整合了所有排程任務配置（週一服事人員通知、活動過期檢查、主日信息過期檢查、圖片清理） |
| `add-database-migrations.sql` | 資料庫結構變更 | 整合了所有資料庫結構變更（LINE 用戶 ID 欄位、服事表年份欄位） |

### 其他 SQL 文件（qa_tracker 資料庫）

| SQL 文件 | 用途 | 說明 |
|---------|------|------|
| `add-scheduled-jobs-url-permissions.sql` | 添加排程任務 URL 權限 | 針對 qa_tracker 資料庫的排程任務 API 權限配置 |
| `add-line-bot-config.sql` | 添加 LINE Bot 配置 | 針對 qa_tracker 資料庫的 LINE Bot 配置 |

## 執行順序建議

### 1. 初始化權限系統（首次安裝）

```bash
# 1. 創建所有權限系統資料表
mysql -u root -p church < mysql/church-security-tables.sql

# 2. 創建系統設定表
mysql -u root -p church < mysql/system-settings-schema.sql

# 3. 添加所有功能（整合版，一次執行即可）
mysql -u root -p church < mysql/add-all-api-permissions.sql
mysql -u root -p church < mysql/add-all-menus.sql
mysql -u root -p church < mysql/add-all-scheduled-jobs.sql
mysql -u root -p church < mysql/add-database-migrations.sql
```

### 2. 後續添加功能（已安裝權限系統）

```bash
# 添加所有功能（整合版，可以安全地重複執行）
mysql -u root -p church < mysql/add-all-api-permissions.sql
mysql -u root -p church < mysql/add-all-menus.sql
mysql -u root -p church < mysql/add-all-scheduled-jobs.sql
mysql -u root -p church < mysql/add-database-migrations.sql
```

### 3. qa_tracker 資料庫相關（個人網站系統）

```bash
# 添加排程任務 URL 權限（qa_tracker 資料庫）
mysql -u root -p qa_tracker < mysql/add-scheduled-jobs-url-permissions.sql

# 添加 LINE Bot 配置（qa_tracker 資料庫）
mysql -u root -p qa_tracker < mysql/add-line-bot-config.sql
```

## 資料表結構對照

### 權限系統核心資料表（來自 `權限系統架構說明.md`）

```
users (用戶表)
├── user_roles (用戶-角色關聯表)
│   └── roles (角色表)
│       └── role_permissions (角色-權限關聯表)
│           └── permissions (權限表)
│
└── user_permissions (用戶-權限關聯表)
    └── permissions (權限表)

url_permissions (URL 權限表)
├── required_role (可選，對應 roles.role_name)
└── required_permission (可選，對應 permissions.permission_code)

menu_items (菜單項表)
└── required_permission (可選，對應 permissions.permission_code)
```

### 系統設定資料表

```
system_settings (系統參數設定表)
└── 存儲系統配置參數（備份設定、LINE Bot 設定、JWT 設定等）
```

## 確認清單

- [x] `users` 表 - ✅ 在 `church-security-tables.sql` 中
- [x] `roles` 表 - ✅ 在 `church-security-tables.sql` 中
- [x] `permissions` 表 - ✅ 在 `church-security-tables.sql` 中
- [x] `user_roles` 表 - ✅ 在 `church-security-tables.sql` 中
- [x] `role_permissions` 表 - ✅ 在 `church-security-tables.sql` 中
- [x] `user_permissions` 表 - ✅ 在 `church-security-tables.sql` 中
- [x] `url_permissions` 表 - ✅ 在 `church-security-tables.sql` 中
- [x] `menu_items` 表 - ✅ 在 `church-security-tables.sql` 中（系統使用）
- [x] `system_settings` 表 - ✅ 在 `system-settings-schema.sql` 中

## 注意事項

1. **`church-security-tables.sql`** 是權限系統的核心文件，包含：
   - 所有核心資料表的創建語句
   - 預設角色和權限的插入語句
   - 預設菜單項的插入語句
   - 預設 URL 權限配置的插入語句

2. **`system-settings-schema.sql`** 用於創建系統參數設定表和插入預設參數。

3. **整合後的 SQL 文件**（可以安全地重複執行）：
   - **`add-all-api-permissions.sql`**：整合了所有 API 權限配置，包括教會後台管理、文件上傳、前台公開、主日信息、系統維護等
   - **`add-all-menus.sql`**：整合了所有菜單配置，包括主日信息、排程管理、系統維護
   - **`add-all-scheduled-jobs.sql`**：整合了所有排程任務配置，包括週一服事人員通知、活動過期檢查、主日信息過期檢查、圖片清理
   - **`add-database-migrations.sql`**：整合了所有資料庫結構變更，包括 LINE 用戶 ID 欄位、服事表年份欄位

4. **qa_tracker 資料庫相關文件**：
   - `add-scheduled-jobs-url-permissions.sql` 和 `add-line-bot-config.sql` 是針對個人網站系統（qa_tracker 資料庫）的，與教會系統（church 資料庫）分開管理

