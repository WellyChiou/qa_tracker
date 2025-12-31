# 教會前台網站功能說明

## 概述

教會前台網站（`frontend-church`）提供了豐富的功能，讓會友和訪客可以了解教會資訊、查看活動、參與代禱等。

## 功能列表

### 1. 小組介紹頁面

**路徑**：`/groups`

**功能說明**：
- 顯示所有啟用的小組列表
- 每個小組卡片顯示：
  - 小組名稱
  - 小組描述
  - 成員數量
- 點擊可查看小組詳情（包含成員列表）

**API 端點**：
- `GET /api/church/public/groups` - 獲取所有啟用的小組列表
- `GET /api/church/public/groups/{id}` - 獲取單一小組詳情（包含成員資訊）

**資料來源**：
- 資料表：`groups`、`persons`、`group_persons`
- 只顯示 `is_active = true` 的小組

### 2. 代禱事項頁面

**路徑**：`/prayer-requests`

**功能說明**：
- 顯示所有啟用的代禱事項
- 每個代禱事項卡片顯示：
  - 標題
  - 內容
  - 類別（可選）
  - 緊急標記（緊急/一般）
  - 建立日期
- 緊急代禱事項會有特殊標記

**API 端點**：
- `GET /api/church/public/prayer-requests` - 獲取所有啟用的代禱事項列表

**資料來源**：
- 資料表：`prayer_requests`
- 只顯示 `is_active = true` 的代禱事項
- 按建立日期降序排列

**欄位說明**：
- `title` - 代禱標題（必填）
- `content` - 代禱內容（必填）
- `category` - 代禱類別（可選，例如：教會、個人、國家）
- `is_urgent` - 是否緊急（1=是，0=否）
- `is_active` - 是否啟用（1=是，0=否）

### 3. 最新消息/公告頁面

**路徑**：`/announcements`

**功能說明**：
- 顯示所有有效的公告
- 每個公告卡片顯示：
  - 標題
  - 內容
  - 類別（可選）
  - 發布日期
  - 置頂標記（如有）
- 置頂公告會優先顯示
- 只顯示在發布日期和到期日期範圍內的公告

**API 端點**：
- `GET /api/church/public/announcements` - 獲取所有有效的公告列表

**資料來源**：
- 資料表：`announcements`
- 只顯示 `is_active = true` 的公告
- 只顯示 `publish_date <= 今天` 且 `expire_date >= 今天` 的公告（如果設定了日期）
- 按置頂優先，然後按發布日期降序排列

**欄位說明**：
- `title` - 公告標題（必填）
- `content` - 公告內容（必填）
- `category` - 公告類別（可選，例如：活動、通知、重要）
- `publish_date` - 發布日期（可選）
- `expire_date` - 過期日期（可選）
- `is_pinned` - 是否置頂（1=是，0=否）
- `is_active` - 是否啟用（1=是，0=否）

## 菜單結構

### 前台菜單（混合式結構）

採用混合式菜單結構，重要頁面直接顯示，次要頁面放在分組下：

**直接顯示（重要頁面）**：
- 首頁
- 關於我們
- 活動
- 主日信息
- 小組介紹
- 聯絡我們

**分組顯示（資訊服務父菜單）**：
- 資訊服務（父菜單，下拉顯示）
  - 最新消息（子菜單）
  - 代禱事項（子菜單）

## 技術實現

### 前端技術

- **框架**：Vue 3
- **路由**：Vue Router
- **樣式**：參考 `Activities.vue` 的風格和結構
- **API 調用**：使用 `apiRequest` 工具函數

### 後端技術

- **框架**：Spring Boot
- **資料庫**：MySQL
- **DTO 命名**：前台公開 API 使用 `Result` 後綴（例如：`GroupResult`、`PrayerRequestResult`、`AnnouncementResult`）
- **DTO 位置**：`dto.church.frontend` 套件

### 權限配置

所有前台公開 API 都配置為公開訪問（`is_public = 1`），無需登入即可查看。

## 資料庫配置

### 執行 SQL 文件

```bash
# 1. 建立表格
mysql -u root -p church < mysql/prayer-requests-schema.sql
mysql -u root -p church < mysql/announcements-schema.sql

# 2. 配置權限
mysql -u root -p church < mysql/prayer-requests-permissions.sql
mysql -u root -p church < mysql/announcements-permissions.sql
mysql -u root -p church < mysql/groups-public-permissions.sql

# 3. 配置菜單
mysql -u root -p church < mysql/add-frontend-menus.sql
```

## 首頁功能

首頁（`/`）是訪客和會友的第一印象，包含以下區塊：

### 1. Hero 區塊
- 歡迎訊息（可從後台配置）
- 三個快速按鈕：
  - 查看最新活動
  - 了解教會
  - 聯絡我們

### 2. 聚會時間
- 晚崇聚會（週六）
- 早崇聚會（週日）
- 顯示時間和地點

### 3. 重要通知
- 只顯示置頂公告（最多3個）
- 優先顯示最重要的通知
- 避免與活動資訊重複

### 4. 緊急代禱
- 只顯示標記為「緊急」的代禱事項（最多3個）
- 使用醒目的樣式提醒會友
- 鼓勵會友參與代禱

### 5. 最新活動
- 顯示即將到來的活動（最多3個）
- 只顯示未來日期的活動
- 按日期排序

**優化說明**：
- 首頁採用精簡設計，只顯示最重要的資訊
- 重要通知和緊急代禱優先顯示
- 訪客可以通過導航菜單訪問其他頁面

詳細的首頁優化建議請參考 [首頁優化指南](./HOME_PAGE_OPTIMIZATION.md)。

## 後台管理

所有前台內容都可以在後台管理系統中進行管理：

- **代禱事項管理**：`/admin/prayer-requests`（需要 `PRAYER_REQUEST_EDIT` 權限）
- **公告管理**：`/admin/announcements`（需要 `ANNOUNCEMENT_EDIT` 權限）

詳細說明請參考 [教會系統設定指南](./CHURCH_SYSTEM_SETUP.md)。

## 相關文檔

- [教會系統設定指南](./CHURCH_SYSTEM_SETUP.md) - 完整的系統設定說明
- [活動資訊 vs 最新消息使用指南](./ACTIVITIES_VS_ANNOUNCEMENTS.md) - 如何區分使用活動和公告
- [首頁優化指南](./HOME_PAGE_OPTIMIZATION.md) - 首頁設計和優化建議

