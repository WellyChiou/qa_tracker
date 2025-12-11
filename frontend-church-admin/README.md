# 教會後台管理系統

這是教會後台管理系統的前端專案，已從 `frontend-church` 中分離出來，成為獨立的專案。

## 專案結構

- `src/views/admin/` - 後台管理頁面
- `src/components/` - 後台專用組件和共用組件
- `src/composables/` - 共用組合式函數（認證、載入狀態等）
- `src/utils/` - 共用工具函數（API 請求等）

## 開發

```bash
npm install
npm run dev
```

開發服務器運行在 `http://localhost:5175`

## 構建

```bash
npm run build
```

構建結果會輸出到 `dist/` 目錄。

## 部署

專案使用 Docker 進行部署，通過 nginx 反向代理訪問。

訪問路徑：`/church-admin/`

## 路由

所有後台路由都移除了 `/admin` 前綴，因為這是後台專案：

- `/login` - 登入頁面
- `/` - 儀表板
- `/users` - 用戶管理
- `/roles` - 角色管理
- `/permissions` - 權限管理
- `/menus` - 菜單管理
- `/url-permissions` - URL 權限管理
- `/persons` - 人員管理
- `/positions` - 崗位管理
- `/service-schedule` - 服事表管理
- `/sunday-messages` - 主日信息管理
- `/activities` - 活動管理
- `/contact-submissions` - 聯絡表單管理
- `/church-info` - 教會資訊管理
- `/about-info` - 關於我們管理
- `/scheduled-jobs` - 排程任務管理
- `/maintenance` - 系統維護

## 注意事項

- 後台專案使用 `/church-admin/` 作為 base path
- 所有 API 請求仍然使用 `/api/church/admin/...` 路徑
- 認證機制與前台共用（JWT）

