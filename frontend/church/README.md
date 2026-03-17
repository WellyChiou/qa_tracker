# 教會網站

這是教會網站的前端應用，使用 Vue 3 構建。

## 功能

- **首頁**：展示最新消息和聚會時間
- **關於我們**：介紹教會的使命、願景和價值
- **活動資訊**：展示教會的各種活動
- **聯絡我們**：提供聯絡資訊和留言表單

## 開發

### 本地開發

```bash
# 安裝依賴
npm install

# 啟動開發服務器
npm run dev
```

開發服務器將在 `http://localhost:5174` 運行。

### 構建

```bash
# 構建生產版本
npm run build
```

## 部署

教會網站已經整合到 Docker Compose 配置中，可以通過以下方式訪問：

- 本地開發：`http://localhost:5174`
- 生產環境：`https://your-domain.com/church/`

## 頁面結構

- `/` - 首頁
- `/about` - 關於我們
- `/activities` - 活動資訊
- `/contact` - 聯絡我們

## 自訂

您可以根據需要修改以下文件：

- `src/views/Home.vue` - 首頁內容
- `src/views/About.vue` - 關於我們頁面
- `src/views/Activities.vue` - 活動列表
- `src/views/Contact.vue` - 聯絡表單
- `src/App.vue` - 導航欄和頁腳
- `src/style.css` - 全局樣式

## 注意事項

- 教會網站使用 `/church/` 作為基礎路徑
- 如果需要連接後端 API，可以在 `src/composables/` 目錄下創建 API 相關的 composable
- 聯絡表單目前只是前端展示，如需實際功能需要連接後端 API

