# 簽到系統 SQL 文件說明

## 📋 文件列表

### 主要文件

1. **`checkin-system-complete-setup.sql`** ⭐ **推薦使用**
   - **用途**：整合所有簽到系統相關的 SQL 配置
   - **內容**：
     - 確保 `member_no` 和 `birthday` 欄位存在（已整合到 `church-init.sql`）
     - 所有 URL 權限配置（包含 Excel 匯出權限）
     - 所有菜單配置
     - 菜單 URL 更新
   - **優點**：一次性執行完成所有配置，使用 `INSERT IGNORE` 可安全重複執行
   - **適用場景**：全新安裝或現有系統更新

### 分步執行文件（可選）

2. **`add-member-no-to-persons.sql`** ⚠️ **已整合**
   - **狀態**：已整合到 `church-init.sql` 和 `checkin-system-complete-setup.sql`
   - **用途**：為 `persons` 表添加 `member_no` 欄位
   - **內容**：添加 `member_no VARCHAR(32) UNIQUE` 欄位
   - **注意**：此檔案保留僅作為歷史記錄，新安裝請使用整合版本

3. **`add-birthday-to-persons.sql`** ⚠️ **已整合**
   - **狀態**：已整合到 `church-init.sql` 和 `checkin-system-complete-setup.sql`
   - **用途**：為 `persons` 表添加 `birthday` 欄位
   - **內容**：添加 `birthday DATE` 欄位
   - **注意**：此檔案保留僅作為歷史記錄，新安裝請使用整合版本

4. **`add-checkin-url-permissions.sql`**
   - **用途**：添加所有簽到系統的 URL 權限配置
   - **內容**：
     - 公開 API 權限（3 個）
     - 場次管理 API 權限（5 個）
     - 場次查詢和統計 API 權限（7 個，包含 CSV 和 Excel 匯出）
     - 補登管理 API 權限（5 個，包含 CSV 和 Excel 匯出）
   - **總計**：20 個 URL 權限配置

5. **`add-checkin-session-management-permissions.sql`**
   - **用途**：專門用於場次管理 CRUD 操作的權限配置
   - **內容**：場次管理的 5 個 API 權限
   - **注意**：此文件已包含在 `add-checkin-url-permissions.sql` 中

6. **`add-checkin-menu-items.sql`**
   - **用途**：添加簽到系統的後台菜單項目
   - **內容**：
     - 主菜單：簽到管理
     - 子菜單：管理場次、補登稽核

7. **`update-checkin-menu-items.sql`**
   - **用途**：更新簽到系統菜單結構
   - **內容**：將主菜單改為父菜單，調整子菜單順序

8. **`update-checkin-menu-sessions-url.sql`**
   - **用途**：更新「管理場次」菜單的 URL
   - **內容**：將 URL 從 `/checkin/admin` 改為 `/checkin/admin/sessions`

## 🚀 執行方式

### 方式一：整合腳本（推薦）

```bash
# 一次性執行所有配置
docker compose exec -T mysql mysql -uroot -prootpassword church < mysql/checkin-system-complete-setup.sql
```

### 方式二：分步執行（不推薦，已整合）

```bash
# 注意：member_no 和 birthday 欄位已整合到 church-init.sql
# 如果使用全新安裝，請直接執行 church-init.sql

# 1. 添加 URL 權限
docker compose exec -T mysql mysql -uroot -prootpassword church < mysql/add-checkin-url-permissions.sql

# 2. 添加菜單
docker compose exec -T mysql mysql -uroot -prootpassword church < mysql/add-checkin-menu-items.sql

# 3. 更新菜單 URL
docker compose exec -T mysql mysql -uroot -prootpassword church < mysql/update-checkin-menu-sessions-url.sql
```

## 📊 URL 權限清單

### 公開 API（無需認證）

| 順序 | URL Pattern | Method | 說明 |
|------|------------|--------|------|
| 100 | `/api/church/checkin/public/sessions/*/token` | GET | 取得簽到短效 token |
| 101 | `/api/church/checkin/public/sessions/*/checkin` | POST | 會眾自助簽到 |

### 場次管理 API（需要認證）

| 順序 | URL Pattern | Method | 說明 |
|------|------------|--------|------|
| 200 | `/api/church/checkin/admin/sessions` | GET | 取得所有場次列表 |
| 201 | `/api/church/checkin/admin/sessions/*` | GET | 取得單一場次 |
| 202 | `/api/church/checkin/admin/sessions` | POST | 新增場次 |
| 203 | `/api/church/checkin/admin/sessions/*` | PUT | 更新場次 |
| 204 | `/api/church/checkin/admin/sessions/*` | DELETE | 刪除場次 |

### 場次查詢和統計 API（需要認證）

| 順序 | URL Pattern | Method | 說明 |
|------|------------|--------|------|
| 210 | `/api/church/checkin/admin/sessions/today` | GET | 取得今日場次列表 |
| 211 | `/api/church/checkin/admin/sessions/*/stats` | GET | 取得場次統計 |
| 212 | `/api/church/checkin/admin/sessions/*/checkins` | GET | 取得場次簽到名單 |
| 213 | `/api/church/checkin/admin/sessions/*/checkins/export.csv` | GET | 匯出場次簽到名單 CSV |
| 214 | `/api/church/checkin/admin/sessions/*/checkins/*` | DELETE | 刪除簽到記錄 |

### 補登管理 API（需要認證）

| 順序 | URL Pattern | Method | 說明 |
|------|------------|--------|------|
| 300 | `/api/church/checkin/admin/manual-checkins` | GET | 取得補登稽核列表 |
| 301 | `/api/church/checkin/admin/manual-checkins` | POST | 新增補登 |
| 302 | `/api/church/checkin/admin/manual-checkins/*/cancel` | PATCH | 取消補登 |
| 303 | `/api/church/checkin/admin/manual-checkins/export.csv` | GET | 匯出補登稽核 CSV |
| 304 | `/api/church/checkin/admin/manual-checkins/export.xlsx` | GET | 匯出補登稽核 Excel |

## 📁 菜單結構

```
簽到管理 (ADMIN_CHECKIN) - 父菜單
├── 管理場次 (ADMIN_CHECKIN_SESSIONS) - /checkin/admin/sessions
└── 補登稽核 (ADMIN_CHECKIN_MANUAL) - /checkin/admin/manual
```

## ✅ 驗證配置

執行 SQL 後，可以使用以下查詢驗證配置：

```sql
-- 檢查 URL 權限
SELECT COUNT(*) AS url_permission_count
FROM url_permissions
WHERE url_pattern LIKE '/api/church/checkin/%'
AND is_active = 1;
-- 預期結果：20（包含 CSV 和 Excel 匯出權限）

-- 檢查菜單配置
SELECT COUNT(*) AS menu_count
FROM menu_items
WHERE menu_code LIKE 'ADMIN_CHECKIN%'
AND is_active = 1;
-- 預期結果：3（1 個父菜單 + 2 個子菜單）

-- 檢查 member_no 欄位
SELECT COUNT(*) AS column_exists
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'church'
AND TABLE_NAME = 'persons'
AND COLUMN_NAME = 'member_no';
-- 預期結果：1
```

## 🔄 更新現有系統

如果系統已經有部分配置，可以安全地執行 `checkin-system-complete-setup.sql`：

- 使用 `INSERT IGNORE` 避免重複插入
- 使用 `UPDATE` 確保配置正確
- 不會刪除現有資料

## 📝 注意事項

1. **執行順序**：必須先執行 `church-security-tables.sql` 建立 `url_permissions` 和 `menu_items` 表
2. **重複執行**：所有 SQL 文件都使用 `INSERT IGNORE` 或檢查機制，可安全重複執行
3. **資料備份**：建議在執行前備份資料庫
4. **權限檢查**：執行後請檢查 URL 權限和菜單是否正確顯示

## 🔗 相關文件

- 變更記錄：`docs/CHECKIN_SYSTEM_CHANGELOG.md`
- 測試指南：`docs/CHECKIN_SYSTEM_TESTING.md`
- 主 README：`mysql/README.md`

