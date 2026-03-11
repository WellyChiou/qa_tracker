# Docker Vue + Java + MySQL 專案

這是一個使用 Docker Compose 架設的完整全端專案，包含：
- **3 個前端站台**：`frontend-personal`、`frontend-church`、`frontend-church-admin`
- **2 個後端專案**：`backend-personal`、`backend-church`（Java Spring Boot）
- **資料庫**：MySQL 8.0
- **反向代理**：Nginx

## 專案結構

```
docker-vue-java-mysql/
├── docker-compose.yml      # Docker Compose 配置
├── frontend-personal/      # 個人系統前端（/personal/）
├── frontend-church/        # 教會前台網站（/church/）
├── frontend-church-admin/  # 教會後台管理系統（/church-admin/）
├── backend-personal/       # 個人系統後端 API
├── backend-church/         # 教會系統後端 API
├── nginx/                  # Nginx 反向代理設定
├── mysql/                  # MySQL schema 與腳本
└── scripts/                # 部署、診斷、維運腳本
```

## 快速開始

### 1. 確認 Docker 已安裝

```bash
docker --version
docker compose version  # 注意：新版本沒有連字號
```

### 2. 啟動所有服務

在專案根目錄執行：

```bash
# 新版本 Docker Compose（推薦）
docker compose up -d --build

# 舊版本 Docker Compose
docker-compose up -d --build
```

這個命令會：
- 構建所有 Docker 映像
- 啟動 MySQL 資料庫
- 啟動 personal / church 兩個 Java 後端
- 啟動 3 個 Vue 前端站台
- 啟動 Nginx 反向代理

### 3. 驗證部署

打開瀏覽器訪問：

- **個人前端**: `http://localhost/personal/`
- **教會前台**: `http://localhost/church/`
- **教會後台**: `http://localhost/church-admin/`
- **個人後端 API**: `http://localhost/api/**`
- **教會後端 API**: `http://localhost/api/church/**`

### 4. 停止服務

停止服務：

```bash
# 停止所有服務
docker compose down

# 停止並刪除資料卷（會清除資料庫資料）
docker compose down -v
```

## 服務與路由

- **Nginx**: `80`, `443`
- **個人前端**: `/personal/`
- **教會前台**: `/church/`
- **教會後台**: `/church-admin/`
- **個人後端 API**: `/api/**`
- **教會後端 API**: `/api/church/**`
- **MySQL**: `3306`

## 資料庫資訊

- **資料庫名稱**: testdb
- **使用者名稱**: appuser
- **密碼**: apppassword
- **Root 密碼**: rootpassword

## 📚 文檔索引

**👉 所有文檔已整合到 [docs/](./docs/) 目錄，請查看 [docs/README.md](./docs/README.md) 獲取完整文檔索引**

### 快速鏈接

- **[📚 完整文檔目錄](./docs/README.md)** - 所有文檔的索引
- **[🚀 快速部署指南](./docs/deployment/QUICK_START.md)** - 快速開始部署
- **[🔒 HTTPS 完整指南](./docs/deployment/HTTPS_COMPLETE_GUIDE.md)** ⭐ - HTTPS 設置（推薦）
- **[🔧 故障排除指南](./docs/TROUBLESHOOTING.md)** - 遇到問題時先看這個！
- **[👥 系統管理指南](./docs/admin/ADMIN_SYSTEM_GUIDE.md)** - 系統管理功能說明
- **[💻 開發指南](./docs/development/)** - 開發相關文檔
- **[📱 LINE Bot 功能](./LINE_BOT_README.md)** - LINE Bot 使用說明

### 🌐 當前部署狀態

- **域名**：`power-light-church.duckdns.org`
- **HTTPS**：✅ 已啟用
- **SSL 證書**：Let's Encrypt（自動續期）
- **部署方式**：一鍵部署（`deploy-to-server.bat`）

## 常見問題

### 遇到問題？

**👉 請先查看 [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) 故障排除指南！**

常見問題包括：
- Docker Compose 安裝問題
- 端口檢查工具問題
- 防火牆配置問題
- Docker 構建錯誤
- 後端連接問題（CORS）
- 前端未啟動問題

### 快速問題排查

```bash
# 1. 檢查容器狀態
docker compose ps

# 2. 查看日誌
docker compose logs

# 3. 測試服務
curl http://localhost/api/hello
```

詳細的故障排除步驟請參考 [TROUBLESHOOTING.md](./TROUBLESHOOTING.md)

## 下一步

現在可以依需求開發對應子系統：

1. `frontend-personal` 個人系統功能
2. `frontend-church` 教會公開網站內容
3. `frontend-church-admin` 教會後台管理功能
4. `backend-personal` 個人系統 API、認證與排程
5. `backend-church` 教會系統 API、認證與排程
