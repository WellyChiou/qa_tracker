# Platform Architecture Overview

## A. 專案 module 結構
- `backend-auth-core`：抽象的 auth/permission/menu 組裝邏輯，供兩個後端共用。包含 facade、assembler、permission mapper 等核心元件。皆由 multi-module 建構。
- `backend-personal`：個人系統後端，透過 `backend-auth-core` 處理認證/授權相關回應，並維持本身的 domain entity、service 及 controller。
- `backend-church`：教會後端，與 personal 共享 `backend-auth-core` 但維持獨立資料模型、repository 與業務邏輯。
- `frontend-personal`：個人後台前端，使用共享的 `shared/composables/useAuthCore`、`shared/utils/authGuard` 以及 `shared/utils/permission` 來判斷登入與權限，並以 `currentUser.menus` 作為 sidebar 來源。
- `frontend-church-admin`：教會後台前端，同樣依賴 shared auth core 與 permission helper，確保 login/current-user/permission 行為與 personal 一致。

## B. auth / permission / menu 架構說明
1. 兩後端透過 `backend-auth-core` 產生統一的 auth DTO（`AuthLoginResponse`、`AuthCurrentUserResponse`、`AuthRefreshResponse`、`AuthLogoutResponse`），表內包含 `menus`、`roles`、`permissions`。
2. 前端的 `shared/composables/useAuthCore.js` 管理 login/logout/token、currentUser 與 `checkAuth`，並由 `shared/utils/authGuard.js` 及 `shared/utils/permission.js` 進行 route/menu/按鈕授權。
3. menu 構建由後端在 response 中提供樹狀資料，前端 layout 以 `currentUser.menus` 為主來源。permission/restrict guard 以 `requiredPermission` 或 `strictPermissions` 設定啟用。

## C. Maven multi-module build 方法
1. 專案根目錄為 aggregator（`pom.xml` 列出三個 module：`backend-auth-core`, `backend-personal`, `backend-church`）。
2. 主要打包命令（不加 `-DskipTests` 可觸發完整測試）：
   - `mvn -pl backend-personal -am clean package`
   - `mvn -pl backend-church -am clean package`
   兩者會自動先構建 `backend-auth-core`，再編譯指定模組。
3. 可搭配 `-DskipTests` 或 `-B` 來加速 CI/產出流程。

## D. Docker build 方法
1. 每個後端的 Dockerfile 以 multi-stage build：
   - 第一階段使用 `maven:3.8-eclipse-temurin-17`，複製 root `pom.xml` 與三個 module 的 `pom` 檔，再下載依賴並執行 `mvn -pl backend-personal -am clean package -DskipTests -B` 或 `mvn -pl backend-church -am clean package -DskipTests -B`。
   - 第二階段使用 `eclipse-temurin:17-jre-jammy`，把對應模組的 jar、備份腳本複製進容器，並設定環境變數。
2. 建構命令仍為 `docker compose build backend-personal` / `backend-church`，但因為 build context 已是 repo root，加上 `.dockerignore` 排除了各 module 的 `target`，所以 `backend-auth-core` 會隨 root 一併被 Docker copy。

## E. docker-compose 啟動方式
1. `docker-compose.yml` 現在對 backend 服務設定 `build.context: .`，藉此把三個 module 包含在 container build context。檔案也定義了必要的 volumes、環境變數與 healthcheck。
2. 啟動順序會先建構 `backend-personal` / `backend-church`，再啟動前端與 nginx。請使用 `docker compose build backend-personal` / `backend-church` 檢查建構，再以 `docker compose up` 啟動整個 stack。
