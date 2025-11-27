# Docker 環境 HTTPS 設置指南

## ✅ 已修正的問題

1. **證書申請前 nginx 無法啟動** - 已修正
   - 現在先使用 HTTP 配置啟動
   - 申請證書後自動切換到 HTTPS 配置

2. **Certbot volumes 權限** - 已修正
   - 從 `:ro`（只讀）改為可寫入
   - Certbot 需要寫入證書文件

3. **分階段配置** - 已實現
   - `nginx.conf` - 初始 HTTP 配置（用於申請證書）
   - `nginx-https.conf` - HTTPS 配置（申請證書後使用）

## 📋 正確的設置流程

### 步驟 1：確認 DuckDNS 設置

1. 前往 https://www.duckdns.org
2. 確認 `wc-project.duckdns.org` 的 IP 為 `38.54.89.136`
3. 如果還沒設置，請設置並點擊 "update ip"

### 步驟 2：創建必要的目錄

```bash
mkdir -p nginx/conf.d certbot/conf certbot/www
```

### 步驟 3：啟動服務（使用 HTTP 配置）

```bash
# 啟動所有服務（nginx 會使用 HTTP 配置）
docker-compose up -d
```

### 步驟 4：申請 SSL 證書

```bash
# 申請證書（替換為您的郵箱）
docker-compose run --rm certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email \
  -d wc-project.duckdns.org
```

### 步驟 5：切換到 HTTPS 配置

```bash
# 複製 HTTPS 配置文件
cp nginx/nginx-https.conf nginx/nginx.conf

# 重啟 nginx
docker-compose restart nginx
```

### 步驟 6：驗證 HTTPS

```bash
# 測試 HTTPS
curl -I https://wc-project.duckdns.org/api/line/webhook

# 在瀏覽器訪問
# https://wc-project.duckdns.org
```

## 🚀 使用自動化腳本（推薦）

### Linux/Mac

```bash
chmod +x setup-https.sh
./setup-https.sh
```

### Windows

```cmd
setup-https.bat
```

腳本會自動：
1. 創建目錄
2. 檢查 DNS
3. 啟動 nginx
4. 申請證書
5. 切換到 HTTPS 配置
6. 重啟服務

## 🔍 Docker 網絡配置說明

### 服務間通信

在 Docker Compose 中，服務之間通過服務名通信：

- `frontend:80` - 前端服務（端口 80）
- `backend:8080` - 後端服務（端口 8080）
- `nginx` - 反向代理服務

### 端口映射

- **80:80** - HTTP（用於 Let's Encrypt 驗證）
- **443:443** - HTTPS（生產環境使用）
- **3306:3306** - MySQL（僅內部使用，不應對外開放）

### 網絡隔離

所有服務都在同一個 Docker 網絡中，可以通過服務名互相訪問，無需暴露端口。

## ⚠️ 常見問題

### 問題 1：nginx 啟動失敗

**原因**：證書文件不存在

**解決**：
- 確保先使用 `nginx.conf`（HTTP 配置）啟動
- 申請證書後再切換到 `nginx-https.conf`

### 問題 2：證書申請失敗

**原因**：
- DNS 未生效
- 80 端口被佔用
- 防火牆阻擋

**解決**：
```bash
# 檢查 80 端口
sudo netstat -tulpn | grep :80

# 檢查 DNS
dig wc-project.duckdns.org

# 檢查防火牆
sudo ufw status
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
```

### 問題 3：服務無法訪問

**原因**：服務名解析失敗

**解決**：
- 確保所有服務在同一個 Docker Compose 網絡中
- 檢查 `depends_on` 配置
- 查看日誌：`docker-compose logs nginx`

## 📝 配置檢查清單

- [ ] DuckDNS 設置正確（IP: 38.54.89.136）
- [ ] DNS 已生效（使用 dnschecker.org 檢查）
- [ ] 創建了必要的目錄
- [ ] 使用 HTTP 配置啟動 nginx
- [ ] 成功申請 SSL 證書
- [ ] 切換到 HTTPS 配置
- [ ] 重啟所有服務
- [ ] HTTPS 訪問正常
- [ ] LINE Bot Webhook 設置正確

## 🔄 證書自動續期

Certbot 服務會自動續期證書（已配置在 docker-compose.yml 中）：

```yaml
certbot:
  entrypoint: "/bin/sh -c 'trap exit TERM; while :; do certbot renew; sleep 12h & wait $${!}; done;'"
```

證書到期前會自動續期，無需手動操作。

## 🎯 總結

現在的配置是**正確的**，適合 Docker 環境：

1. ✅ 使用 Docker Compose 網絡進行服務間通信
2. ✅ 分階段配置（先 HTTP，後 HTTPS）
3. ✅ Certbot 有正確的寫入權限
4. ✅ 自動化腳本簡化設置流程
5. ✅ 證書自動續期

按照上述步驟操作即可完成 HTTPS 設置！

