# HTTPS 配置指南

## 📋 為什麼需要 HTTPS？

1. **LINE Bot 要求**：LINE Bot 的 Webhook URL 必須使用 HTTPS（生產環境）
2. **安全性**：保護傳輸中的數據不被竊聽
3. **瀏覽器信任**：現代瀏覽器會標記 HTTP 為不安全

## 🎯 方案選擇

### 方案 1：使用域名 + Let's Encrypt（推薦，免費）

如果您有域名，這是**最佳方案**，完全免費且自動續期。

### 方案 2：使用 IP 地址 + 自簽名證書

僅用於測試，LINE Bot 可能不接受自簽名證書。

### 方案 3：使用 ngrok（臨時測試方案）

快速測試用，不適合生產環境。

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
        server frontend:80;
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
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email \
  -d yourdomain.com

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

## 🔧 方案 2：IP 地址 + 自簽名證書（僅測試）

⚠️ **注意**：LINE Bot 可能不接受自簽名證書，此方案僅用於測試。

### 生成自簽名證書

```bash
mkdir -p nginx/ssl

# 生成私鑰
openssl genrsa -out nginx/ssl/server.key 2048

# 生成證書請求
openssl req -new -key nginx/ssl/server.key -out nginx/ssl/server.csr \
  -subj "/C=TW/ST=State/L=City/O=Organization/CN=38.54.89.136"

# 生成自簽名證書
openssl x509 -req -days 365 -in nginx/ssl/server.csr -signkey nginx/ssl/server.key \
  -out nginx/ssl/server.crt
```

### 簡化的 Nginx 配置

創建 `nginx/conf.d/default.conf`：

```nginx
upstream frontend {
    server frontend:80;
}

upstream backend {
    server backend:8080;
}

server {
    listen 443 ssl http2;
    server_name 38.54.89.136;

    ssl_certificate /etc/nginx/ssl/server.crt;
    ssl_certificate_key /etc/nginx/ssl/server.key;

    location / {
        proxy_pass http://frontend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /api {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 🚀 方案 3：使用 ngrok（快速測試）

### 安裝 ngrok

```bash
# 下載 ngrok
wget https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-amd64.tgz
tar xvzf ngrok-v3-stable-linux-amd64.tgz
sudo mv ngrok /usr/local/bin/
```

### 啟動 ngrok

```bash
# 創建 HTTPS 隧道（指向後端 8080 端口）
ngrok http 8080
```

### 使用 ngrok 提供的 HTTPS URL

ngrok 會提供一個類似 `https://xxxxx.ngrok.io` 的 URL，將此 URL 設置為 Webhook URL：

```yaml
LINE_BOT_WEBHOOK_URL: https://xxxxx.ngrok.io/api/line/webhook
```

⚠️ **注意**：
- ngrok 免費版 URL 會變動（每次重啟）
- 不適合生產環境
- 僅用於開發測試

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

## 🔄 更新現有配置

如果已經在使用 HTTP，需要：

1. **更新 docker-compose.yml**：添加 Nginx 服務
2. **申請 SSL 證書**：使用 Let's Encrypt
3. **更新 Webhook URL**：改為 HTTPS
4. **重啟服務**：`docker-compose down && docker-compose up -d`
5. **在 LINE Console 更新 Webhook URL**

---

## 📝 常見問題

### Q: 我沒有域名，只有 IP 地址怎麼辦？

A: 建議購買一個便宜的域名（約 $10/年），然後使用方案 1。如果只是測試，可以使用方案 3（ngrok）。

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

---

## 🎯 推薦配置流程

1. **購買域名**（如果還沒有）
2. **配置 DNS**：將域名 A 記錄指向 `38.54.89.136`
3. **使用方案 1**：配置 Nginx + Let's Encrypt
4. **更新 Webhook URL**：在 LINE Console 和 docker-compose.yml 中
5. **測試驗證**：確保一切正常

