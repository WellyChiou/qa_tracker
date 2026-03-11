#!/bin/bash

# HTTPS 設置腳本（在服務器上執行）
# 注意：將 power-light-church.duckdns.org 替換為您的域名

set -e

echo "🚀 開始設置 HTTPS..."
echo ""

# 1. 檢查是否在項目目錄
if [ ! -f "docker-compose.yml" ]; then
    echo "❌ 錯誤: 請在項目根目錄執行此腳本"
    exit 1
fi

# 2. 創建必要的目錄
echo "📁 創建目錄結構..."
mkdir -p nginx/conf.d
mkdir -p certbot/conf
mkdir -p certbot/www
echo "✅ 目錄創建完成"
echo ""

# 3. 檢查 DNS 是否生效
echo "🔍 檢查 DNS 設置..."
DNS_IP=$(dig +short power-light-church.duckdns.org 2>/dev/null || echo "")
if [ -z "$DNS_IP" ]; then
    echo "⚠️  警告: 無法解析 DNS，請確認："
    echo "   1. DuckDNS 中已設置 IP 為 38.54.89.136"
    echo "   2. DNS 已生效（可能需要 5-30 分鐘）"
    echo ""
    read -p "是否繼續？(y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
elif [ "$DNS_IP" != "38.54.89.136" ]; then
    echo "⚠️  警告: DNS 解析不正確"
    echo "   當前 DNS 解析: $DNS_IP"
    echo "   預期 IP: 38.54.89.136"
    echo "   請確認在 DuckDNS 中已設置 IP 為 38.54.89.136"
    echo ""
    read -p "是否繼續？(y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo "✅ DNS 設置正確: $DNS_IP"
fi
echo ""

# 4. 確保使用 HTTP 配置啟動 nginx
echo "🔧 確保使用 HTTP 配置..."
if [ -f "nginx/nginx-https.conf" ] && [ ! -f "nginx/nginx.conf" ]; then
    # 如果只有 HTTPS 配置，創建 HTTP 配置
    echo "創建初始 HTTP 配置..."
    cat > nginx/nginx.conf << 'EOF'
events {
    worker_connections 1024;
}

http {
    upstream frontend-personal {
        server frontend-personal:80;
    }

    upstream frontend-church {
        server frontend-church:80;
    }

    upstream frontend-church-admin {
        server frontend-church-admin:80;
    }

    upstream backend-personal {
        server backend-personal:8080;
    }

    upstream backend-church {
        server backend-church:8080;
    }

    server {
        listen 80;
        server_name power-light-church.duckdns.org 38.54.89.136;

        location /.well-known/acme-challenge/ {
            root /var/www/certbot;
        }

        location / {
            proxy_pass http://frontend-personal;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        location / {
            proxy_pass http://frontend-church;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        location / {
            proxy_pass http://frontend-church-admin;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        location /api/church/ {
            proxy_pass http://backend-church;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        location /api/ {
            proxy_pass http://backend-personal;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
}
EOF
fi
echo "✅ 配置檢查完成"
echo ""

# 5. 啟動 nginx（如果還沒啟動）
echo    # 停止 Nginx 容器
    docker compose stop nginx 2>/dev/null || docker compose up -d nginx 2>/dev/null || true

# 等待 nginx 啟動
echo "等待 Nginx 啟動..."
sleep 5
echo "✅ Nginx 已啟動"
echo ""

# 6. 申請 SSL 證書
echo "📜 申請 Let's Encrypt SSL 證書..."
echo "   請輸入您的郵箱地址（用於證書到期提醒）:"
read -r EMAIL

if [ -z "$EMAIL" ]; then
    echo "❌ 錯誤: 郵箱地址不能為空"
    exit 1
fi

echo ""
echo "正在申請證書，這可能需要幾分鐘..."
docker compose run --rm certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email "$EMAIL" \
  --agree-tos \
  --no-eff-email \
  -d power-light-church.duckdns.org

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ SSL 證書申請成功！"
    echo ""
    
    # 7. 更新 nginx 配置為 HTTPS 版本
    echo "🔄 更新 Nginx 配置為 HTTPS..."
    if [ -f "nginx/nginx-https.conf" ]; then
        cp nginx/nginx-https.conf nginx/nginx.conf
        echo "✅ 配置已更新"
    else
        echo "⚠️  警告: 找不到 nginx-https.conf，請手動配置"
    fi
    echo ""
    
    # 8. 重啟服務
    # 重新加載 Nginx 配置
    docker compose exec nginx nginx -s reload 2>/dev/null || docker compose restart nginx 2>/dev/null || true
    docker compose down 2>/dev/null || docker compose down 2>/dev/null || true
    docker compose up -d 2>/dev/null || docker compose up -d 2>/dev/null || true
    
    echo ""
    echo "=========================================="
    echo "🎉 HTTPS 設置完成！"
    echo "=========================================="
    echo ""
    echo "📋 下一步："
    echo "1. 在 LINE Developers Console 設置 Webhook URL:"
    echo "   https://power-light-church.duckdns.org/api/line/webhook"
    echo ""
    echo "2. 訪問您的應用："
    echo "   前端: https://power-light-church.duckdns.org"
    echo "   API: https://power-light-church.duckdns.org/api"
    echo ""
    echo "3. 驗證 HTTPS："
    echo "   curl -I https://power-light-church.duckdns.org/api/line/webhook"
    echo ""
else
    echo ""
    echo "❌ SSL 證書申請失敗"
    echo ""
    echo "請檢查："
    echo "1. DNS 是否已生效（使用 dnschecker.org 檢查）"
    echo "2. 80 端口是否開放"
    echo "3. 防火牆設置"
    echo "4. Nginx 是否正常運行"
    echo ""
    echo "查看 Nginx 日誌："
    echo "  docker-compose logs nginx"
    exit 1
fi
