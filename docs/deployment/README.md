# 部署文檔索引

## 📚 文檔分類

### 🚀 快速開始

- **[HTTPS_COMPLETE_GUIDE.md](./HTTPS_COMPLETE_GUIDE.md)** ⭐ **主要指南**
  - 完整的 HTTPS 設置指南
  - 包含所有方案和故障排除
  - **推薦先閱讀此文件**

- **[QUICK_START.md](./QUICK_START.md)**
  - 快速部署指南

- **[DEPLOYMENT_STEPS.md](./DEPLOYMENT_STEPS.md)**
  - 一鍵部署流程說明

### 🔒 HTTPS 設置

- **[HTTPS_COMPLETE_GUIDE.md](./HTTPS_COMPLETE_GUIDE.md)** ⭐ **主要指南**
  - 完整 HTTPS 設置指南（推薦）

- **[HTTPS_PERSISTENCE.md](./HTTPS_PERSISTENCE.md)**
  - HTTPS 設定持久化說明
  - 後續部署自動保留機制

- **[DOCKER_HTTPS_SETUP.md](./DOCKER_HTTPS_SETUP.md)**
  - Docker 環境 HTTPS 設置細節

- **[FREE_DOMAIN_SETUP.md](./FREE_DOMAIN_SETUP.md)**
  - 免費域名設置指南（DuckDNS、No-IP 等）

- **[HTTPS_WITHOUT_DOMAIN.md](./HTTPS_WITHOUT_DOMAIN.md)**
  - 無域名時的 HTTPS 方案（ngrok、自簽名證書）

- **[HTTPS_SETUP.md](./HTTPS_SETUP.md)**
  - 通用 HTTPS 設置指南（參考用）

### 📦 部署相關

- **[DEPLOY.md](./DEPLOY.md)**
  - 通用部署指南

- **[DEPLOY_WINDOWS.md](./DEPLOY_WINDOWS.md)**
  - Windows 部署指南

- **[DEPLOY_FIX.md](./DEPLOY_FIX.md)**
  - 部署問題修復

- **[DEPLOYMENT_STATUS.md](./DEPLOYMENT_STATUS.md)**
  - 部署狀態檢查

### 🛠️ 工具和配置

- **[INSTALL_DOCKER_COMPOSE.md](./INSTALL_DOCKER_COMPOSE.md)**
  - Docker Compose 安裝指南

- **[SSH_PASSWORDLESS_LOGIN.md](./SSH_PASSWORDLESS_LOGIN.md)**
  - SSH 免密碼登入設置

---

## 🎯 推薦閱讀順序

### 首次部署

1. [QUICK_START.md](./QUICK_START.md) - 了解基本流程
2. [HTTPS_COMPLETE_GUIDE.md](./HTTPS_COMPLETE_GUIDE.md) - 設置 HTTPS
3. [DEPLOYMENT_STEPS.md](./DEPLOYMENT_STEPS.md) - 一鍵部署流程

### 後續部署

1. [HTTPS_PERSISTENCE.md](./HTTPS_PERSISTENCE.md) - 了解自動保留機制
2. [DEPLOYMENT_STEPS.md](./DEPLOYMENT_STEPS.md) - 執行一鍵部署

### 遇到問題

1. [HTTPS_COMPLETE_GUIDE.md](./HTTPS_COMPLETE_GUIDE.md) - 故障排除章節
2. [DEPLOY_FIX.md](./DEPLOY_FIX.md) - 常見問題修復

---

## 📝 當前配置

- **域名**：`wc-project.duckdns.org`
- **HTTPS**：✅ 已啟用
- **SSL 證書**：Let's Encrypt（自動續期）
- **部署方式**：一鍵部署（`deploy-to-server.bat`）

---

## 🔗 相關文件

- [主 README.md](../../README.md) - 項目總覽
- [LINE_BOT_README.md](../../LINE_BOT_README.md) - LINE Bot 功能說明

