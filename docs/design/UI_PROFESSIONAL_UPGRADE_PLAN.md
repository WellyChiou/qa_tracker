# UI Professional Upgrade Plan

## Goal

為三個前端建立一套可執行的專業化改版方向：

- `frontend-personal`
- `frontend-church`
- `frontend-church-admin`

原則：

- 不先改業務邏輯
- 先統一視覺語言與設計系統
- 再依產品定位分別升級畫面
- 避免只做局部換色或零碎美化

---

## Current Assessment

目前三個站的問題不是功能不足，而是設計層不夠一致。

### Shared issues

- 字體與配色策略不統一，仍偏預設字型與傳統後台色感
- 大量頁面使用各自的 scoped CSS，缺少穩定的 tokens / components 層
- 元件有重複，但視覺規則沒有真正模組化
- admin modal、table、form、page header 雖已有初步統一，但層次還不夠高級
- public / admin 的品牌辨識度仍不夠明確

### `frontend-personal`

- 已有 shared styles 基礎，但視覺仍偏「工程型後台」
- 資訊密度高，對比與節奏尚可再專業化
- 財務 / tracker / admin 缺少更明確的產品感與數據產品語言

### `frontend-church`

- 已經有較完整的 public site 方向
- 色彩與卡片語言比 personal 好，但仍偏模板式
- Hero、內容節奏、影像區塊、活動與事工內容可再提升品牌感

### `frontend-church-admin`

- 有初步 tokens 與 admin shell
- 但大量 modal 與管理頁仍保留舊式後台觀感
- 頁面間的一致性與「專業工具感」還不夠

---

## Product Positioning

三個站不能共用同一種氣質，否則會像同皮不同站。

### `frontend-personal`

定位：`Private operations console`

關鍵字：

- 精準
- 冷靜
- 高效率
- 數據導向

建議風格：

- 深海軍藍 + 電光藍 + 冷白底
- 緊實但不壓迫的資訊排版
- 強化數據卡、表格、狀態標記、趨勢感

### `frontend-church`

定位：`Warm editorial church website`

關鍵字：

- 溫暖
- 信任
- 明亮
- 內容導向

建議風格：

- 暖白、森林綠、柔金、少量深墨色
- 大版面 hero、內容段落節奏、攝影與文字並重
- 更像品牌網站 / editorial site，不像系統頁

### `frontend-church-admin`

定位：`Mission control for church operations`

關鍵字：

- 清楚
- 專業
- 穩定
- 管理工具感

建議風格：

- 石板藍、雲白、深墨、少量綠色成功態
- 以 dashboard / command center 邏輯設計
- 更強的 page header、filters、table toolbar、empty state、status tags

---

## Design System Strategy

建議建立一層 shared design foundation，但不要強迫三個站長得一樣。

### Shared foundation

建議抽成 `shared/styles/` 與 `shared/ui-spec/` 的共用規則：

- color tokens 命名規範
- typography scale
- spacing scale
- radius / shadow / border 規則
- motion duration / easing
- form / button / card / modal / table 基礎樣式
- empty / loading / error / success state 樣式

### Brand layer

每個站各自保留：

- theme tokens
- page shell
- hero / dashboard 風格
- icon / illustration / accent pattern

### Component layer

優先統一這些高頻元件：

- AppShell / AdminShell
- PageHeader
- StatCard
- DataTable
- SearchFilterBar
- ModalShell
- FormSection
- EmptyState
- ConfirmDialog
- StatusBadge

---

## Execution Phases

### Phase 1: Design foundation

先做不碰頁面的底層規則：

- 定義三站 theme tokens
- 定義 typography / spacing / elevation
- 統一 button / input / select / textarea / table / modal / badge
- 整理 shared CSS import 結構

交付：

- `shared/styles/tokens-*.css`
- `shared/styles/admin-primitives.css`
- `shared/styles/public-primitives.css`
- `shared/styles/motion.css`

### Phase 2: Shell redesign

先重做整體框架，不先重做所有細頁：

- `frontend-personal`：Top nav + dashboard shell + admin page header
- `frontend-church`：header / footer / hero / section shell
- `frontend-church-admin`：admin navbar / content wrapper / page meta / toolbar shell

交付：

- 三個站先有一致的第一印象
- 後續頁面改版成本下降

### Phase 3: High-impact pages

先改最有感、最常用的頁面。

#### `frontend-personal`

優先順序：

1. `Dashboard.vue`
2. `Expenses.vue`
3. `Tracker.vue`
4. `Login.vue`
5. `Maintenance.vue`

#### `frontend-church`

優先順序：

1. `Home.vue`
2. `About.vue`
3. `Activities.vue`
4. `Groups.vue`
5. `ServiceSchedule.vue`

#### `frontend-church-admin`

優先順序：

1. `儀表板 / 首頁`
2. `Maintenance.vue`
3. `LineGroups.vue`
4. `人員 / 權限 / 菜單 / URL 權限管理`
5. `服事表管理`

### Phase 4: Deep polish

最後補完整體專業度：

- loading skeleton
- empty states
- success / error feedback
- micro motion
- mobile breakpoint tuning
- icon consistency
- chart / stat visualization polish

---

## Visual Direction Proposal

### Personal

- 主字體：`"IBM Plex Sans TC", "Noto Sans TC", sans-serif`
- 數字字體：`"JetBrains Mono", monospace`
- 主視覺：深藍漸層、低飽和背景、強化數字卡
- 關鍵元件：高對比 KPI 卡、精簡工具列、專業表格

### Church

- 主字體：`"Noto Serif TC"` 搭配 `"Noto Sans TC"`
- 主視覺：大留白、暖白紙感、綠金點綴、內容卡片更 editorial
- 關鍵元件：hero、event cards、group cards、content sections

### Church Admin

- 主字體：`"IBM Plex Sans TC"` 或 `"Manrope"` 搭配 `"Noto Sans TC"`
- 主視覺：高可讀工具介面、淺色操作板、層次清楚的 section
- 關鍵元件：toolbar、filters、status badge、admin tables、modal shell

---

## Technical Recommendations

### CSS architecture

- 保留現有 Vue SFC 結構
- 先用 CSS variables 與 shared stylesheet 整理
- 不急著引入大型 UI framework
- 優先消除各頁散落的重複按鈕 / 表單 / modal style

### Refactor rules

- 先抽共用 class，不先大搬 template
- 每改一頁，順手把 page header / filter bar / table 升級
- 新樣式命名採 BEM 或 page-scope 規則，不混亂疊加

### Accessibility baseline

- 明確 focus state
- 表單 label 與錯誤提示一致
- 色彩對比通過基本可讀性
- mobile 可操作區塊不少於 40px 高

---

## Recommended Order From Now

建議直接照這個順序開工：

1. 建立三站 tokens 與 shared primitives
2. 先重做 `frontend-church-admin` shell
3. 再重做 `frontend-personal` dashboard / expenses
4. 最後重做 `frontend-church` public home / about / activities

原因：

- `church-admin` 最能快速建立「專業感」
- `personal` 是高頻工具站，改版回饋最直接
- `church` public site 需要更多品牌與內容設計，應放在第三波

---

## Definition of Done

以下條件達成，才算 UI 專業化第一階段完成：

- 三個站都有清楚的獨立視覺定位
- 共用 primitives 已建立，不再靠頁面各自亂寫
- shell、page header、button、input、table、modal 已統一
- 每個站至少完成 2 到 3 個代表性頁面重做
- mobile 與 desktop 均可正常使用
- 不改動既有業務邏輯與 API 行為

---

## Next Step

下一步建議直接開始：

### Sprint A

- 重構 `frontend-church-admin` 的 shell、page header、table/filter 工具列
- 同步建立 admin design primitives

### Sprint B

- 重構 `frontend-personal` 的 dashboard / expenses 視覺系統

### Sprint C

- 重構 `frontend-church` 的 home / about / activities 品牌頁
