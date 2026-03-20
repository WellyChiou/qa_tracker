# INVEST 專案第一階段交接文件

## 0. 文件目的
本文件用於交接 INVEST 專案第一階段（V1 + V1.5）已完成功能、初始化方式、部署方式與已知限制。  
本文件與系統內容一律使用繁體中文，不使用簡體中文。

## 1. 專案定位
- 系統定位：給股票新手使用的持股風險分析、日常監控、簡版盤中警示系統。
- 不做：自動下單、全市場即時掃描、AI 黑箱買賣建議、推播通知（LINE/Email/Push）。
- 核心價值：
- 整理持股資料。
- 以可解釋規則呈現風險原因。
- 提供新手可理解的建議與風險提醒。
- 提供每日報告與輕量盤中警示。

## 2. 已完成範圍（V1 / V1.5）

### 2.1 V1（日級風險分析）
- 持股中心：`stock`、`stock_price_daily`、`portfolio` CRUD。
- Dashboard：總成本、市值、損益、持股數、最新報告摘要、警示摘要。
- 日級風險分析：
- `risk_rule`、`portfolio_risk_result`、`portfolio_risk_reason`。
- 5 條固定規則（可由 `risk_rule.enabled` / `score_weight` 控制生效與權重）。
- recommendation 映射：`HOLD / WATCH / REDUCE / STOP_LOSS_CHECK`。
- 每日報告：
- `portfolio_daily_snapshot`、`daily_report`、`scheduler_job_log`。
- run-now API（僅當前登入者）。
- Scheduler（可開關）。

### 2.2 V1.5（盤中警示最小版）
- `portfolio_alert_setting`、`portfolio_alert_event`。
- 觸發條件：
- `STOP_LOSS`（跌破停損價）。
- `DROP_PERCENT`（當日跌幅達門檻）。
- `ABNORMAL_DROP`（短時間異常下跌，最小版記憶體基準）。
- run-now API（僅當前登入者）。
- Scheduler（可開關）。

## 3. 系統架構
- 後端：`backend/invest`（獨立 Spring Boot App，非 personal 子模組）。
- 前端：`frontend/invest-admin`（獨立 Vue Admin 專案）。
- 資料庫：`invest`（業務資料 + ACL 資料皆獨立）。
- ACL/Auth：invest 自有 `users/roles/permissions/menu_items/url_permissions`，不與 personal/church 共用 ACL 資料表。
- 可共用程式基礎：`backend/auth-core` 與 `shared`（工具與框架邏輯）。

## 4. 已完成模組清單
- Step 1：
- `stock`
- `stock_price_daily`
- `portfolio`
- `portfolio-dashboard`
- Step 2：
- `risk_rule`
- `portfolio_risk_result`
- `portfolio_risk_reason`
- Step 3：
- `portfolio_daily_snapshot`
- `daily_report`
- `scheduler_job_log`
- `jobs/run-daily-portfolio-risk-report`
- Step 4：
- `portfolio_alert_setting`
- `portfolio_alert_event`
- `jobs/run-alert-polling`

## 5. Menu / Permission / URL Permission 概況

### 5.1 角色與帳號
- 角色：`ROLE_INVEST_ADMIN`
- baseline 管理者：`admin`（密碼 `admin123`，僅供初始化驗證，部署後需更換）

### 5.2 權限命名
- 全部以 `INVEST_` 前綴。
- 主要群組：
- 基本功能：`INVEST_DASHBOARD_VIEW`、`INVEST_PORTFOLIO_*`、`INVEST_STOCK_*`、`INVEST_STOCK_PRICE_DAILY_*`、`INVEST_RISK_*`、`INVEST_DAILY_REPORT_VIEW`、`INVEST_ALERT_*`。
- 任務執行：`INVEST_JOB_RUN_DAILY_REPORT`、`INVEST_JOB_RUN_ALERT_POLLING`。
- 系統骨架：`INVEST_SYS_*`。

### 5.3 菜單
- 儀表板：`INVEST_DASHBOARD`。
- 基本功能群組：`INVEST_BASIC_FEATURES`。
- 持股中心、股票主資料、每日行情、每日報告、警示事件。
- 系統設定群組：`INVEST_SYSTEM_SETTINGS`。
- 用戶/角色/權限/菜單/URL 權限/系統維護/排程/LINE 群組（目前為骨架頁）。

### 5.4 URL 權限
- `url_permissions` 以 method + path 管理。
- `/api/invest/auth/*` 有 public/bypass 設定。
- 其餘 `/api/invest/**` 受 JWT + URL 權限過濾控制。

## 6. Migration / Seed 結構

### 6.1 目錄分層
- `database/invest/infra/`：只放 DB 建立與授權（非 app schema）。
- `database/invest/migrations/schema/`：只放 schema/table/index/constraint。
- `database/invest/migrations/seed-baseline/`：必要基準資料（ACL/admin/risk rules）。
- `database/invest/migrations/seed-demo/`：可選示範資料，正式環境預設不啟用。
- `database/invest/migrations/maintenance/`：維護修復腳本（可選）。
- `database/invest/migrations/rollback/`：手動回退腳本。

### 6.2 from 0 順序
1. `infra/00_create_invest_db_and_user.sql`
2. `schema/01_create_invest_step1.sql`
3. `schema/02_create_invest_step2_risk.sql`
4. `schema/03_create_invest_acl.sql`
5. `schema/04_create_invest_step3_report.sql`
6. `schema/05_create_invest_step4_alert.sql`
7. `seed-baseline/01_seed_risk_rules.sql`
8. `seed-baseline/02_seed_acl.sql`
9. `seed-baseline/03_hide_invest_risk_results_menu.sql`
10. `seed-baseline/04_set_admin_password_baseline.sql`
11. `seed-baseline/05_seed_system_menu_permission.sql`
12. `seed-baseline/06_seed_step3_report_acl.sql`
13. `seed-baseline/07_seed_step4_alert_acl.sql`
14. （可選）`seed-demo/01_seed_step1_demo_data.sql`

## 7. 本地部署方式

### 7.1 整包（all）
- 啟動：`./deploy_local.sh build`

### 7.2 invest-only
- 啟動：`./deploy_local.sh build invest`
- 僅啟/停：`./deploy_local.sh normal invest`、`./deploy_local.sh down invest`
- compose：`docker-compose.invest.yml`
- env：可使用 `.env.invest.prod` 覆蓋。

## 8. 遠端部署方式

### 8.1 指令入口
- 腳本：`./scripts/deployment/deploy-to-server-v1.1.sh`
- 支援模式：
- CLI：`all`、`personal`、`church`、`invest`
- 無參數：互動式選單（1~4）

### 8.2 常用方式
- all：`./scripts/deployment/deploy-to-server-v1.1.sh`
- invest-only：`./scripts/deployment/deploy-to-server-v1.1.sh invest`

## 9. Smoke Test 重點

### 9.1 基本健康檢查
- `GET /api/invest/hello`
- `GET /invest-admin/`

### 9.2 登入與權限
- `POST /api/invest/auth/login`
- `GET /api/invest/auth/current-user`
- 驗證 token key：`invest_access_token`、`invest_refresh_token`
- 驗證 menu 來自 invest ACL。

### 9.3 業務 API
- `GET /api/invest/stocks/paged`
- `GET /api/invest/portfolios/paged`
- `GET /api/invest/portfolio-risk-results/paged`
- `GET /api/invest/daily-reports/paged`
- `GET /api/invest/portfolio-alert-events/paged`

### 9.4 run-now 驗證
- `POST /api/invest/jobs/run-daily-portfolio-risk-report`
- `POST /api/invest/jobs/run-alert-polling`
- 驗證資料表寫入：
- `scheduler_job_log`
- `portfolio_daily_snapshot`
- `daily_report`
- `portfolio_alert_event`

## 10. 已知限制
- V2 功能尚未實作：強勢股偵測、機會股清單、strength score、轉強提醒。
- 不做推播（LINE/Email/Push）。
- `ABNORMAL_DROP` 最小版使用記憶體基準：
- 服務重啟會重置基準。
- 多實例不保證一致。
- 去重仍以資料庫條件為主。
- 系統設定相關頁面目前為骨架（placeholder），尚未接深度後端功能。
- `admin/admin123` 僅供初始驗證，不可作為長期正式帳密。

## 11. 下一階段候選方向
- 方向 A：遠端部署與維運強化
- 完整備份/還原自動化。
- deployment smoke 腳本標準化。
- 監控與告警（容器健康、DB 連線、排程失敗）。
- 方向 B：V2 功能（仍維持可解釋、非買進指令）
- 強勢股偵測
- 機會清單
- 可觀察 / 等待回檔 / 條件成立再評估
- 方向 C：系統設定骨架深化
- 用戶、角色、權限、菜單、URL 權限管理頁串接完整 CRUD。

