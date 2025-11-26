# Docker Vue + Java + MySQL 專案

這是一個使用 Docker Compose 架設的完整全端專案，包含：
- **前端**: Vue (使用 Nginx 提供靜態文件服務)
- **後端**: Java Spring Boot
- **資料庫**: MySQL 8.0

## 專案結構

```
docker-vue-java-mysql/
├── docker-compose.yml      # Docker Compose 配置
├── frontend/               # 前端目錄
│   ├── Dockerfile
│   └── app/
│       └── index.html      # 前端頁面
├── backend/                # 後端目錄
│   ├── Dockerfile
│   ├── pom.xml             # Maven 配置
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/example/helloworld/
│           │       ├── HelloWorldApplication.java
│           │       └── controller/
│           │           └── HelloController.java
│           └── resources/
│               └── application.properties
└── README.md
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
- 啟動 Java 後端
- 啟動 Vue 前端

### 3. 驗證部署

打開瀏覽器訪問：

- **前端**: http://localhost
  - 應該會看到 "Hello World!" 頁面，表示前端部署成功

- **後端 API**: http://localhost:8080/api/hello
  - 應該會看到 JSON 回應：`{"message":"Hello World!","status":"success","service":"Java Spring Boot Backend"}`

### 4. 停止服務

停止服務：

```bash
# 停止所有服務
docker compose down

# 停止並刪除資料卷（會清除資料庫資料）
docker compose down -v
```

## 服務端口

- **前端 (Nginx)**: 80
- **後端 (Spring Boot)**: 8080
- **MySQL**: 3306

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
- **[🔧 故障排除指南](./docs/TROUBLESHOOTING.md)** - 遇到問題時先看這個！
- **[👥 系統管理指南](./docs/admin/ADMIN_SYSTEM_GUIDE.md)** - 系統管理功能說明
- **[💻 開發指南](./docs/development/)** - 開發相關文檔

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
curl http://localhost:8080/api/hello
```

詳細的故障排除步驟請參考 [TROUBLESHOOTING.md](./TROUBLESHOOTING.md)

## 下一步

現在您已經成功看到 "Hello World"，可以開始：

1. 開發 Vue 前端應用
2. 擴展 Java 後端 API
3. 設計 MySQL 資料庫結構
4. 整合前後端功能

祝開發順利！🎉


