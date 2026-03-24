# New Project Baseline From Invest

## 1. 文件目的
本文件是「從 Invest 專案萃取、且已驗證可行」的新專案 baseline 手冊，目標是讓你或 Codex/Cursor 在同一個 monorepo 內，快速建立下一個可獨立部署、可維運的後台系統。

適用情境：
- 需要建立全新業務域（不可混入 personal/church 舊業務模型）。
- 需要獨立資料庫與獨立 ACL（users/roles/permissions/menus/url_permissions）。
- 需要沿用既有技術基座（JWT、URL permission filter、前端 shared 工具、部署入口）。

不適用情境：
- 單純在既有 personal/church 內加一個小模組。
- 只做前台網站（非 admin/backoffice）。
- 想直接導入高複雜度即時交易/黑箱 AI 決策。

---

## 2. Monorepo 結構建議（已驗證）
建議固定以下結構：

```text
backend/
  auth-core/
  <project>/
frontend/
  <project>-admin/
database/
  <project>/
    infra/
    migrations/
      schema/
      seed-baseline/
      seed-demo/
scripts/
  migration/
  deployment/
backups/
  <project>/
docker-compose.yml
docker-compose.<project>.yml
```

已驗證重點：
- `backend/<project>` 可獨立啟動（Spring Boot app，不是 library module）。
- `frontend/<project>-admin` 可獨立 build/run。
- `database/<project>` 與其他專案分離。
- `backups/<project>` 可持久化掛載並保留歷史檔。

---

## 3. Backend Baseline（已驗證）
技術基線：
- Spring Boot 3.x
- JPA + MySQL
- JWT 認證
- URL permission filter
- scheduler job handler/facade 模式（沿用，不重做 engine）

分層原則：
- `controller`：僅處理 API contract/參數驗證/回應。
- `service`：業務邏輯與交易邊界。
- `repository`：資料存取。
- `scheduler`：只負責排程觸發，業務邏輯下沉到 service。
- `system` 模組（users/roles/permissions/menus/url_permissions/maintenance/scheduler）與業務模組分離。

API 風格（已在 invest 落地）：
- `paged`：分頁列表
- `options`：精簡下拉資料
- `all`：全量列表
- `tree`：樹狀資料（例如 menu）

路徑原則：
- 一律 ` /api/<project>/* `

---

## 4. Frontend Baseline（已驗證）
技術基線：
- Vue 3 + Router + Composition API
- admin layout + permission guard
- 共用元件與工具放 `shared`

頁面骨架（已驗證可重複）：
- page header
- overview cards
- filters
- table
- pagination
- modal
- empty state

系統頁延續原則：
- scheduler、maintenance 優先沿用已驗證 baseline 互動節奏，不另發明第二套。
- run-now / logs / CRUD 操作語意要一致。

---

## 5. Database Baseline（已驗證）
核心原則：
- 每個專案獨立 DB。
- ACL 表也必須獨立，不與其他專案共用資料。

常見 baseline 表（跨專案可重用概念）：
- ACL：`users`, `roles`, `permissions`, `menu_items`, `user_roles`, `role_permissions`, `user_permissions`, `url_permissions`
- 系統設定：`system_settings`（key-value，含 category/sort）
- 排程紀錄：`scheduler_job_log`（或等價 execution log）

業務域專屬表（需重定義，不可照搬語意）：
- 例如投資域的 `portfolio`, `risk_result`, `strength_snapshot`, `opportunity_signal` 等。

migration/seed 原則：
- `schema`：表/索引/約束
- `seed-baseline`：ACL、admin、必要系統參數
- `seed-demo`：示範資料（不可預設進正式流程）

---

## 6. Backup Baseline（已驗證）
已驗證能力：
1. 手動 backup（create/list/download/delete API）
2. backup 檔案持久化（container `/app/backups` 掛載到 host `./backups/<project>`）
3. 單層路徑規範（避免雙層 `<project>/<project>`）
4. 舊雙層歷史檔相容顯示
5. 一次性搬平工具（dry-run / execute，不覆蓋同名檔）
6. backup 接入既有 scheduler（`DATABASE_BACKUP`，含 run-now/logs/cron）

路徑規範：
- container：`/app/backups`
- host：`./backups/<project>/`
- 檔名：`<project>_YYYYMMDD_HHMMSS.sql.gz`

---

## 7. Scheduler Baseline（已驗證）
原則：沿用既有 scheduler engine，不重做。

新增 job 標準流程（已驗證）：
1. 新增 `jobCode`
2. 新增 handler（實作 list/run-now/log mapping）
3. 必要時新增 scheduler trigger（`@Scheduled`）
4. 統一寫入 execution/log（成功/失敗可追蹤）
5. 接到 system scheduler API 與前端頁

已驗證可用能力：
- job list
- run-now
- logs（paged/latest）
- enable/disable（依既有機制）
- cron 顯示與基本管理語意

---

## 8. Maintenance Baseline（已驗證 MVP）
System Maintenance 第一版建議能力（已驗證）：
1. `system_settings` 設定面板（分類顯示、可編輯 key-value）
2. backup 管理（create/list/download/delete）
3. line test push（只做測試發送，不含 webhook/event engine）
4. 可顯示關聯 scheduler 狀態（例如 DATABASE_BACKUP 最後執行結果）

MVP 原則：
- 先做可讀、可改、可存，不做重型設定平台。
- 不把 ACL 管理混進 maintenance 頁。

---

## 9. 部署與初始化 Baseline（已驗證）
部署入口（已整合）：
- 本地：`./deploy_local.sh build`、`./deploy_local.sh build <mode>`
- 遠端：`./scripts/deployment/deploy-to-server-v1.1.sh`、`... <mode>`

初始化原則：
1. 先 infra（DB 建立與權限）
2. 再 migration runner（只跑未執行版本）
3. 再 seed-baseline
4. 不預設跑 seed-demo

建議必備：
- migration history table（避免重複執行）
- smoke scripts（API/ACL/scheduler/maintenance）

---

## 10. 推薦開發順序（A/B/C 小步）
建議順序（已在 invest 驗證可行）：
1. Admin baseline + auth/current-user + ACL seed
2. 核心業務 CRUD（Step 1）
3. 規則/分析（Step 2）
4. 報表與批次（Step 3）
5. 輕量警示（Step 4）
6. system baseline 補齊（scheduler/users/roles/permissions/menus/url permissions/maintenance）
7. backup 持久化 + scheduler 化 + 維運工具

每一步都要能：
- 單獨驗證
- 可 rollback
- 不提前混做下一步

---

## 11. 已驗證事項 vs 未完成事項
### 11.1 已驗證，可作為 baseline
- 獨立 backend/frontend/database 專案化方式
- 獨立 ACL 資料與 URL permission 控制
- `/api/<project>` 命名與 paged/options/all/tree 風格
- scheduler 管理（list/run-now/logs）與 adapter/facade 對接
- backup 手動流程 + 持久化 + 舊檔相容 + 搬平工具 + scheduler 化
- maintenance MVP（settings + backup + line test）
- migration runner（只跑一次）與部署整合
- smoke 驗收腳本化

### 11.2 尚未成熟或屬 future/optional（不可當 baseline 已完成）
- 跨節點/分散式鎖的排程併發控制
- 完整 restore 流程與災難復原演練自動化
- 全功能動態 cron 編輯引擎（立即生效治理）
- 高頻即時行情串流/WebSocket
- 黑箱 AI 決策與買進指令系統

---

## 12. 給 Codex/Cursor 的新專案起手模板
可直接使用以下提示詞啟動新專案第一輪：

```markdown
請協助我建立全新專案 `<project>`，嚴格遵守：

1. 獨立專案：`backend/<project>`、`frontend/<project>-admin>`、`database/<project>`
2. 獨立資料庫與 ACL（users/roles/permissions/menu_items/url_permissions）
3. API 前綴一律 `/api/<project>/...`
4. 先做 MVP，不做重型架構與黑箱 AI
5. 列表 API 使用 paged/options/all/tree 風格
6. migration 分層：schema / seed-baseline / seed-demo
7. deploy 需可重建、可 rollback、可 smoke 驗證

請先輸出：
- 範圍確認
- 目錄與模組切分
- Step 1（migration/entity/repository/service/controller/dto/frontend page）清單
- 驗證方式與風險點
等我確認後再開始實作。
```
