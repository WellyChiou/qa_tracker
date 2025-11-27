# 一鍵部署流程說明

## 📋 完整部署步驟

### 步驟 1：首次部署（包含 HTTPS 設置）

#### 1.1 本地執行部署

```bash
# Windows
deploy-to-server.bat

# 或 Linux/Mac
./deploy-to-server.sh
```

這會：
- 打包項目
- 上傳到服務器
- 在服務器上解壓並執行部署

#### 1.2 在服務器上設置 HTTPS（首次部署後）

部署完成後，SSH 到服務器執行：

```bash
# SSH 到服務器
ssh root@38.54.89.136

# 進入項目目錄
cd /root/project/work/docker-vue-java-mysql

# 執行 HTTPS 設置腳本
chmod +x setup-https-on-server.sh
./setup-https-on-server.sh
```

或者手動執行：

```bash
# 1. 創建目錄
mkdir -p nginx/conf.d certbot/conf certbot/www

# 2. 啟動服務（使用 HTTP 配置）
docker-compose up -d

# 3. 申請 SSL 證書
docker-compose run --rm certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email \
  -d wc-project.duckdns.org

# 4. 切換到 HTTPS 配置
cp nginx/nginx-https.conf nginx/nginx.conf

# 5. 重啟服務
docker-compose restart nginx
docker-compose down
docker-compose up -d
```

### 步驟 2：後續更新部署

如果已經設置過 HTTPS，後續只需要：

```bash
# 本地執行
deploy-to-server.bat
```

部署腳本會自動：
- 保留現有的 SSL 證書（在 `certbot/conf` 目錄）
- 保留 Nginx 配置
- 更新應用代碼並重啟服務

## 🔄 部署流程圖

```
本地開發
   ↓
執行 deploy-to-server.bat
   ↓
打包項目 (tar.gz)
   ↓
上傳到服務器 (scp)
   ↓
服務器執行 remote_deploy.sh
   ↓
解壓項目
   ↓
執行 deploy.sh
   ↓
Docker Compose 部署
   ↓
[首次部署] → 執行 setup-https-on-server.sh → HTTPS 設置完成
[後續部署] → 直接完成
```

## 📝 重要文件說明

### 本地文件
- `deploy-to-server.bat` - Windows 一鍵部署腳本
- `deploy-to-server.sh` - Linux/Mac 一鍵部署腳本
- `remote_deploy.sh` - 服務器端解壓腳本（自動上傳）

### 服務器端文件
- `deploy.sh` - 實際部署腳本（執行 docker compose）
- `setup-https-on-server.sh` - HTTPS 設置腳本（首次部署後執行）

### 配置文件
- `docker-compose.yml` - Docker Compose 配置
- `nginx/nginx.conf` - Nginx HTTP 配置（申請證書用）
- `nginx/nginx-https.conf` - Nginx HTTPS 配置（生產環境用）

## ⚠️ 注意事項

### 1. HTTPS 設置時機

- **首次部署**：部署完成後，需要單獨執行 HTTPS 設置
- **後續部署**：不需要重新設置 HTTPS，證書會自動保留

### 2. 證書自動續期

Certbot 服務會自動續期證書（已配置在 docker-compose.yml 中），無需手動操作。

### 3. DNS 設置

確保在執行 HTTPS 設置前：
- DuckDNS 中已設置 IP 為 `38.54.89.136`
- DNS 已生效（可能需要 5-30 分鐘）

### 4. 防火牆

確保服務器開放以下端口：
- 80 (HTTP - Let's Encrypt 驗證)
- 443 (HTTPS - 生產環境)

## 🚀 快速命令參考

### 本地部署
```bash
deploy-to-server.bat
```

### 服務器上操作
```bash
# 查看服務狀態
docker-compose ps

# 查看日誌
docker-compose logs -f

# 重啟服務
docker-compose restart

# 停止服務
docker-compose down

# 設置 HTTPS（首次）
./setup-https-on-server.sh
```

## 🔍 故障排除

### 問題 1：部署失敗

**檢查**：
- 服務器連接是否正常
- SSH 密碼是否正確
- 服務器磁盤空間是否足夠

### 問題 2：HTTPS 設置失敗

**檢查**：
- DNS 是否已生效
- 80 端口是否開放
- 防火牆設置

### 問題 3：服務無法訪問

**檢查**：
- 服務是否正常運行：`docker-compose ps`
- 查看日誌：`docker-compose logs`
- 檢查端口：`netstat -tulpn | grep -E '80|443'`

