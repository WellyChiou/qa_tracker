# Invest Phase 2-1 操作手冊

最後更新日期：2026-03-20  
適用範圍：Invest V1 + V1.5 + V2 Phase 2-1（強勢股/機會訊號第一版）

---

## 1. 管理員日常操作流程

### 1.1 登入
1. 開啟 `https://<你的網域>/invest-admin/`。
2. 使用管理員帳號登入（目前 baseline 常見為 `admin / admin123`）。
3. 登入成功後，前端會以 invest 專用流程載入：
   - current-user：`/api/invest/auth/current-user`
   - 菜單：由 invest ACL 權限決定可見項目

### 1.2 維護 default watchlist（預設觀察清單）
1. 進入「強勢股分析」頁：`/invest-admin/strong-stocks`。
2. 在「預設觀察清單」區塊：
   - 選股票
   - 填備註（可留空）
   - 按「新增標的」
3. 需要移除時，按該列「移除」。

對應 API：
- `GET /api/invest/watchlists/default`
- `GET /api/invest/watchlists/default/items/paged`
- `POST /api/invest/watchlists/default/items`
- `DELETE /api/invest/watchlists/default/items/{itemId}`

### 1.3 手動執行 run-price-update（更新我的持股行情）
1. 進入「每日行情管理」頁：`/invest-admin/stock-price-dailies`。
2. 按「更新我的持股行情」。
3. 等待成功訊息（會顯示更新結果摘要）。

對應 API：
- `POST /api/invest/jobs/run-price-update`

### 1.4 手動執行 run-market-analysis（市場分析）
1. 進入「強勢股分析」頁。
2. 選擇分析範圍：
   - `持股 + 觀察清單`（`HOLDINGS_AND_WATCHLIST`，預設）
   - `僅持股`（`HOLDINGS_ONLY`）
3. 按「手動執行市場分析」。

對應 API：
- `POST /api/invest/jobs/run-market-analysis`
- 可帶參數：`scope=HOLDINGS_ONLY|HOLDINGS_AND_WATCHLIST`

### 1.5 查看強勢股頁
頁面：`/invest-admin/strong-stocks`

建議操作：
1. 先看查詢條件（交易日、強度等級、範圍、代碼）。
2. 先看列表摘要欄位：
   - 強度分數、等級、建議、品質、摘要
3. 需要深入時按「明細」查看 factor。
4. factor 預設只顯示前 3 筆，其餘可展開（避免畫面過長）。

### 1.6 查看機會訊號頁
頁面：`/invest-admin/opportunities`

建議操作：
1. 先用狀態（ACTIVE/EXPIRED）與建議碼過濾。
2. 看摘要與前 3 條原因。
3. 按「明細」查看條件說明、完整原因與免責提醒。

---

## 2. 分析結果如何解讀

### 2.1 `strength_score`
- 範圍：`0 ~ 100`
- 來源：多個因子分數加總（上限 100）
- 用途：強度量化，分數越高代表相對偏強，但不代表買進指令

### 2.2 `strength_level`
第一版常用等級：
- `STRONG`：強勢
- `GOOD`：偏強
- `NEUTRAL`：中性
- `WEAK`：偏弱

### 2.3 factor breakdown（因子拆解）
第一版主要因子：
- 趨勢（MA20）
- 動能（5日/20日報酬）
- 量能確認（20日均量）
- 穩定度（10日波動）
- 回撤（20日高點）

每個因子都提供：
- 原始值
- 門檻規則
- 分數貢獻
- 白話解釋

### 2.4 `opportunity_signal`
第一版定位：觀察訊號，不是買進訊號。  
常見欄位：
- `signal_type`：訊號分類
- `signal_key`：去重鍵（避免同條件重複訊號）
- `status`：`ACTIVE` / `EXPIRED`

### 2.5 `recommendation_code`
第一版觀察型建議：
- `OBSERVE`：可觀察
- `WAIT_PULLBACK`：等待回檔
- `REEVALUATE_WHEN_CONDITION_MET`：條件成立再評估
- `NOT_SUITABLE_CHASE`：不建議追價

### 2.6 `data_quality`
統一語意：
- `GOOD`：資料完整且新鮮
- `PARTIAL`：部分欄位缺漏
- `STALE`：資料過舊（目前以日曆天規則判斷）
- `INSUFFICIENT`：資料不足（常見為不足 20 日）

### 2.7 `priceSource` / `currentPriceTradeDate`
這兩個在持股列表/明細很重要：
- `priceSource` 是「持股估值語意」
  - `LATEST_CLOSE`：使用最新收盤價
  - `COST_FALLBACK`：回退使用成本估值
- `currentPriceTradeDate` 是本次現價對應的交易日

請注意：`stock_price_daily.data_source` 與 `portfolio.priceSource` 是不同概念，不可混用。

---

## 3. 常見問題排查

### 3.1 沒資料（強勢股/機會訊號為空）
優先檢查：
1. 是否先執行 `run-price-update`
2. 是否再執行 `run-market-analysis`
3. 該股票是否有足夠 `stock_price_daily`（至少 20 筆）
4. 帳號是否有查詢權限（ACL）

### 3.2 現價不更新
優先檢查：
1. `run-price-update` 回傳是否 `SUCCESS` 或 `PARTIAL_FAILED`
2. `stock_price_daily` 是否真的新增/更新到最新交易日
3. 持股頁 `priceSource` 是否為 `COST_FALLBACK`

若仍為 `COST_FALLBACK`，通常代表最新行情缺漏或資料品質不足。

### 3.3 watchlist 沒分析到
優先檢查：
1. 這次 `scope` 是否誤選 `HOLDINGS_ONLY`
2. default watchlist 是否有 active 標的
3. 觀察標的是否有行情資料

另外，系統有 scope 規則：
- `universe_type=HOLDING` → `watch_scope_id=0`
- `universe_type=WATCHLIST` → `watch_scope_id=watchlist_id`

### 3.4 signal 沒出現
可能原因：
1. `data_quality=INSUFFICIENT` 或 `STALE`
2. 分數低或條件不成立
3. 同條件已存在 `signal_key`，會做 upsert 而非重複新增

### 3.5 `data_quality=STALE / INSUFFICIENT`
- `STALE`：先更新行情，再重新分析
- `INSUFFICIENT`：先補足歷史行情（至少 20 日）再分析

---

## 4. 管理員維運操作

### 4.1 確認 migration 是否已執行
可在資料庫查 `schema_migration_history`：

```sql
USE invest;
SELECT id, version, script_name, executed_at
FROM schema_migration_history
ORDER BY id DESC;
```

檢查特定版本（例如 S008/S009/B009）：

```sql
USE invest;
SELECT version, script_name, executed_at
FROM schema_migration_history
WHERE version IN ('S008', 'S009', 'B009')
ORDER BY version;
```

### 4.2 手動執行 migration runner
在專案根目錄執行（正式環境不含 demo seed）：

```bash
./scripts/migration/run-invest-migrations.sh \
  --compose-file docker-compose.invest.yml \
  --env-file .env.invest.prod
```

如需 demo seed（僅測試用途）：

```bash
./scripts/migration/run-invest-migrations.sh \
  --compose-file docker-compose.invest.yml \
  --env-file .env.invest.prod \
  --include-demo
```

### 4.3 查 job log / job detail
目前建議至少查三類：

1. 排程批次紀錄（daily report / alert polling 等）
```sql
USE invest;
SELECT id, job_name, run_date, status, started_at, finished_at, message
FROM scheduler_job_log
ORDER BY id DESC
LIMIT 50;
```

2. 行情更新批次（Phase 1）
```sql
USE invest;
SELECT id, user_id, job_name, batch_id, run_mode, status, total_count, success_count, fail_count, started_at, finished_at, message
FROM price_update_job_log
ORDER BY id DESC
LIMIT 50;
```

3. 行情更新明細（失敗原因最常在這裡）
```sql
USE invest;
SELECT id, job_log_id, ticker, status, reason, trade_date, fetched_at
FROM price_update_job_detail
WHERE job_log_id = <你的 job_log_id>
ORDER BY id ASC;
```

補充：
- `run-market-analysis` 目前回傳執行摘要（SUCCESS / PARTIAL_FAILED / FAILED），分析結果落在 `strength_snapshot` 與 `opportunity_signal`。

---

## 5. 已知限制

1. 目前不是即時行情，非 WebSocket/streaming。
2. 目前分析範圍是「持股 + default watchlist」，不是全市場掃描。
3. 目前輸出為觀察型建議，不提供買進指令。
4. provider 與資料延遲仍受來源限制，不可視為即時報價。
5. 服務重啟或多實例情境下，部分短期記憶體輔助狀態可能重置（事件去重仍以 DB 規則為主）。

---

## 附錄 A：建議的每日操作節奏

1. 先跑 `run-price-update`  
2. 再跑 `run-market-analysis`（預設 `HOLDINGS_AND_WATCHLIST`）  
3. 進強勢股頁看資料品質與因子  
4. 進機會訊號頁看 ACTIVE 訊號與理由  
5. 若品質為 `STALE/INSUFFICIENT`，先補資料再解讀建議

---

## 附錄 B：給 Codex/Cursor 的最小操作提示

可直接使用以下指令描述：

> 請先檢查 invest 目前最新 `price_update_job_log`、`price_update_job_detail`、`strength_snapshot`、`opportunity_signal`，再依 `data_quality` 與最近一次 `run-market-analysis` 結果，輸出今日管理員操作建議（繁體中文，禁止買進指令）。

