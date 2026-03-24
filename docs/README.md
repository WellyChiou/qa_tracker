# 文件索引（Monorepo）

本文件用來快速定位目前 repo 的可用文件。  
原則：先看「核心基線」與「操作手冊」，再看部署與故障排除；歷史規劃稿放 `docs/archive/`。

## 0) 建議閱讀順序（30 秒）
1. 先看基線：`NEW_PROJECT_BASELINE_FROM_INVEST.md`
2. 再看交接：`INVEST_PROJECT_HANDOVER.md`
3. 再看操作：`docs/invest/`
4. 發生問題時看：`docs/TROUBLESHOOTING.md`

## 1) 核心基線（優先閱讀）
- ⭐ 首先閱讀：[NEW_PROJECT_BASELINE_FROM_INVEST.md](../NEW_PROJECT_BASELINE_FROM_INVEST.md)
- ⭐ 其次閱讀：[INVEST_PROJECT_HANDOVER.md](../INVEST_PROJECT_HANDOVER.md)
- [NEW_PROJECT_BASELINE_FROM_INVEST.md](../NEW_PROJECT_BASELINE_FROM_INVEST.md)
  - 新專案 baseline（獨立 DB/ACL、部署、scheduler、backup、maintenance MVP）。
- [INVEST_PROJECT_HANDOVER.md](../INVEST_PROJECT_HANDOVER.md)
  - Invest 第一階段交接狀態與可部署範圍。
- [MONOREPO_ADMIN_BASELINE.md](../MONOREPO_ADMIN_BASELINE.md)
  - Monorepo admin 演進決策（跨 personal/church/invest 的平台視角）。

## 2) Invest 操作手冊
- [PHASE2_1_OPERATION_MANUAL.md](./invest/PHASE2_1_OPERATION_MANUAL.md)
  - Invest 日常維運與功能操作手冊。
- [DAILY_DECISION_WORKFLOW.md](./invest/DAILY_DECISION_WORKFLOW.md)
  - 投資決策日常流程（非技術版）。

## 3) 部署與維運
- [docs/deployment/README.md](./deployment/README.md)
- [docs/deployment/QUICK_START.md](./deployment/QUICK_START.md)
- [README-local-deploy.md](../README-local-deploy.md)
- [docs/TROUBLESHOOTING.md](./TROUBLESHOOTING.md)
  - 已包含 Frontend 白屏專章。
- [docs/FRONTEND_TROUBLESHOOTING.md](./FRONTEND_TROUBLESHOOTING.md)
  - 舊入口（內容已合併到 `docs/TROUBLESHOOTING.md`）。

## 4) 資料庫相關
- [database/README.md](../database/README.md)
- [database/invest/README.md](../database/invest/README.md)
- [database/MIGRATION_GUIDE.md](../database/MIGRATION_GUIDE.md)
- [database/diagnostics/README.md](../database/diagnostics/README.md)

## 5) 系統管理/安全
- [docs/admin/ADMIN_SYSTEM_GUIDE.md](./admin/ADMIN_SYSTEM_GUIDE.md)
- [docs/admin/DYNAMIC_URL_PERMISSIONS.md](./admin/DYNAMIC_URL_PERMISSIONS.md)
- [docs/SECURITY_IMPLEMENTATION.md](./SECURITY_IMPLEMENTATION.md)

## 6) 歷史文件（已歸檔）
以下文件已移到 `docs/archive/`，保留參考但不作為目前 baseline：
- `docs/archive/development/BACKEND_SPLIT_PHASE2_STATUS.md`
- `docs/archive/development/BACKEND_SPLIT_PHASE3_PLAN.md`
- `docs/archive/development/BACKEND_SPLIT_FILE_MANIFEST.md`
- `docs/archive/PROJECT_REFRESH_STATUS.md`

## 7) 使用建議
- 新專案啟動：先看「核心基線」。
- Invest 實際操作：看 `docs/invest/`。
- 部署或白屏問題：先看 `docs/TROUBLESHOOTING.md`。
- 舊規劃稿：到 `docs/archive/` 查，不要直接當現況規格。
