# Monorepo Admin Baseline（中文版）

> 最後更新：2026-03-18  
> 範圍：`backend/*`、`frontend/*`、`shared`、`database`  
> 目的：提供可延續的正式基準文件，供後續對話、AI 接手與新模組開發直接套用。

---

## 0. 核心決策（不可推翻）

1. `church / church-admin` 是平台標準（baseline）。
2. `personal` 採漸進式對齊 `church`。
3. 不為了 `personal` 反向影響 `church` 設計品質。
4. auth / permission / menu / url-permission / maintenance / scheduler / line 最終都以 church 為準。
5. 優先對齊輸出 contract，再處理行為邏輯重構。
6. 先低風險再高風險。
7. 每一步都必須可 rollback。
8. 避免一次性大改。
9. `paged / options / tree / all` API 必須明確分工。
10. legacy API 不直接刪除：**先去耦 -> 再 deprecated -> 再觀察 -> 最後決定下架**。

---

## 1. 專案結構與定位

### 1.1 Monorepo 結構

- `backend/auth-core`
- `backend/church`
- `backend/personal`
- `frontend/church`
- `frontend/church-admin`
- `frontend/personal`
- `shared`
- `database`
- `docker-compose.yml`

### 1.2 模組定位

- `backend/church` + `frontend/church-admin`：**標準實作基底**。
- `backend/personal` + `frontend/personal`：**對齊方**。
- `backend/auth-core`：**後端共用核心**（auth DTO、gateway abstraction、shared filter engine、auth 組裝）。
- `shared`：前端共用層（composables / utils / styles），維持獨立，不回搬到單一前端專案。

---

## 2. 已完成的核心架構重構

### 2.1 auth-core 共用化（已完成）

`backend/auth-core` 已承接下列共用能力：

- Auth DTO/組裝：
  - `AuthCurrentUserResponse`、`AuthLoginResponse`、`AuthRefreshResponse`、`AuthLogoutResponse`
  - `AuthMenuItem`、`AuthUserProfile`
  - `AbstractAuthFacade`、`AuthFacade`、`AuthMenuMapper`、`AuthPermissionMapper`、`AuthRoleMapper`、`AuthUserProfileAssembler`
- 共用契約：
  - `AuthDomain`、`JwtTokenGateway`、`UrlPermissionGateway`、`CommonUrlPermission`
- 共用安全引擎：
  - `AbstractAuthGateway`
  - `AbstractJwtAuthenticationFilter`
  - `AbstractUrlPermissionFilter`
- Domain policy abstraction：
  - `AuthDomainPolicy`、`UrlPermissionPolicy`
  - church/personal policy 實作保留在各自 domain
- 共用安全元件：
  - `DomainAuthenticationManagerFactory`、`TokenBlacklistService`（auth-core 已移除 Spring annotation，bean 由各 domain app layer 註冊）

### 2.2 church/personal 薄封裝狀態（已完成）

church 與 personal 目前都採薄封裝模式：

- `AuthGateway extends AbstractAuthGateway`
- `JwtAuthenticationFilter extends AbstractJwtAuthenticationFilter`
- `UrlPermissionFilter extends AbstractUrlPermissionFilter`

Policy 實作：

- church：`ChurchAuthDomainPolicy`、`ChurchUrlPermissionPolicy`
- personal：`PersonalAuthDomainPolicy`、`PersonalUrlPermissionPolicy`

### 2.3 PERM_ 程式層清理（已完成主線）

- UserDetails authority 輸出已收斂為 normalized（無 `PERM_` 前綴）方向。
- Menu / URL permission compare 以 normalized code 為主。
- 對歷史資料仍保留必要的 normalize 保護，避免舊資料導致行為錯誤。

### 2.4 public API 控制方向（已確立）

- 方向：public/protected 存取控制由 `url_permissions` 主導。
- Security 白名單維持最小化。
- 允許 domain-specific public endpoint，但由 policy + URL permission 規則管理。

### 2.5 personal contract 對齊成果（已完成）

- `maintenance` response contract 對齊 church。
- `line-groups` response contract 已對齊，且互動模型往 church-admin 靠攏。
- `current-user` contract 已補強，包含 `menus` 結構穩定性。

### 2.6 personal paged 化與 UI 同步（已完成範圍）

已完成 paged + UI baseline 同步的管理模組：

- `users`
- `roles`
- `permissions`
- `url-permissions`
- `menus`（採 B 策略：tree 與 admin paged list 分離）

UI 強制同步完成：

- `users`、`roles`、`permissions`、`url-permissions`、`menus`、`scheduled-jobs`、`line-groups`

### 2.7 legacy API 狀態（目前）

`backend/personal` 下列 legacy GET list API 已標記 `@Deprecated`，但仍保留：

- `GET /api/personal/users`
- `GET /api/personal/roles`
- `GET /api/personal/permissions`
- `GET /api/personal/url-permissions`

---

## 3. Backend API baseline 規範

### A. 管理列表 API

- 標準路徑：`GET /{resource}/paged`
- 標準回傳：`ApiResponse<PageResponse<T>>`
- `PageResponse` 前端標準欄位：
  - `content`
  - `totalElements`
  - `totalPages`
  - `currentPage`
  - `size`

### B. options API

- 標準路徑：`GET /{resource}/options`
- 用途：
  - selector 下拉
  - modal options
  - dual-list source
- 回傳原則：僅最小必要欄位，避免整個 entity 全量暴露。

### C. tree / all API

- `tree`：給 navigation / sidebar / `current-user.menus`。
- `all`：給 parent selector / 全量輔助用途。
- 不可把 tree/all 與 paged admin list 混為同一責任。

### D. legacy list API

- 舊型式：`GET /{resource}`
- 僅過渡期保留。
- 生命週期：
  1. 去耦前端依賴
  2. 加 deprecated 註記
  3. 觀察流量
  4. 通過檢核後才下架

---

## 4. Frontend admin UI baseline 規範

以 church-admin 為唯一 UI baseline。personal 管理頁應對齊：

1. page header（標題/說明/主操作）
2. overview cards（摘要區）
3. collapsible filters（可收合查詢區）
4. table 與 actions 位置
5. pagination 區塊樣式與控制項
6. empty state 呈現
7. modal 視覺與欄位排列

允許保留差異：

- 僅限 domain 資料結構必要的最小相容差異；
- 不允許無理由保留舊版 UI 排列。

---

## 5. paged 行為標準（前端）

所有 paged 管理頁統一規則：

1. 列表資料讀 `content`
2. 分頁資訊讀 `totalElements`、`totalPages`、`currentPage`、`size`
3. 查詢後回第 1 頁
4. 清除條件後回第 1 頁
5. CRUD 後刷新當前頁
6. 若刪除後頁碼越界，clamp 到最後一頁
7. 分頁控制至少包含：
   - 第一頁
   - 上一頁
   - 下一頁
   - 最後一頁
   - page size selector

---

## 6. 特殊頁型策略

### 6.1 menus

- **tree 與 admin paged list 必須分離**。
- API 分工：
  - `/menus`：tree/navigation 可見菜單
  - `/menus/all`：parent selector 等全量輔助
  - `/menus/paged`：admin 管理列表
- 禁止破壞 sidebar / top-nav / `current-user.menus`。

### 6.2 line-groups

- 先流程同步，再 UI 同步。
- 最終模型：master-detail。
  - selectedGroup 驅動 members 區 reload
  - group/member CRUD 後刷新策略優先保留上下文

### 6.3 scheduled-jobs

- 以 UI 同步為主，保留既有行為流程。
- execute / toggle / history / polling 不隨意改動。
- 不強制套 paged，除非資料量與需求明確需要。

### 6.4 maintenance

- 不納入 paged 標準化。
- 以 response contract 對齊為主軸。

---

## 7. 已完成模組狀態總表

### users
- backend：有 `/users/paged`；legacy `/users`(GET deprecated，寫入 API 仍正常)
- frontend：Users 頁已接 paged
- paged：是
- UI 同步：是
- legacy API：有
- 備註：roles/permissions options 已改走 `/options`

### roles
- backend：有 `/roles/paged`、`/roles/options`；legacy `/roles`(GET deprecated)
- frontend：Roles 頁已接 paged
- paged：是
- UI 同步：是
- legacy API：有
- 備註：role modal 權限來源已改 `/permissions/options`

### permissions
- backend：有 `/permissions/paged`、`/permissions/options`；legacy `/permissions`(GET deprecated)
- frontend：Permissions 頁已接 paged
- paged：是
- UI 同步：是
- legacy API：有
- 備註：options 供 users/roles/url-permissions 使用

### url-permissions
- backend：有 `/url-permissions/paged`、`/url-permissions/active`；legacy `/url-permissions`(GET deprecated)
- frontend：UrlPermissions 頁已接 paged
- paged：是
- UI 同步：是
- legacy API：有
- 備註：modal options 已改 `/roles/options`、`/permissions/options`

### menus
- backend：有 `/menus` + `/menus/all` + `/menus/paged`
- frontend：Menus 管理頁用 paged；parent selector 用 all
- paged：是（管理列表）
- UI 同步：是（在 B 策略下）
- legacy API：否（`/menus`、`/menus/all` 為功能性 API）
- 備註：不得破壞 tree/navigation 路徑

### scheduled-jobs
- backend：list + execute/toggle/history
- frontend：UI 同步完成
- paged：否
- UI 同步：是
- legacy API：不適用
- 備註：保留既有行為流程

### line-groups
- backend：groups + members endpoint 完整
- frontend：流程與 UI 同步完成
- paged：否
- UI 同步：是
- legacy API：不適用
- 備註：selectedGroup 驅動 members 管理

### maintenance
- backend：system-settings/backups contract 已對齊
- frontend：maintenance 契約已對齊
- paged：否
- UI 同步：不屬 paged baseline 範圍
- legacy API：不適用
- 備註：維持 contract-first 策略

---

## 8. legacy API 退場規範

### 8.1 已標記 @Deprecated（personal GET list）

- `GET /api/personal/users` -> 替代：`/api/personal/users/paged`
- `GET /api/personal/roles` -> 替代：`/api/personal/roles/paged`、`/api/personal/roles/options`
- `GET /api/personal/permissions` -> 替代：`/api/personal/permissions/paged`、`/api/personal/permissions/options`
- `GET /api/personal/url-permissions` -> 替代：`/api/personal/url-permissions/paged`

### 8.2 觀察期建議

- 建議：**2 個版本或 6 週（取較長）**。
- 觀察內容：
  - 前端是否仍有呼叫
  - 外部 client 是否仍有流量
  - 測試/腳本/Postman 是否仍依賴

### 8.3 下架前 checklist（最小版）

1. frontend 呼叫已移除
2. 其他 frontend（church/church-admin/shared）確認無依賴
3. Postman/腳本/批次工作確認完成
4. access log 觀察期內流量為 0 或可接受閾值
5. 替代 API 已穩定
6. rollback 路徑已明確

### 8.4 最小 rollback 策略

- 下架後若發現隱藏依賴：
  1. 回復對應 controller 的 `@GetMapping` method
  2. 同 path 的 `POST/PUT/DELETE` 不受影響（HTTP method 分離）
  3. 以單一 hotfix commit 快速恢復

---

## 9. 未來新模組實作規則

新增 admin 管理模組時，遵循：

1. backend 先定義 API 分工：`paged` / `options` / (`tree` 或 `all` 視需求)
2. frontend 直接套 church-admin UI baseline
3. 不把 legacy `GET /resource` 當主列表入口
4. 若屬特殊頁型：先定流程模型，再做 UI baseline 對齊
5. 全程維持小步可回滾

---

## 10. 後續建議（低風險且必要）

1. 持續追蹤 4 支 deprecated legacy GET list API 的流量觀察。
2. 補齊 API 文件/changelog，明確標註 `paged/options/tree/all` 分工。
3. 觀察期完成後，依序下架（建議）：
   - `url-permissions` -> `users` -> `roles` -> `permissions`
4. `menus` 的 `/menus` 與 `/menus/all` 維持功能性 API，不建議直接納入 legacy 清理。

---

## 附錄：刻意保留的差異

- `menus` 保留 tree 與 admin list 分離設計（功能性差異，不是歷史負擔）。
- `scheduled-jobs`、`line-groups` 不強制套一般 paged CRUD，優先保留正確流程模型。
- `maintenance` 不納入 paged 標準，維持 contract 對齊策略。
