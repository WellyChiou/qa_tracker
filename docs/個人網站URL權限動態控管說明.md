# 個人網站 URL 權限動態控管說明

## 現狀分析

### ✅ 已實現的功能

1. **UrlPermissionFilter 已支援個人網站**
   - 對於非 `/api/church/` 的路徑，會從 `qa_tracker` 資料庫的 `url_permissions` 表讀取配置
   - 已實現公開、角色、權限檢查邏輯

2. **後端服務已完整**
   - `UrlPermissionService` - 處理個人網站的 URL 權限
   - `UrlPermissionController` - 提供管理 API (`/api/url-permissions`)
   - `UrlPermission` 實體 - 對應 `qa_tracker.url_permissions` 表

3. **前端管理頁面已存在**
   - `frontend/src/views/admin/UrlPermissions.vue` - URL 權限管理頁面
   - 已支援新增、編輯、刪除功能
   - 已支援下拉選單選擇角色和權限

### ⚠️ 需要注意的地方

**SecurityConfig 中的靜態規則優先級**

目前 `SecurityConfig` 中有很多硬編碼的規則，例如：
```java
.requestMatchers("/api/records/**").authenticated()
.requestMatchers("/api/expenses/**").authenticated()
```

這些規則會在 `UrlPermissionFilter` **之前**執行，所以：
- 如果 SecurityConfig 中設定了 `.authenticated()`，則必須登入
- 如果 SecurityConfig 中設定了 `.permitAll()`，則可以直接訪問
- `UrlPermissionFilter` 只會在通過 SecurityConfig 檢查後才會執行

## 運作機制

### 執行順序

```
1. SecurityConfig 靜態規則檢查
   ↓ (如果通過)
2. JwtAuthenticationFilter (JWT Token 驗證)
   ↓ (如果通過)
3. UrlPermissionFilter (動態權限檢查)
   ↓ (如果通過)
4. Controller 層的 @PreAuthorize 檢查
   ↓ (如果通過)
5. 執行實際的業務邏輯
```

### UrlPermissionFilter 的處理邏輯

對於個人網站的 API（非 `/api/church/`）：

1. **檢查資料庫配置**
   ```java
   // 從 qa_tracker.url_permissions 表讀取配置
   List<UrlPermission> permissions = urlPermissionService.getAllActivePermissions();
   ```

2. **匹配 URL 模式**
   - 支援通配符：`*` (單層路徑)、`**` (多層路徑)
   - 例如：`/api/records/*` 可以匹配 `/api/records/1`、`/api/records/2`

3. **檢查 HTTP 方法**
   - 如果配置中指定了 HTTP 方法，必須匹配
   - 如果配置中為空，則匹配所有方法

4. **權限檢查順序**
   ```
   a. 是否公開 (is_public = true) → 直接允許
   b. 是否已登入 → 未登入則返回 401
   c. 是否需要特定角色 (requiredRole) → 沒有則返回 403
   d. 是否需要特定權限 (requiredPermission) → 沒有則返回 403
   e. 通過所有檢查 → 允許訪問
   ```

## 使用方式

### 1. 在資料庫中配置 URL 權限

使用個人網站的後台管理頁面：`/admin/url-permissions`

或直接在資料庫中插入：

```sql
USE qa_tracker;

INSERT INTO url_permissions (
    url_pattern, 
    http_method, 
    is_public, 
    required_role, 
    required_permission, 
    is_active, 
    order_index, 
    description
) VALUES (
    '/api/records',           -- URL 模式
    'GET',                    -- HTTP 方法（NULL = 所有方法）
    0,                        -- 是否公開（0 = 需要登入，1 = 公開）
    NULL,                     -- 需要角色（NULL = 不需要特定角色）
    NULL,                     -- 需要權限（NULL = 不需要特定權限）
    1,                        -- 是否啟用
    0,                        -- 排序（數字越小優先級越高）
    '查詢記錄列表'            -- 描述
);
```

### 2. 範例配置

#### 範例 1：公開查詢（無需登入）
```sql
INSERT INTO url_permissions VALUES (
    NULL, '/api/public/stats', 'GET', 1, NULL, NULL, 1, 0, '公開統計資料'
);
```

#### 範例 2：需要登入但不需要特定角色
```sql
INSERT INTO url_permissions VALUES (
    NULL, '/api/records', 'GET', 0, NULL, NULL, 1, 0, '查詢記錄（需登入）'
);
```

#### 範例 3：需要管理員角色
```sql
INSERT INTO url_permissions VALUES (
    NULL, '/api/admin/**', NULL, 0, 'ROLE_ADMIN', NULL, 1, 0, '管理功能（需管理員）'
);
```

#### 範例 4：需要特定權限
```sql
INSERT INTO url_permissions VALUES (
    NULL, '/api/records', 'DELETE', 0, NULL, 'RECORD_DELETE', 1, 0, '刪除記錄（需權限）'
);
```

## 與 SecurityConfig 的配合

### 建議的配置策略

1. **在 SecurityConfig 中設定寬鬆的規則**
   ```java
   // 讓 UrlPermissionFilter 來處理具體的權限控制
   .requestMatchers("/api/**").authenticated()  // 預設需要登入
   ```

2. **在資料庫中設定細粒度的規則**
   - 使用 `is_public = 1` 來設定公開 API
   - 使用 `required_role` 來限制角色
   - 使用 `required_permission` 來限制權限

### 注意事項

⚠️ **如果 SecurityConfig 中設定了 `.permitAll()`，UrlPermissionFilter 仍然會執行**

例如：
```java
.requestMatchers("/api/public/**").permitAll()
```

即使 SecurityConfig 允許公開訪問，UrlPermissionFilter 仍會檢查資料庫配置：
- 如果資料庫中配置了該 URL 且 `is_public = 1`，則允許訪問
- 如果資料庫中配置了該 URL 且 `is_public = 0`，則需要登入
- 如果資料庫中沒有配置，則按照 SecurityConfig 的規則（permitAll）

## 與教會後台的對比

| 項目 | 教會後台 | 個人網站 |
|------|---------|---------|
| 資料庫 | `church.url_permissions` | `qa_tracker.url_permissions` |
| Service | `ChurchUrlPermissionService` | `UrlPermissionService` |
| Controller | `/api/church/admin/url-permissions` | `/api/url-permissions` |
| Filter 處理 | 優先檢查教會資料庫 | 檢查個人網站資料庫 |
| 前端頁面 | `frontend-church/src/views/admin/UrlPermissions.vue` | `frontend/src/views/admin/UrlPermissions.vue` |

## 總結

✅ **個人網站已經支援 URL 權限動態控管！**

- 後端 Filter 已實現
- 後端 Service 和 Controller 已完整
- 前端管理頁面已存在
- 資料庫表結構已就緒

只需要：
1. 在後台管理頁面配置 URL 權限規則
2. 確保 SecurityConfig 中的規則不會過於嚴格（建議使用 `.authenticated()` 作為預設）
3. 使用資料庫配置來實現細粒度的權限控制

這樣就可以像教會後台一樣，通過資料庫動態控制個人網站的 API 訪問權限了！

