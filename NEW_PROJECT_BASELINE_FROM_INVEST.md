# 新專案 Baseline（由 INVEST 萃取）

## 0. 目的與適用範圍
此文件是「新專案快速起建規範」，用於在同一個 monorepo 下建立下一個獨立後台系統。  
規範重點：專案獨立、資料庫獨立、ACL 獨立、部署可重建、可回滾。  
本文件與新專案內容一律使用繁體中文，不使用簡體中文。

## 1. 適用情境
- 你要建立一個新業務域後台系統。
- 你希望沿用現有技術基礎（JWT、權限框架、前端 shared 工具）。
- 你不希望新系統污染 personal/church 既有資料表與 ACL。

## 2. 建議 monorepo 結構
- 後端：`backend/<new-project>`
- 前端：admin：`frontend/<new-project>-admin`
- 資料庫腳本：`database/<new-project>`
- 共用後端框架：`backend/auth-core`
- 共用前端工具：`shared`

建議目錄：
```text
backend/
  auth-core/
  <new-project>/
frontend/
  <new-project>-admin/
database/
  <new-project>/
    infra/
    migrations/
      schema/
      seed-baseline/
      seed-demo/
      maintenance/
      rollback/
```

## 3. 資料庫原則（必守）
- 每個專案使用獨立 DB（例如 `invest`、`<new_project_db>`）。
- ACL 也必須獨立，不可共用其他專案 ACL 資料表。
- 新專案 DB 需包含：
- `users`
- `roles`
- `permissions`
- `menu_items`
- `user_roles`
- `role_permissions`
- `user_permissions`
- `url_permissions`
- App migration 不可放 `CREATE DATABASE` / `GRANT`。
- `CREATE DATABASE` / `GRANT` 必須放在 `infra` 層腳本。

## 4. Auth / Security 原則
- 可重用程式邏輯：
- JWT utility / token filter
- URL permission filter
- current-user / menu 組裝邏輯
- 權限守衛通用工具
- 不可重用資料來源：
- 不可連到其他專案的 users/roles/permissions/menu/url_permissions。
- 每個專案都要有自己的 DataSource / EntityManager / TransactionManager。
- CORS 白名單必須可配置（透過 env），避免遠端 IP/網域 403。

## 5. API 命名規則
- 一律加專案前綴：`/api/<new-project>/...`
- 列表接口保持分層：
- `/paged`：分頁查詢
- `/options`：選單/下拉精簡欄位
- `/all`：不分頁查詢
- 例：
- `/api/<new-project>/stocks/paged`
- `/api/<new-project>/stocks/options`
- `/api/<new-project>/stocks/all`

## 6. Menu / Permission / URL Permission 命名規則
- permission code 前綴：`<NEW_PROJECT>_`（全大寫）
- menu code 前綴：`<NEW_PROJECT>_`
- URL permission 對應 method + path，且 required_permission 與 permission code 一致。
- 建議命名：
- `*_DASHBOARD_VIEW`
- `*_XXX_VIEW`
- `*_XXX_EDIT`
- `*_JOB_RUN_YYY`

## 7. 前端 Admin Baseline
- 路由前綴固定：`/<new-project>-admin/`
- 內部頁面統一路由：
- `/<new-project>-admin/`
- `/<new-project>-admin/<module>`
- `/<new-project>-admin/<module>/:id`
- 頁面基礎版型（每頁儘量一致）：
- page header
- overview cards
- filters
- table
- pagination
- modal
- empty state
- ACL 守衛：
- route guard 檢查登入
- permission guard 檢查 `requiredPermission`

## 8. Migration / Seed 分層原則

### 8.1 schema migration
- 只建立表、索引、約束、enum code 欄位結構。
- 按開發切片分檔（`01`、`02`、`03`...）。

### 8.2 seed-baseline
- 必備資料：
- 角色、權限、菜單、url_permission、admin 綁定
- 系統必要規則（例如 risk_rule）
- 預設 admin 密碼只作驗證用途，部署後強制更換。

### 8.3 seed-demo
- 僅示範資料，可選。
- 不得預設混入正式初始化流程。

## 9. Deploy Baseline
- 本地入口：
- `./deploy_local.sh build`（all）
- `./deploy_local.sh build <mode>`（例如 invest）
- 遠端入口：
- `./scripts/deployment/deploy-to-server-v1.1.sh`（all）
- `./scripts/deployment/deploy-to-server-v1.1.sh <mode>`
- 無參數可走互動選單。
- `<new-project>` 專案至少準備：
- `docker-compose.<new-project>.yml`
- `nginx/nginx-<new-project>.conf`
- `.env.<new-project>.prod.example`

## 10. 開發順序建議（Step 1 ~ Step 4）
- Step 1：主資料與核心 CRUD（可獨立驗證）。
- Step 2：規則分析與建議映射（可解釋、可追溯）。
- Step 3：日報表與批次（run-now 先行，scheduler 後開）。
- Step 4：輕量警示（polling，不先做 websocket/推播）。

每個 Step 都要滿足：
- 可單獨啟動驗證。
- 可資料回滾（至少有 rollback 腳本）。
- 不提前混入下一步範圍。

## 11. 哪些可直接複製，哪些必須重定義

### 11.1 可直接複製（建議）
- `backend/<new-project>` 的 Spring Boot 獨立啟動骨架（Application、DataSource、Security Config）。
- `frontend/<new-project>-admin` 的 admin 基線（router、layout、auth guard、permission guard）。
- `database/<new-project>` 目錄層級規範。
- 部署入口模式化做法（all / project mode）。

### 11.2 必須重定義（不可直接沿用業務語意）
- Entity、Repository、Service、Controller 的業務模型。
- permission/menu/url permission 的語意代碼。
- DB schema 欄位與索引。
- 風險規則、報表欄位、警示條件。
- 前端頁面文案與新手提醒內容。

## 12. 給 Codex/Cursor 的起手提示詞模板
以下模板可直接複製，建立新專案第一輪規劃：

```markdown
你現在要協助我建立一個全新獨立專案 `<new-project>`，請嚴格遵守：

1. 專案是獨立業務域，不是 personal/church 子模組。
2. 後端路徑：`backend/<new-project>`，必須可獨立啟動 Spring Boot app。
3. 前端路徑：`frontend/<new-project>-admin`，路由前綴 `/<new-project>-admin/`。
4. 資料庫：`<new-project-db>`，ACL 也必須獨立（users/roles/permissions/menu_items/url_permissions）。
5. 可重用只有技術基礎（auth-core/shared），不可共用其他專案業務資料表。
6. API 一律 `/api/<new-project>/*`，列表接口用 paged/options/all 分層。
7. 先做 Step 1（資料骨架 + CRUD + dashboard 基礎），不要提前做 Step 2~4。
8. migration 分層為 infra/schema/seed-baseline/seed-demo，app migration 不可包含 CREATE DATABASE/GRANT。

請先輸出：
- 範圍確認
- 目錄/模組切分
- Step 1 DB migration/entity/repository/service/controller/dto/page 清單
- 驗證方式與 seed 策略

等我確認後再開始實作。
```

