你現在要做的是「依據 baseline 文件，自動生成一個全新專案 skeleton」，請嚴格依照文件建立，不要自行發明架構。

請嚴格遵守：

* 必須依據 NEW_PROJECT_BASELINE_FROM_INVEST.md
* 不要套用舊 church / personal 專案結構
* 不要加入未在 baseline 中的複雜功能
* 不要一次做完整產品，只做「可啟動的 skeleton」
* 必須低風險、可逐步擴展

==================================================
一、目標
====

建立一個「全新專案骨架（skeleton）」，包含：

* frontend（Vue3 admin）
* backend（Spring Boot）
* database（migration + seed）
* docker-compose
* 基礎 maintenance / scheduler / backup 結構（只要骨架）

此專案不承接 invest 業務，視為全新 domain。

==================================================
二、專案命名（請使用以下）
=============

project code：platform

請產生：

* backend/platform
* frontend/platform-admin
* database/platform

API prefix：

* /api/platform

==================================================
三、monorepo 結構（必須符合）
===================

請建立：

* backend/platform
* frontend/platform-admin
* database/platform
* docker-compose.yml（整合 platform）

若已有其他 module，請以最小影響新增，不要破壞既有結構。

==================================================
四、backend skeleton（Spring Boot）
===============================

請建立最小可運作 backend：

1. 基本結構

   * controller
   * service
   * config
   * scheduler（接既有模式）
   * maintenance（基礎結構）

2. API 規範

   * 支援 paged / options / all（只需建立範例 endpoint）

3. auth

   * 沿用既有 auth-core / JWT（不要重寫）

4. scheduler

   * 接入既有 scheduler 架構
   * 建立一個 sample job（例如 PLATFORM_HEALTH_CHECK）

5. backup

   * 只建立接口與結構（不需完整實作）

==================================================
五、frontend skeleton（Vue3）
=========================

請建立：

* platform-admin（Vue3 + Vite）

包含：

1. 基本 admin layout

   * header
   * sidebar
   * content

2. 基本頁面

   * dashboard（空頁即可）
   * scheduler page（沿用既有 UI 模式）
   * maintenance page（空骨架）

3. route / auth guard

   * 沿用 shared 模組（若存在）

==================================================
六、database skeleton
===================

請建立：

* database/platform

包含：

1. schema.sql

   * 最小 tables（users / roles / permissions 可先留空或 stub）

2. seed.sql

   * 基本 admin 帳號（若 baseline 有）

3. migration runner 結構（沿用既有）

==================================================
七、docker-compose
================

請整合：

* mysql（沿用）
* backend-platform
* frontend-platform-admin

並確保：

* API 可透過 /api/platform 存取
* frontend 可正常 build

==================================================
八、限制（非常重要）
==========

這一輪禁止：

* 不要實作完整 business logic
* 不要實作 invest domain
* 不要加入 AI / strategy / 分析功能
* 不要重做 scheduler
* 不要重做 auth
* 不要設計新架構

只做：

👉 baseline 骨架 + 最小可運作

==================================================
九、驗收標準
======

請完成後確認：

1. backend-platform 可啟動
2. frontend-platform-admin 可 build
3. /api/platform 有基本 endpoint 可回應
4. scheduler sample job 可出現在 job list
5. docker-compose 可成功啟動整個平台

==================================================
十、輸出要求
======

請回報：

# 1. 新增檔案結構

* backend/platform
* frontend/platform-admin
* database/platform

# 2. 建立的 skeleton 能力

* backend 有哪些 endpoint
* frontend 有哪些頁面
* scheduler sample job

# 3. 是否完全依 baseline 建立

* 哪些地方有依 baseline
* 是否有偏離（若有請說明）
