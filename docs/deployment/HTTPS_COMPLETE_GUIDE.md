# HTTPS 完整配置指南

## 📋 為什麼需要 HTTPS？

1. **LINE Bot 要求**：LINE Bot 的 Webhook URL 必須使用 HTTPS（生產環境）
2. **安全性**：保護傳輸中的數據不被竊聽
3. **瀏覽器信任**：現代瀏覽器會標記 HTTP 為不安全

## 🎯 方案選擇

### 方案 1：使用域名 + Let's Encrypt（推薦，免費）

如果您有域名，這是**最佳方案**，完全免費且自動續期。

### 方案 2：使用免費域名服務（DuckDNS、No-IP 等）

適合沒有域名的用戶，完全免費。

### 方案 3：使用 ngrok（臨時測試方案）

快速測試用，不適合生產環境。

### 方案 4：使用 IP 地址 + 自簽名證書

僅用於測試，LINE Bot 可能不接受自簽名證書。

---

## 🚀 方案 1：域名 + Let's Encrypt（推薦）

### 前置條件

1. 有一個域名（例如：`yourdomain.com`）
2. 域名已解析到您的服務器 IP（`38.54.89.136`）
3. 服務器可以訪問外網

### 步驟 1：更新 docker-compose.yml

添加 Nginx 反向代理服務：

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

### 步驟 2：創建 Nginx 配置目錄

```bash
mkdir -p nginx/conf.d
mkdir -p certbot/conf
mkdir -p certbot/www
```

### 步驟 3：創建 Nginx 配置文件

創建 `nginx/nginx.conf`：

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

    # HTTP 服務器（重定向到 HTTPS）
    server {
        listen 80;
        server_name yourdomain.com 38.54.89.136;

        # Let's Encrypt 驗證
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
        server_name yourdomain.com 38.54.89.136;

        # SSL 證書（Let's Encrypt）
        ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

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

### 步驟 4：申請 SSL 證書

```bash
# 啟動服務（不包含 certbot）
docker-compose up -d nginx

# 申請證書（替換為您的域名和郵箱）
docker-compose run --rm certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email chiou713@gmail.com \
  --agree-tos \
  --no-eff-email \
  -d power-light-church.duckdns.org

# 如果成功，重啟 nginx
docker-compose restart nginx
```

### 步驟 5：更新 docker-compose.yml 中的 Webhook URL

```yaml
LINE_BOT_WEBHOOK_URL: https://yourdomain.com/api/line/webhook
```

### 步驟 6：更新 docker-compose.yml 端口映射

由於 Nginx 現在處理 80 和 443 端口，需要更新：

```yaml
frontend:
  ports:
    # 移除對外端口，僅內部使用
    # - "80:80"  # 移除這行
```

```yaml
backend:
  ports:
    # 移除對外端口，僅內部使用
    # - "8080:8080"  # 移除這行
```

---

## 🆓 方案 2：使用免費域名服務

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

**注意**：免費版需要每 30 天確認一次，否則會被暫停。

### 設置 DNS 記錄

1. **確認 DNS 已生效**
   - 使用 https://dnschecker.org 檢查
   - 輸入您的域名，選擇 "A" 記錄類型
   - 確認顯示 `38.54.89.136`

2. **等待 DNS 傳播**
   - 通常需要 5-30 分鐘

### 申請 SSL 證書

按照方案 1 的步驟 4，將域名替換為您的免費域名即可。

---

## 🚀 方案 3：使用 ngrok（快速測試）

### 優點
- ✅ 完全免費
- ✅ 5 分鐘內完成設置
- ✅ LINE Bot 完全支援
- ✅ 提供有效的 HTTPS 證書

### 缺點
- ⚠️ 免費版 URL 每次重啟會變動
- ⚠️ 不適合長期生產環境

### 安裝步驟

#### Windows 系統

1. **下載 ngrok**
   - 前往 https://ngrok.com/download
   - 下載 Windows 版本
   - 解壓縮到任意目錄（例如：`C:\ngrok`）

2. **註冊帳號（免費）**
   - 前往 https://dashboard.ngrok.com/signup
   - 註冊免費帳號
   - 獲取您的 authtoken

3. **配置 ngrok**
   ```cmd
   # 在命令提示字元中執行
   ngrok config add-authtoken YOUR_AUTH_TOKEN
   ```

4. **啟動 ngrok**
   ```cmd
   # 創建 HTTPS 隧道，指向後端 8080 端口
   ngrok http 8080
   ```

5. **獲取 HTTPS URL**
   - ngrok 會顯示類似這樣的 URL：
     ```
     Forwarding  https://xxxx-xxxx-xxxx.ngrok-free.app -> http://localhost:8080
     ```
   - 複製這個 HTTPS URL（例如：`https://abc123.ngrok-free.app`）

6. **更新 docker-compose.yml**
   ```yaml
   LINE_BOT_WEBHOOK_URL: https://abc123.ngrok-free.app/api/line/webhook
   ```

7. **在 LINE Developers Console 設置 Webhook**
   - Webhook URL: `https://abc123.ngrok-free.app/api/line/webhook`
   - 點擊 "Verify" 驗證

#### Linux 系統

```bash
# 1. 下載 ngrok
wget https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-amd64.tgz
tar xvzf ngrok-v3-stable-linux-amd64.tgz
sudo mv ngrok /usr/local/bin/

# 2. 配置 authtoken（從 https://dashboard.ngrok.com 獲取）
ngrok config add-authtoken YOUR_AUTH_TOKEN

# 3. 啟動 ngrok（在背景執行）
nohup ngrok http 8080 > /dev/null 2>&1 &

# 4. 查看 URL
curl http://localhost:4040/api/tunnels
```

### 自動啟動 ngrok（Linux）

創建 systemd 服務文件 `/etc/systemd/system/ngrok.service`：

```ini
[Unit]
Description=ngrok tunnel
After=network.target

[Service]
Type=simple
User=root
ExecStart=/usr/local/bin/ngrok http 8080
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

啟動服務：
```bash
sudo systemctl enable ngrok
sudo systemctl start ngrok
```

---

## 🔧 方案 4：IP 地址 + 自簽名證書（不推薦）

⚠️ **警告**：LINE Bot **可能不接受**自簽名證書，因為瀏覽器和 LINE 會認為證書不可信。

### 如果仍想嘗試

```bash
# 創建證書目錄
mkdir -p nginx/ssl

# 生成自簽名證書
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/ssl/server.key \
  -out nginx/ssl/server.crt \
  -subj "/C=TW/ST=State/L=City/O=Organization/CN=38.54.89.136"
```

然後配置 Nginx 使用此證書。

---

## 📊 方案比較

| 方案 | 費用 | 設置難度 | LINE Bot 支援 | 穩定性 | 推薦度 |
|------|------|----------|--------------|--------|--------|
| 域名 + Let's Encrypt | 免費 | ⭐⭐⭐ 中等 | ✅ 完全支援 | ✅ 最穩定 | ⭐⭐⭐⭐⭐ |
| 免費域名 | 免費 | ⭐⭐⭐ 中等 | ✅ 完全支援 | ✅ 穩定 | ⭐⭐⭐⭐ |
| ngrok | 免費 | ⭐ 非常簡單 | ✅ 完全支援 | ⚠️ URL 會變 | ⭐⭐⭐⭐⭐ 測試用 |
| ngrok 付費 | $8/月 | ⭐ 非常簡單 | ✅ 完全支援 | ✅ 固定 URL | ⭐⭐⭐⭐ |
| 自簽名證書 | 免費 | ⭐⭐ 簡單 | ❌ 可能不支援 | ✅ 穩定 | ⭐ 不推薦 |

---

## ✅ 驗證 HTTPS 配置

### 測試 Webhook URL

```bash
# 測試 HTTPS 連接
curl -I https://yourdomain.com/api/line/webhook

# 應該返回 200 或 405（方法不允許，這是正常的）
```

### 在 LINE Developers Console 驗證

1. 進入 LINE Developers Console
2. 找到 Webhook settings
3. 點擊 "Verify" 按鈕
4. 應該顯示 "Success"

---

## 🔄 自動續期證書

Let's Encrypt 證書有效期為 90 天，certbot 服務會自動續期（已配置在 docker-compose.yml 中）。

可以手動測試續期：
```bash
docker-compose run --rm certbot renew --dry-run
```

---

## 📝 常見問題

### Q: 我沒有域名，只有 IP 地址怎麼辦？

A: 建議使用免費域名服務（DuckDNS）或購買一個便宜的域名（約 $10/年）。如果只是測試，可以使用 ngrok。

### Q: Let's Encrypt 證書會過期嗎？

A: 會，但使用 certbot 可以自動續期（已配置在 docker-compose.yml 中）。

### Q: 如何檢查證書是否有效？

A: 使用瀏覽器訪問 `https://yourdomain.com`，應該看到鎖圖標。

### Q: LINE Bot Webhook 驗證失敗？

A: 確保：
- Webhook URL 使用 HTTPS
- 服務器可以從外網訪問
- 防火牆允許 443 端口
- SSL 證書有效（不是自簽名）

### Q: DNS 未生效？

**解決**：
1. 使用 https://dnschecker.org 檢查 DNS 是否已傳播
2. 等待更長時間（最多 24 小時）
3. 確認 A 記錄設置正確

### Q: 證書申請失敗？

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

### Q: Nginx 無法啟動？

**解決**：
```bash
# 檢查 nginx 配置語法
docker-compose exec nginx nginx -t

# 查看日誌
docker-compose logs nginx
```

---

## 🎯 推薦配置流程

1. **選擇方案**
   - 有域名：使用方案 1（域名 + Let's Encrypt）
   - 沒有域名：使用方案 2（免費域名）或方案 3（ngrok 測試）

2. **配置 DNS**
   - 將域名 A 記錄指向 `38.54.89.136`
   - 等待 DNS 傳播（5-30 分鐘）

3. **配置 Nginx + Let's Encrypt**
   - 更新 docker-compose.yml
   - 創建 Nginx 配置文件
   - 申請 SSL 證書

4. **更新 Webhook URL**
   - 在 LINE Console 和 docker-compose.yml 中更新為 HTTPS

5. **測試驗證**
   - 確保一切正常

---

## 📝 完整配置檢查清單

- [ ] 選擇 HTTPS 方案
- [ ] 註冊域名或使用免費域名服務
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

## 💡 提示

1. **域名選擇**：選擇容易記住的域名
2. **郵箱**：使用真實郵箱，Let's Encrypt 會在證書到期前提醒
3. **備份**：定期備份 `certbot/conf` 目錄
4. **監控**：設置監控告警，確保證書自動續期成功

---

完成以上步驟後，您就擁有一個穩定的 HTTPS 配置，可以長期使用！

