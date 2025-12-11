# 方案 2：使用免費域名設置 HTTPS

## 🎯 方案概述

使用免費域名服務（如 Freenom）註冊免費域名，然後使用 Let's Encrypt 申請免費 SSL 證書。

---

## 📋 步驟 1：註冊免費域名

⚠️ **注意**：Freenom 已停止免費域名註冊服務。

### 選項 A：DuckDNS（推薦，最簡單）

1. **前往 DuckDNS**
   - 網址：https://www.duckdns.org
   - 點擊右上角 "Sign in with Google" 或 "Sign in with GitHub"

2. **登入帳號**
   - 使用 Google 或 GitHub 帳號登入（完全免費）

3. **創建子域名**
   - 在首頁輸入您想要的子域名（例如：`myexpensetracker`）
   - 選擇域名後綴：`.duckdns.org`
   - 點擊 "add domain"
   - 您的完整域名：`myexpensetracker.duckdns.org`

4. **設置 IP 地址**
   - 在域名下方輸入您的 IP：`38.54.89.136`
   - 點擊 "update ip"
   - 或勾選 "Use my IP" 自動更新

5. **記下您的域名**
   - 例如：`myexpensetracker.duckdns.org`

**優點**：
- ✅ 完全免費
- ✅ 永久有效
- ✅ 自動更新 IP（可選）
- ✅ 設置簡單

### 選項 B：No-IP（動態域名）

1. **前往 No-IP**
   - 網址：https://www.noip.com
   - 點擊 "Sign Up" 註冊

2. **註冊帳號**
   - 填寫基本資訊
   - 驗證郵箱

3. **創建主機名**
   - 登入後點擊 "Add Hostname"
   - 輸入主機名（例如：`myexpensetracker`）
   - 選擇域名後綴（例如：`.ddns.net`）
   - 設置 IP：`38.54.89.136`
   - 點擊 "Create Hostname"

4. **記下您的域名**
   - 例如：`myexpensetracker.ddns.net`

**注意**：免費版需要每 30 天確認一次，否則會被暫停。

### 選項 C：Afraid.org（免費動態 DNS）

1. **前往 Afraid.org**
   - 網址：https://freedns.afraid.org
   - 點擊 "Sign Up" 註冊

2. **註冊帳號**
   - 填寫基本資訊
   - 驗證郵箱

3. **添加子域名**
   - 登入後選擇一個免費域名（例如：`mooo.com`, `zapto.org`）
   - 創建子域名（例如：`myexpensetracker.mooo.com`）
   - 設置 A 記錄指向 `38.54.89.136`

### 選項 D：購買便宜域名（長期方案）

如果免費域名都不適合，可以購買便宜域名：

- **Namecheap**: 約 $10-15/年（.com）
- **Cloudflare**: 約 $8-12/年（最便宜）
- **GoDaddy**: 約 $12-20/年

### 選項 E：使用 GitHub Pages 或 Vercel 的免費子域名

這些服務提供免費子域名，但需要將應用部署到他們的平台。

---

## 🎯 推薦順序

1. **DuckDNS**（最推薦）
   - 設置最簡單
   - 永久免費
   - 支援自動更新 IP

2. **No-IP**
   - 需要定期確認
   - 但功能完整

3. **購買便宜域名**
   - 最穩定
   - 適合長期使用

---

## 📋 步驟 2：設置 DNS 記錄

### 在 Freenom 設置 A 記錄

1. **登入 Freenom**
   - 前往 https://www.freenom.com
   - 登入您的帳號

2. **進入域名管理**
   - 點擊 "Services" → "My Domains"
   - 找到您註冊的域名，點擊 "Manage Domain"

3. **設置 A 記錄**
   - 點擊 "Manage Freenom DNS"
   - 添加以下記錄：
     ```
     Type: A
     Name: @ (或留空)
     TTL: 3600
     Target: 38.54.89.136
     ```
   - 點擊 "Save Changes"

4. **等待 DNS 傳播**
   - 通常需要 5-30 分鐘
   - 可以使用 https://dnschecker.org 檢查是否已生效
   - 在搜尋框輸入您的域名，選擇 "A" 記錄類型
   - 確認顯示 `38.54.89.136`

---

## 📋 步驟 3：更新 docker-compose.yml

添加 Nginx 反向代理和 Certbot 服務：

```yaml
services:
  # ... 現有的 mysql, backend, frontend 服務 ...

  # Nginx 反向代理（處理 HTTPS）
  nginx:
    image: nginx:alpine
    container_name: nginx_proxy
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/conf.d:/etc/nginx/conf.d:ro
      - ./certbot/conf:/etc/letsencrypt:ro
      - ./certbot/www:/var/www/certbot:ro
    depends_on:
      - frontend
      - backend
    restart: unless-stopped
    command: "/bin/sh -c 'while :; do sleep 6h & wait $${!}; nginx -s reload; done & nginx -g \"daemon off;\"'"

  # Certbot（自動申請和續期 SSL 證書）
  certbot:
    image: certbot/certbot
    container_name: certbot
    volumes:
      - ./certbot/conf:/etc/letsencrypt:ro
      - ./certbot/www:/var/www/certbot:ro
    entrypoint: "/bin/sh -c 'trap exit TERM; while :; do certbot renew; sleep 12h & wait $${!}; done;'"
```

---

## 📋 步驟 4：創建必要的目錄和文件

```bash
# 創建目錄
mkdir -p nginx/conf.d
mkdir -p certbot/conf
mkdir -p certbot/www
```

---

## 📋 步驟 5：創建 Nginx 配置文件

### 創建 `nginx/nginx.conf`

```nginx
events {
    worker_connections 1024;
}

http {
    upstream frontend {
        server frontend-personal:80;
    }

    upstream backend {
        server backend:8080;
    }

    # HTTP 服務器（重定向到 HTTPS + Let's Encrypt 驗證）
    server {
        listen 80;
        server_name YOUR_DOMAIN.tk 38.54.89.136;

        # Let's Encrypt 驗證路徑
        location /.well-known/acme-challenge/ {
            root /var/www/certbot;
        }

        # 其他請求重定向到 HTTPS
        location / {
            return 301 https://$host$request_uri;
        }
    }

    # HTTPS 服務器
    server {
        listen 443 ssl http2;
        server_name YOUR_DOMAIN.tk 38.54.89.136;

        # SSL 證書（Let's Encrypt）
        ssl_certificate /etc/letsencrypt/live/YOUR_DOMAIN.tk/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/YOUR_DOMAIN.tk/privkey.pem;

        # SSL 配置（安全最佳實踐）
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers HIGH:!aNULL:!MD5;
        ssl_prefer_server_ciphers on;
        ssl_session_cache shared:SSL:10m;
        ssl_session_timeout 10m;

        # 前端
        location / {
            proxy_pass http://frontend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # 後端 API
        location /api {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
}
```

**重要**：將 `YOUR_DOMAIN.tk` 替換為您的實際域名！

---

## 📋 步驟 6：更新 docker-compose.yml 端口映射

由於 Nginx 現在處理 80 和 443 端口，需要移除 frontend 和 backend 的對外端口：

```yaml
frontend:
  # ... 其他配置 ...
  ports:
    # 移除這行，僅內部使用
    # - "80:80"
  # ... 其他配置 ...

backend:
  # ... 其他配置 ...
  ports:
    # 移除這行，僅內部使用
    # - "8080:8080"
  # ... 其他配置 ...
```

---

## 📋 步驟 7：申請 SSL 證書

### 1. 先啟動 Nginx（不包含 certbot）

```bash
docker-compose up -d nginx
```

### 2. 申請 Let's Encrypt 證書

```bash
# 替換 YOUR_DOMAIN.tk 和 your-email@example.com
docker-compose run --rm certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email \
  -d YOUR_DOMAIN.tk
```

**範例**：
```bash
docker-compose run --rm certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email admin@example.com \
  --agree-tos \
  --no-eff-email \
  -d myexpensetracker.tk
```

### 3. 如果成功，會看到：

```
Successfully received certificate.
Certificate is saved at: /etc/letsencrypt/live/YOUR_DOMAIN.tk/fullchain.pem
```

### 4. 更新 nginx.conf 中的域名

確保 `nginx/nginx.conf` 中的域名正確（應該已經設置好了）

### 5. 重啟 Nginx

```bash
docker-compose restart nginx
```

---

## 📋 步驟 8：更新 LINE Bot 配置

### 更新 docker-compose.yml

```yaml
LINE_BOT_WEBHOOK_URL: https://YOUR_DOMAIN.tk/api/line/webhook
```

**範例**：
```yaml
LINE_BOT_WEBHOOK_URL: https://myexpensetracker.tk/api/line/webhook
```

### 重啟服務

```bash
docker-compose down
docker-compose up -d
```

---

## 📋 步驟 9：在 LINE Developers Console 設置 Webhook

1. 前往 https://developers.line.biz/console/
2. 選擇您的 Channel
3. 進入 "Messaging API" 設定
4. 找到 "Webhook settings"
5. 設置 Webhook URL：`https://YOUR_DOMAIN.tk/api/line/webhook`
6. 點擊 "Verify" 驗證
7. 啟用 "Use webhook"

---

## ✅ 驗證配置

### 1. 測試 HTTPS 連接

```bash
curl -I https://YOUR_DOMAIN.tk/api/line/webhook
```

應該返回 200 或 405（這是正常的）

### 2. 在瀏覽器訪問

- 前端：`https://YOUR_DOMAIN.tk`
- 應該看到鎖圖標（表示 HTTPS 有效）

### 3. 檢查證書

在瀏覽器中點擊鎖圖標，查看證書資訊，應該顯示由 Let's Encrypt 簽發

---

## 🔧 故障排除

### 問題 1：DNS 未生效

**症狀**：無法訪問域名

**解決**：
1. 使用 https://dnschecker.org 檢查 DNS 是否已傳播
2. 等待更長時間（最多 24 小時）
3. 確認 A 記錄設置正確

### 問題 2：證書申請失敗

**症狀**：certbot 報錯

**常見原因**：
- DNS 未生效
- 80 端口被佔用
- 防火牆阻擋

**解決**：
```bash
# 檢查 80 端口
sudo netstat -tulpn | grep :80

# 檢查防火牆
sudo ufw status
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
```

### 問題 3：Nginx 無法啟動

**症狀**：docker-compose 報錯

**解決**：
```bash
# 檢查 nginx 配置語法
docker-compose exec nginx nginx -t

# 查看日誌
docker-compose logs nginx
```

---

## 🔄 自動續期證書

Let's Encrypt 證書有效期為 90 天，certbot 服務會自動續期（已配置在 docker-compose.yml 中）。

可以手動測試續期：
```bash
docker-compose run --rm certbot renew --dry-run
```

---

## 📝 完整配置檢查清單

- [ ] 註冊免費域名
- [ ] 設置 DNS A 記錄指向 `38.54.89.136`
- [ ] 確認 DNS 已生效（使用 dnschecker.org）
- [ ] 創建必要的目錄（nginx, certbot）
- [ ] 創建 nginx.conf（替換域名）
- [ ] 更新 docker-compose.yml（添加 nginx 和 certbot）
- [ ] 移除 frontend 和 backend 的對外端口
- [ ] 啟動 nginx
- [ ] 申請 SSL 證書
- [ ] 更新 LINE_BOT_WEBHOOK_URL
- [ ] 重啟所有服務
- [ ] 在 LINE Console 設置 Webhook
- [ ] 測試驗證

---

## 🎯 快速命令參考

```bash
# 1. 創建目錄
mkdir -p nginx/conf.d certbot/conf certbot/www

# 2. 啟動 nginx
docker-compose up -d nginx

# 3. 申請證書（替換域名和郵箱）
docker-compose run --rm certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email \
  -d YOUR_DOMAIN.tk

# 4. 重啟服務
docker-compose restart nginx
docker-compose down
docker-compose up -d

# 5. 檢查證書
docker-compose exec nginx nginx -t
```

---

## 💡 提示

1. **域名選擇**：選擇容易記住的域名
2. **郵箱**：使用真實郵箱，Let's Encrypt 會在證書到期前提醒
3. **備份**：定期備份 `certbot/conf` 目錄
4. **監控**：設置監控告警，確保證書自動續期成功

---

## 🆚 與 ngrok 比較

| 特性 | 免費域名 | ngrok |
|------|----------|-------|
| 費用 | 免費 | 免費（URL 會變） |
| URL 穩定性 | ✅ 固定 | ⚠️ 會變動 |
| 設置難度 | ⭐⭐⭐ 中等 | ⭐ 簡單 |
| 適合場景 | 生產環境 | 測試環境 |
| SSL 證書 | Let's Encrypt | ngrok 提供 |

---

完成以上步驟後，您就擁有一個穩定的 HTTPS 域名，可以長期使用！

