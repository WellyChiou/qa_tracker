# 簽到系統變更記錄

## 2024-12-23: 裝置類型顯示、Excel 匯出與 SQL 整合

### 變更說明

新增裝置類型顯示功能（識別手機、電腦等裝置），將 CSV 匯出改為 Excel 匯出，並整合所有 SQL 檔案到初始化腳本中。

### 主要變更

#### 1. 裝置類型顯示功能

**後端變更**：
- `CheckinRepository.java`：查詢中加入 `user_agent` 欄位
- `SessionCheckinRow.java`：新增 `getUserAgent()` 方法
- `ManualCheckinRow.java`：新增 `getUserAgent()` 方法
- `ExcelService.java`：新增 `getDeviceType()` 方法，可識別：
  - 📱 iPhone、📱 Android、📱 iPad
  - 💻 電腦(Windows)、💻 電腦(Mac)、💻 電腦(Linux)
  - ❓ 未知

**前端變更**：
- `SessionDetail.vue`：新增「裝置」欄位和 `getDeviceType()` 函數
- `ManualView.vue`：新增「裝置」欄位和 `getDeviceType()` 函數
- Excel 匯出中也包含裝置類型資訊

#### 2. Excel 匯出功能

**後端變更**：
- `pom.xml`：新增 Apache POI 依賴（`poi-ooxml 5.2.5`）
- `ExcelService.java`：新增 Excel 匯出服務
  - `exportSessionCheckins()`：匯出場次簽到名單為 Excel
  - `exportManualCheckins()`：匯出補登稽核為 Excel
  - 包含標題樣式、自動調整欄位寬度、中文欄位名稱
- `AdminSessionController.java`：新增 `/export.xlsx` 端點
- `AdminManualController.java`：新增 `/export.xlsx` 端點

**前端變更**：
- `SessionDetail.vue`：將「匯出 CSV」改為「匯出 Excel」
- `ManualView.vue`：將「匯出 CSV」改為「匯出 Excel」
- 使用 `apiRequest` 下載 Excel 檔案（帶 JWT 認證）
- 自動生成檔名（包含日期）

#### 3. 補登稽核日期預設值

**前端變更**：
- `ManualView.vue`：進入頁面時自動設定日期區間
  - 起日：上週一
  - 迄日：今日
  - 自動執行查詢

#### 4. 簽到記錄操作按鈕優化

**前端變更**：
- `SessionDetail.vue`：調整操作按鈕顯示邏輯
  - 補登記錄：顯示「取消」和「刪除」按鈕
  - 自助簽到：只顯示「刪除」按鈕

#### 5. SQL 檔案整合

**整合內容**：
- `church-init.sql`：persons 表定義中直接包含 `member_no` 和 `birthday` 欄位
- `checkin-system-complete-setup.sql`：
  - 整合 `member_no` 和 `birthday` 欄位檢查（作為備用）
  - 新增 Excel 匯出權限
  - 更新權限總數為 20 個

**標記為已整合的檔案**：
- `add-member-no-to-persons.sql`：已整合，保留作為歷史記錄
- `add-birthday-to-persons.sql`：已整合，保留作為歷史記錄

**文件更新**：
- `README_CHECKIN_SQL.md`：更新權限數量、說明整合情況
- `README.md`：標記已整合的檔案
- `church-schema.sql`：更新註解說明

### SQL 變更

#### URL 權限新增（共 20 個）

**公開 API（3 個）**：
- GET `/api/church/checkin/public/sessions/*` - 取得場次資訊
- GET `/api/church/checkin/public/sessions/*/token`
- POST `/api/church/checkin/public/sessions/*/checkin`

**場次管理 API（5 個）**：
- GET `/api/church/checkin/admin/sessions`
- POST `/api/church/checkin/admin/sessions`
- GET `/api/church/checkin/admin/sessions/*`
- PUT `/api/church/checkin/admin/sessions/*`
- DELETE `/api/church/checkin/admin/sessions/*`

**場次查詢和統計 API（7 個）**：
- GET `/api/church/checkin/admin/sessions/today`
- GET `/api/church/checkin/admin/sessions/*/stats`
- GET `/api/church/checkin/admin/sessions/*/checkins`
- GET `/api/church/checkin/admin/sessions/*/checkins/export.csv`
- GET `/api/church/checkin/admin/sessions/*/checkins/export.xlsx` ⭐ 新增
- PATCH `/api/church/checkin/admin/sessions/*/checkins/*/cancel`
- DELETE `/api/church/checkin/admin/sessions/*/checkins/*`

**補登管理 API（5 個）**：
- GET `/api/church/checkin/admin/manual-checkins`
- POST `/api/church/checkin/admin/manual-checkins`
- PATCH `/api/church/checkin/admin/manual-checkins/*/cancel`
- GET `/api/church/checkin/admin/manual-checkins/export.csv`
- GET `/api/church/checkin/admin/manual-checkins/export.xlsx` ⭐ 新增

### 資料庫結構變更

#### persons 表（已整合到 church-init.sql）

```sql
CREATE TABLE IF NOT EXISTS persons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    person_name VARCHAR(100) NOT NULL,
    display_name VARCHAR(100),
    member_no VARCHAR(32) UNIQUE COMMENT '會員編號（用於簽到系統）',  -- 已整合
    birthday DATE COMMENT '生日（非必填）',  -- 已整合
    phone VARCHAR(20),
    email VARCHAR(255),
    notes TEXT,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_member_no (member_no)  -- 已整合
);
```

### 程式碼變更摘要

#### 後端變更

**新增檔案**：
- `service/church/checkin/ExcelService.java` - Excel 匯出服務

**修改檔案**：
- `pom.xml` - 新增 Apache POI 依賴
- `repository/church/checkin/CheckinRepository.java` - 查詢中加入 user_agent
- `dto/church/checkin/SessionCheckinRow.java` - 新增 getUserAgent()
- `dto/church/checkin/ManualCheckinRow.java` - 新增 getUserAgent()
- `controller/church/checkin/AdminSessionController.java` - 新增 Excel 匯出端點
- `controller/church/checkin/AdminManualController.java` - 新增 Excel 匯出端點

#### 前端變更

**修改檔案**：
- `views/checkin/SessionDetail.vue`
  - 新增「裝置」欄位
  - 新增 `getDeviceType()` 函數
  - 將「匯出 CSV」改為「匯出 Excel」
  - 調整操作按鈕顯示邏輯（補登才顯示取消按鈕）
- `views/checkin/ManualView.vue`
  - 新增「裝置」欄位
  - 新增 `getDeviceType()` 函數
  - 將「匯出 CSV」改為「匯出 Excel」
  - 新增日期預設值（上週一至今日）
  - 進入頁面時自動執行查詢

### SQL 檔案整合狀態

#### 已整合到初始化 SQL

- ✅ `member_no` 欄位 → 整合到 `church-init.sql`
- ✅ `birthday` 欄位 → 整合到 `church-init.sql`
- ✅ Excel 匯出權限 → 整合到 `checkin-system-complete-setup.sql`

#### 保留作為歷史記錄

- ⚠️ `add-member-no-to-persons.sql` - 已標記為已整合
- ⚠️ `add-birthday-to-persons.sql` - 已標記為已整合

### 注意事項

1. **全新安裝**：直接執行 `church-init.sql` 即可，無需執行欄位添加腳本
2. **現有系統**：`checkin-system-complete-setup.sql` 會檢查欄位是否存在，可安全執行
3. **Excel 匯出**：需要重新建置後端以安裝 Apache POI 依賴
4. **裝置識別**：基於 User-Agent 字串解析，可能無法識別所有裝置類型

### 相關文件

- SQL 整合腳本：`mysql/checkin-system-complete-setup.sql`
- SQL 文件說明：`mysql/README_CHECKIN_SQL.md`
- 測試指南：`docs/CHECKIN_SYSTEM_TESTING.md`

---

## 2024-12-23: 場次管理功能與系統優化

### 變更說明

新增完整的場次管理功能，包括場次列表、編輯、刪除，以及簽到記錄的刪除功能。同時修復了多個技術問題。

### 主要變更

#### 1. 檔案重命名與結構調整

- **重命名**：`AdminView.vue` → `SessionDetail.vue`
  - 更符合功能定位：顯示場次詳情和編輯
  - 更新了所有路由引用

#### 2. 新增場次管理功能

**後端 API（AdminSessionController）**：
- `GET /api/church/checkin/admin/sessions` - 取得所有場次列表
- `GET /api/church/checkin/admin/sessions/{id}` - 取得單一場次
- `POST /api/church/checkin/admin/sessions` - 新增場次
- `PUT /api/church/checkin/admin/sessions/{id}` - 更新場次
- `DELETE /api/church/checkin/admin/sessions/{id}` - 刪除場次
- `DELETE /api/church/checkin/admin/sessions/{sessionId}/checkins/{checkinId}` - 刪除簽到記錄

**前端頁面**：
- `SessionList.vue` - 場次列表頁面（新增）
  - 顯示所有場次（session_code, title, status, open_at, close_at）
  - 查詢功能（場次代碼、標題、狀態）
  - 分頁功能
  - 編輯和刪除按鈕
  - 新增場次 Modal

- `SessionDetail.vue` - 場次詳情/編輯頁面（重命名自 AdminView.vue）
  - 支援兩種模式：列表模式（顯示今天的場次）和編輯模式（編輯單一場次）
  - 可編輯所有場次欄位（session_code, title, session_type, session_date, open_at, close_at, status）
  - 保留原有功能：簽到名單、補登、QR Code 等
  - 新增刪除簽到記錄功能

**組件**：
- `SessionModal.vue` - 新增/編輯場次的 Modal 組件

#### 3. 技術修復

**Spring Data JPA 原生查詢映射問題**：
- 將 `SessionCheckinRow` 從 class 改為 interface
- 使用 interface projection 映射原生 SQL 查詢結果
- 修復 Boolean 類型映射問題（MySQL bit(1) → Integer → Boolean）

**SQL 查詢優化**：
- 將 `JOIN` 改為 `LEFT JOIN` 避免資料遺失
- 使用 `CASE WHEN` 確保 Boolean 類型正確返回
- 添加 `COALESCE` 處理 NULL 值

#### 4. 功能增強

**刪除簽到記錄**：
- 後端：`CheckinService.deleteCheckin()` 方法
- 前端：簽到名單表格新增「操作」欄位和刪除按鈕
- 包含確認對話框和成功提示

#### 5. 路由配置更新

- `/checkin/admin/sessions` - 場次列表
- `/checkin/admin/sessions/:id` - 場次詳情/編輯
- `/checkin/admin` - 今天的場次列表（原有功能）
- `/checkin/admin/manual` - 補登稽核（原有功能）

### SQL 變更

#### 新增 SQL 文件

1. **`checkin-system-complete-setup.sql`**（新增，整合所有配置）
   - 整合了所有簽到系統相關的 SQL 配置
   - 包含：member_no 欄位、URL 權限、菜單配置
   - 可一次性執行完成所有配置

2. **`add-checkin-url-permissions.sql`**（更新）
   - 新增場次管理 CRUD 權限
   - 新增刪除簽到記錄權限
   - 包含所有簽到系統相關的 URL 權限

3. **`add-checkin-session-management-permissions.sql`**（新增）
   - 專門用於場次管理的權限配置
   - 可單獨執行

4. **`update-checkin-menu-sessions-url.sql`**（新增）
   - 更新「管理場次」菜單的 URL

#### SQL 執行順序

**全新安裝**：
```bash
# 1. 基礎資料表結構
mysql -uroot -p church < mysql/church-schema.sql

# 2. 安全系統表（包含 menu_items 和 url_permissions）
mysql -uroot -p church < mysql/church-security-tables.sql

# 3. 簽到系統完整配置（推薦）
mysql -uroot -p church < mysql/checkin-system-complete-setup.sql
```

**或分步執行**：
```bash
# 1. 添加 member_no 欄位
mysql -uroot -p church < mysql/add-member-no-to-persons.sql

# 2. 添加 URL 權限
mysql -uroot -p church < mysql/add-checkin-url-permissions.sql

# 3. 添加菜單
mysql -uroot -p church < mysql/add-checkin-menu-items.sql

# 4. 更新菜單 URL
mysql -uroot -p church < mysql/update-checkin-menu-sessions-url.sql
```

**現有系統更新**：
```bash
# 只需執行完整配置腳本（使用 INSERT IGNORE，不會重複插入）
mysql -uroot -p church < mysql/checkin-system-complete-setup.sql
```

### 資料庫變更摘要

#### URL 權限新增（共 14 個）

**公開 API（2 個）**：
- GET `/api/church/checkin/public/sessions/*/token`
- POST `/api/church/checkin/public/sessions/*/checkin`

**場次管理 API（5 個）**：
- GET `/api/church/checkin/admin/sessions`
- POST `/api/church/checkin/admin/sessions`
- GET `/api/church/checkin/admin/sessions/*`
- PUT `/api/church/checkin/admin/sessions/*`
- DELETE `/api/church/checkin/admin/sessions/*`

**場次查詢和統計 API（5 個）**：
- GET `/api/church/checkin/admin/sessions/today`
- GET `/api/church/checkin/admin/sessions/*/stats`
- GET `/api/church/checkin/admin/sessions/*/checkins`
- GET `/api/church/checkin/admin/sessions/*/checkins/export.csv`
- DELETE `/api/church/checkin/admin/sessions/*/checkins/*`

**補登管理 API（4 個）**：
- GET `/api/church/checkin/admin/manual-checkins`
- POST `/api/church/checkin/admin/manual-checkins`
- PATCH `/api/church/checkin/admin/manual-checkins/*/cancel`
- GET `/api/church/checkin/admin/manual-checkins/export.csv`

#### 菜單配置

- **主菜單**：`ADMIN_CHECKIN` - 簽到管理（父菜單）
- **子菜單 1**：`ADMIN_CHECKIN_SESSIONS` - 管理場次（`/checkin/admin/sessions`）
- **子菜單 2**：`ADMIN_CHECKIN_MANUAL` - 補登稽核（`/checkin/admin/manual`）

### 程式碼變更摘要

#### 後端變更

**新增檔案**：
- `dto/church/checkin/SessionCheckinRow.java`（改為 interface）

**修改檔案**：
- `controller/church/checkin/AdminSessionController.java`
  - 新增場次 CRUD API
  - 新增刪除簽到記錄 API
- `service/church/checkin/CheckinService.java`
  - 新增 `deleteCheckin()` 方法
- `repository/church/checkin/CheckinRepository.java`
  - 修復 SQL 查詢的 Boolean 類型映射
- `service/church/checkin/CsvService.java`
  - 更新為使用 interface projection

#### 前端變更

**新增檔案**：
- `views/checkin/SessionList.vue` - 場次列表頁面
- `components/SessionModal.vue` - 場次新增/編輯 Modal

**重命名檔案**：
- `views/checkin/AdminView.vue` → `views/checkin/SessionDetail.vue`

**修改檔案**：
- `views/checkin/SessionDetail.vue`
  - 支援編輯模式
  - 新增刪除簽到記錄功能
  - 新增場次編輯表單
- `router/index.js`
  - 新增場次列表和編輯路由

### 注意事項

1. **SQL 執行**：建議使用 `checkin-system-complete-setup.sql` 一次性完成所有配置
2. **權限配置**：所有 API 都需要認證（除了公開的簽到 API）
3. **資料完整性**：刪除場次前請確認沒有重要的簽到記錄
4. **刪除簽到**：刪除簽到記錄是永久性的，無法復原

### 相關文件

- 測試指南：`docs/CHECKIN_SYSTEM_TESTING.md`
- SQL 整合腳本：`mysql/checkin-system-complete-setup.sql`
- URL 權限配置：`mysql/add-checkin-url-permissions.sql`
- 菜單配置：`mysql/add-checkin-menu-items.sql`

---

## 2024-12-21: 整合 persons 表

### 變更說明

簽到系統已從獨立的 `members` 表改為使用現有的 `persons` 表，實現資料統一管理。

### 主要變更

#### 1. 資料庫結構

- **新增欄位**：`persons` 表新增 `member_no` 欄位（VARCHAR(32), UNIQUE）
- **執行腳本**：`mysql/add-member-no-to-persons.sql`
- **保留表**：`members` 表保留以維持向後兼容，但新系統不再使用

#### 2. Entity 層

- **刪除**：`com.example.helloworld.entity.church.checkin.Member`
- **更新**：`com.example.helloworld.entity.church.Person`
  - 新增 `memberNo` 欄位
  - 新增對應的 getter/setter

#### 3. Repository 層

- **刪除**：`com.example.helloworld.repository.church.checkin.MemberRepository`
- **更新**：`com.example.helloworld.repository.church.PersonRepository`
  - 新增 `findByMemberNo(String memberNo)` 方法
- **更新**：`com.example.helloworld.repository.church.checkin.CheckinRepository`
  - SQL 查詢從 `JOIN members` 改為 `JOIN persons`
  - 顯示名稱使用 `COALESCE(p.display_name, p.person_name)`

#### 4. Service 層

- **更新**：`com.example.helloworld.service.church.checkin.CheckinService`
  - 使用 `PersonRepository` 替代 `MemberRepository`
  - 返回名稱時優先使用 `displayName`，否則使用 `personName`

#### 5. 資料查詢變更

- **簽到記錄查詢**：`checkins.member_id` 現在指向 `persons.id`
- **名稱顯示**：優先顯示 `display_name`，如果為空則顯示 `person_name`
- **搜尋功能**：同時搜尋 `member_no`、`person_name` 和 `display_name`

### 遷移步驟

#### 對於新安裝

1. 執行基礎 schema：
   ```bash
   mysql -uroot -p church < mysql/church-schema.sql
   ```

2. 添加 member_no 欄位：
   ```bash
   mysql -uroot -p church < mysql/add-member-no-to-persons.sql
   ```

3. 為現有 persons 資料添加 member_no：
   ```sql
   UPDATE persons SET member_no = 'A001' WHERE person_name = '張三';
   -- 依此類推...
   ```

#### 對於現有系統

1. 執行遷移腳本：
   ```bash
   mysql -uroot -p church < mysql/add-member-no-to-persons.sql
   ```

2. 遷移現有 members 資料到 persons（如果需要）：
   ```sql
   -- 如果 members 表有資料需要遷移到 persons
   INSERT INTO persons (person_name, display_name, member_no, is_active)
   SELECT name, name, member_no, 1
   FROM members
   ON DUPLICATE KEY UPDATE 
     person_name = VALUES(person_name),
     member_no = VALUES(member_no);
   ```

3. 更新 checkins 表的 member_id（如果需要）：
   ```sql
   -- 如果 checkins 表的 member_id 還指向舊的 members 表
   -- 需要根據 member_no 重新映射到 persons.id
   UPDATE checkins c
   JOIN members m ON m.id = c.member_id
   JOIN persons p ON p.member_no = m.member_no
   SET c.member_id = p.id;
   ```

### API 變更

**無變更**：所有 API 接口保持不變，前端無需修改。

### 注意事項

1. **member_no 唯一性**：`persons.member_no` 必須是唯一的
2. **資料完整性**：確保所有需要簽到的人員都有 `member_no`
3. **向後兼容**：`members` 表暫時保留，但新功能不再使用
4. **顯示名稱**：系統會優先使用 `display_name`，如果為空則使用 `person_name`

### 相關文件

- 測試指南：`docs/CHECKIN_SYSTEM_TESTING.md`
- 資料庫腳本：`mysql/add-member-no-to-persons.sql`
- Entity 定義：`backend/src/main/java/com/example/helloworld/entity/church/Person.java`

