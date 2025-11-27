# 部署相關更新日誌

## 2025-11-27 - HTTPS 功能整合

### ✨ 新增功能

1. **HTTPS 支持**
   - 使用 Nginx 反向代理處理 HTTPS
   - 使用 Let's Encrypt 免費 SSL 證書
   - 自動證書續期

2. **自動化部署**
   - 部署腳本自動檢測 SSL 證書
   - 自動切換 HTTP/HTTPS 配置
   - 證書自動保留

3. **LINE Bot 整合**
   - 支持 HTTPS Webhook
   - 用戶綁定功能
   - 訊息記錄費用功能

### 📝 新增文件

#### 配置文件
- `nginx/nginx.conf` - Nginx 主配置（自動切換）
- `nginx/nginx-https.conf` - HTTPS 配置
- `nginx/nginx-http-only.conf` - HTTP 配置（申請證書用）

#### 設置腳本
- `setup-https-on-server.sh` - 服務器端 HTTPS 設置腳本（推薦）
- `setup-https.sh` - Linux/Mac 本地設置腳本
- `setup-https.bat` - Windows 本地設置腳本

#### 文檔
- `docs/deployment/HTTPS_COMPLETE_GUIDE.md` ⭐ - 完整 HTTPS 指南
- `docs/deployment/HTTPS_PERSISTENCE.md` - HTTPS 持久化說明
- `docs/deployment/DOCKER_HTTPS_SETUP.md` - Docker 環境設置
- `docs/deployment/FREE_DOMAIN_SETUP.md` - 免費域名設置
- `docs/deployment/HTTPS_WITHOUT_DOMAIN.md` - 無域名方案
- `docs/deployment/DEPLOYMENT_STEPS.md` - 部署流程說明
- `docs/deployment/README.md` - 部署文檔索引

### 🔧 修改文件

1. **docker-compose.yml**
   - 添加 Nginx 反向代理服務
   - 添加 Certbot 自動續期服務
   - 更新 LINE Bot Webhook URL 為 HTTPS

2. **deploy.sh**
   - 添加 HTTPS 目錄檢查
   - 添加自動證書檢測和配置切換
   - 更新部署完成提示

3. **前端代碼**
   - `frontend/src/composables/useApi.js` - 修正 API 端口配置
   - `frontend/src/views/Login.vue` - 修正 API 端口配置
   - `frontend/src/views/Dashboard.vue` - 修正 API 端口配置

4. **後端代碼**
   - `backend/src/main/java/com/example/helloworld/config/SecurityConfig.java` - 添加域名到 CORS 配置

### 🎯 當前配置

- **域名**：`wc-project.duckdns.org`
- **SSL 證書**：Let's Encrypt（有效期至 2026-02-25）
- **證書郵箱**：`chiou713@gmail.com`
- **自動續期**：已啟用

### 📋 使用說明

#### 首次部署
1. 執行 `deploy-to-server.bat`
2. 部署完成後，執行 `setup-https-on-server.sh`

#### 後續部署
1. 執行 `deploy-to-server.bat`
2. 完成（證書和配置自動保留）

### ⚠️ 注意事項

- 不要刪除 `certbot/conf` 目錄（會刪除證書）
- 證書會自動續期，無需手動操作
- 部署腳本會自動處理配置切換

---

## 文件結構說明

### 主要文檔（推薦閱讀）

- `HTTPS_COMPLETE_GUIDE.md` ⭐ - 完整 HTTPS 指南
- `HTTPS_PERSISTENCE.md` - 持久化說明
- `DEPLOYMENT_STEPS.md` - 部署流程

### 參考文檔

- `DOCKER_HTTPS_SETUP.md` - Docker 環境細節
- `FREE_DOMAIN_SETUP.md` - 免費域名設置
- `HTTPS_WITHOUT_DOMAIN.md` - 無域名方案
- `HTTPS_SETUP.md` - 通用設置指南

