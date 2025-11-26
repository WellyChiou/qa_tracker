# 更新菜單 URL 為 Vue Router 路徑

## 問題說明

將前端從 HTML 遷移到 Vue 3 後，資料庫中的菜單 URL 還是指向舊的 HTML 文件路徑（如 `/expenses.html`），需要更新為 Vue Router 路徑（如 `/expenses`）。

## 解決方法

### 方法 1: 使用 SQL 腳本（推薦）

執行以下 SQL 腳本：

```bash
# 連接到 MySQL
docker compose exec mysql mysql -u appuser -papppassword qa_tracker

# 或直接執行 SQL 文件
docker compose exec mysql mysql -u appuser -papppassword qa_tracker < mysql/update-menu-urls-to-vue.sql
```

### 方法 2: 手動執行 SQL

連接到資料庫後執行：

```sql
USE qa_tracker;

-- 更新記帳系統
UPDATE menu_items SET url = '/expenses' WHERE menu_code = 'EXPENSES';

-- 更新 QA Tracker
UPDATE menu_items SET url = '/tracker' WHERE menu_code = 'TRACKER';

-- 更新管理頁面
UPDATE menu_items SET url = '/admin/users' WHERE menu_code = 'ADMIN_USERS';
UPDATE menu_items SET url = '/admin/roles' WHERE menu_code = 'ADMIN_ROLES';
UPDATE menu_items SET url = '/admin/permissions' WHERE menu_code = 'ADMIN_PERMISSIONS';
UPDATE menu_items SET url = '/admin/menus' WHERE menu_code = 'ADMIN_MENUS';
UPDATE menu_items SET url = '/admin/url-permissions' WHERE menu_code = 'ADMIN_URL_PERMISSIONS';

-- 通用更新：移除所有 .html 後綴
UPDATE menu_items SET url = REPLACE(url, '.html', '') WHERE url LIKE '%.html';
```

### 方法 3: 使用 Docker 命令

```bash
# 從本地執行 SQL 文件
docker compose exec -T mysql mysql -u appuser -papppassword qa_tracker < mysql/update-menu-urls-to-vue.sql
```

## 驗證更新

執行以下 SQL 查詢檢查更新結果：

```sql
SELECT 
    menu_code,
    menu_name,
    url,
    CASE 
        WHEN url LIKE '%.html' THEN '⚠️ 仍需更新'
        ELSE '✅ 已更新'
    END AS status
FROM menu_items
WHERE url IS NOT NULL AND url != '#'
ORDER BY order_index;
```

應該看到所有 URL 都沒有 `.html` 後綴。

## URL 對應表

| 舊 URL (HTML) | 新 URL (Vue Router) |
|--------------|---------------------|
| `/expenses.html` | `/expenses` |
| `/tracker.html` | `/tracker` |
| `/admin/users.html` | `/admin/users` |
| `/admin/roles.html` | `/admin/roles` |
| `/admin/permissions.html` | `/admin/permissions` |
| `/admin/menus.html` | `/admin/menus` |
| `/admin/url-permissions.html` | `/admin/url-permissions` |
| `/` | `/` (不變) |
| `#` | `#` (不變，父菜單) |

## 注意事項

1. **備份資料庫**：執行更新前建議先備份
   ```bash
   docker compose exec mysql mysqldump -u appuser -papppassword qa_tracker > backup.sql
   ```

2. **更新後需要重新登入**：更新菜單 URL 後，用戶需要重新登入才能看到新的菜單

3. **檢查所有菜單**：確保所有菜單都已更新，沒有遺漏

## 如果更新失敗

如果某些菜單沒有更新，可以手動檢查：

```sql
-- 查看所有需要更新的菜單
SELECT * FROM menu_items WHERE url LIKE '%.html';

-- 手動更新特定菜單
UPDATE menu_items SET url = '/new-path' WHERE menu_code = 'MENU_CODE';
```

