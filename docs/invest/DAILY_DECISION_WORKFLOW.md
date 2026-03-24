# 每日投資決策流程（Invest System）

## 1. 每日固定流程（3～5 分鐘）

### Step 1：更新行情
- 進入 Dashboard，點擊「更新我的持股行情」。
- 或進入「系統設定 > 排程管理」，執行 `PRICE_UPDATE_HOLDINGS` 的 `Run Now`。
- 目的：先確保今天看到的是最新可用價格資料。

### Step 2：執行市場分析
- 在 Dashboard 點擊「執行市場分析」。
- 或進入排程管理，執行 `MARKET_ANALYSIS` 的 `Run Now`。
- 目的：更新持股強弱與機會訊號。

### Step 3：查看 Dashboard
- 先看 `Data As Of` 是否為最新。
- 再看資料品質是否出現 `STALE` 或 `INSUFFICIENT`。
- 若資料品質不佳，先不要做進一步判斷。

### Step 4：快速判讀
- 看持股強弱：`STRONG / GOOD / NEUTRAL / WEAK`。
- 看機會訊號：`OBSERVE / WAIT_PULLBACK / REEVALUATE / NOT_SUITABLE_CHASE`。
- 看 alert 是否有異常（尤其停損或異常下跌）。

### Step 5：決策分類（非常重要）
- 只允許以下四種：
  - 偏強觀察
  - 等待回檔
  - 暫不判斷
  - 資料不足
- 禁止：
  - 建議買進
  - 追價

## 2. 判讀規則（重點）

### 2.1 Data Quality 判斷
- `GOOD`：可參考。
- `PARTIAL`：保守看，降低決策強度。
- `STALE`：不做判斷，先更新資料。
- `INSUFFICIENT`：先忽略，不做結論。

### 2.2 Strength 解讀
- `STRONG`：偏強，觀察是否延續。
- `GOOD`：可留意，但不急著行動。
- `NEUTRAL`：中性，不急不追。
- `WEAK`：偏弱，先注意風險。

### 2.3 Opportunity 解讀
- `OBSERVE`：可觀察。
- `WAIT_PULLBACK`：等待回檔，不追價。
- `REEVALUATE_WHEN_CONDITION_MET`：條件成立再看。
- `NOT_SUITABLE_CHASE`：不追。

## 3. 常見錯誤（避免踩雷）

1. 資料還沒更新就開始判斷。  
2. 看到 `STALE` 還硬做結論。  
3. 看到 `INSUFFICIENT` 還硬分析。  
4. 把 `OBSERVE` 當成買進訊號。  
5. 忽略 alert / risk 提醒。  

## 4. 什麼時候不要使用這套系統

1. 市場極端波動（例如重大事件當日）。  
2. 當日資料尚未更新完成。  
3. 多數標的 `data_quality` 不是 `GOOD`。  
4. API 或 job 執行異常。  

## 5. 補充

- 本系統是「觀察與判讀工具」。
- 不提供買進建議。
- 不保證獲利。
- 最終決策仍需自行判斷與風險控管。
