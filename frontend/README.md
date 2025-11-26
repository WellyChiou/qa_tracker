# Vue 3 前端應用

這是使用 Vue 3 + Vite 構建的前端應用程序。

## 技術棧

- **Vue 3** - 最新版本的 Vue.js 框架
- **Vue Router 4** - 官方路由管理器
- **Vite** - 下一代前端構建工具
- **Composition API** - Vue 3 的組合式 API

## 項目結構

```
frontend/
├── src/
│   ├── main.js              # 應用入口
│   ├── App.vue              # 根組件
│   ├── style.css            # 全局樣式
│   ├── router/              # 路由配置
│   │   └── index.js
│   ├── composables/         # 組合式函數
│   │   ├── useApi.js        # API 服務
│   │   └── useAuth.js       # 認證相關
│   └── views/               # 頁面組件
│       ├── Login.vue
│       ├── Dashboard.vue
│       ├── Expenses.vue
│       ├── Tracker.vue
│       └── admin/
│           ├── Users.vue
│           ├── Roles.vue
│           ├── Permissions.vue
│           ├── Menus.vue
│           └── UrlPermissions.vue
├── index.html               # HTML 模板
├── package.json             # 依賴配置
├── vite.config.js          # Vite 配置
└── Dockerfile              # Docker 構建文件
```

## 開發

### 安裝依賴

```bash
npm install
```

### 啟動開發服務器

```bash
npm run dev
```

開發服務器將在 `http://localhost:5173` 啟動。

### 構建生產版本

```bash
npm run build
```

構建結果將輸出到 `dist/` 目錄。

### 預覽生產構建

```bash
npm run preview
```

## Docker 構建

### 構建 Docker 鏡像

```bash
docker build -t expense-tracker-frontend .
```

### 運行容器

```bash
docker run -p 80:80 expense-tracker-frontend
```

## 功能特性

- ✅ 用戶認證（登入/登出）
- ✅ 儀表板（系統狀態、快速訪問）
- ✅ 記帳管理（新增、編輯、刪除、複製記錄）
- ✅ 記錄篩選和分頁
- ✅ 月度統計（收入、支出、淨收入）
- ✅ 響應式設計
- ✅ 路由守衛（認證保護）

## API 集成

前端通過 `useApi` composable 與後端 API 通信。API 基礎 URL 配置在 `src/composables/useApi.js` 中。

## 路由

- `/login` - 登入頁面
- `/` - 儀表板
- `/expenses` - 記帳管理
- `/tracker` - 追蹤頁面（開發中）
- `/admin/*` - 管理頁面（開發中）

## 注意事項

1. 確保後端 API 服務運行在 `http://localhost:8080`
2. 開發環境下，Vite 會自動代理 `/api` 請求到後端
3. 生產環境需要配置 nginx 反向代理或 CORS

