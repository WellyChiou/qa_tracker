#!/bin/bash
# 在專案根目錄執行，用來「申請或重新申請」 Let's Encrypt 憑證
# 需搭配：
#   1. docker-compose.yml 裡有 nginx + certbot 服務（跟你現在的一樣）
#   2. nginx.conf 有：
#        location /.well-known/acme-challenge/ { root /var/www/certbot; }

set -e

echo "🚀 開始設置 / 重新申請 HTTPS..."
echo ""

# 1. 確認在專案根目錄
if [ ! -f "docker-compose.yml" ]; then
  echo "❌ 錯誤：請在專案根目錄（有 docker-compose.yml 的地方）執行此腳本"
  exit 1
fi

# 2. 輸入網域與 Email（可重複使用、可換網域）
read -rp "請輸入要申請憑證的網域（例如 power-light-church.duckdns.org）： " DOMAIN
if [ -z "$DOMAIN" ]; then
  echo "❌ 錯誤：網域不能為空"
  exit 1
fi

read -rp "請輸入您的 Email（用於憑證到期通知）： " EMAIL
if [ -z "$EMAIL" ]; then
  echo "❌ 錯誤：Email 不能為空"
  exit 1
fi

echo ""

# 3. 建立必要目錄
echo "📁 建立/確認 certbot 相關目錄..."
mkdir -p certbot/conf
mkdir -p certbot/www
mkdir -p nginx/conf.d
echo "✅ 目錄準備完成"
echo ""

# 4. 取得本機對外 IP（用於 DNS 檢查）
echo "🌐 偵測本機對外 IP..."
SERVER_IP=$(dig +short myip.opendns.com @resolver1.opendns.com 2>/dev/null || echo "")
if [ -z "$SERVER_IP" ]; then
  echo "⚠️  無法自動偵測本機 IP，將略過 IP 比對，只檢查是否能解析網域"
fi

# 5. 檢查 DNS 是否指向本機
echo "🔍 檢查 DNS 設置（使用 8.8.8.8 查詢）..."
DNS_IP=$(dig +short "$DOMAIN" @8.8.8.8 2>/dev/null || echo "")

if [ -z "$DNS_IP" ]; then
  echo "⚠️  警告：無法解析網域 $DOMAIN"
  echo "   請確認 DuckDNS / DNS 提供商中已設定正確 IP。"
  echo ""
  read -rp "仍要繼續申請憑證嗎？(y/N) " -n 1 CONT
  echo
  if [[ ! "$CONT" =~ ^[Yy]$ ]]; then
    exit 1
  fi
else
  echo "   目前 DNS 解析結果：$DNS_IP"
  if [ -n "$SERVER_IP" ] && [ "$DNS_IP" != "$SERVER_IP" ]; then
    echo "⚠️  警告：DNS 解析 IP 與本機 IP 不一致"
    echo "   DNS:   $DNS_IP"
    echo "   本機:  $SERVER_IP"
    echo "   請確認網域已正確指向此伺服器。"
    echo ""
    read -rp "仍要繼續申請憑證嗎？(y/N) " -n 1 CONT
    echo
    if [[ ! "$CONT" =~ ^[Yy]$ ]]; then
      exit 1
    fi
  else
    echo "✅ DNS 設定看起來正確"
  fi
fi
echo ""

# 6. 檢查 nginx.conf 是否有 ACME 路徑
if ! grep -q "/.well-known/acme-challenge/" nginx/nginx.conf 2>/dev/null; then
  echo "⚠️  警告：nginx/nginx.conf 中找不到 ACME 驗證路徑設定："
  echo "    location /.well-known/acme-challenge/ {"
  echo "        root /var/www/certbot;"
  echo "    }"
  echo "   如果沒有這段，憑證申請會失敗。"
  echo ""
  read -rp "確認已手動加上該段設定後，再繼續。要繼續嗎？(y/N) " -n 1 CONT
  echo
  if [[ ! "$CONT" =~ ^[Yy]$ ]]; then
    exit 1
  fi
fi

# 7. 啟動 / 重啟 nginx（確保 80/443 都在跑）
echo "🚀 啟動 / 重啟 nginx 容器..."
docker compose up -d nginx
echo "✅ nginx 已啟動"
echo ""

# 8. 使用 webroot 申請 / 重新申請正式憑證
echo "📜 使用 webroot 模式申請 Let's Encrypt 憑證..."
echo "   網域：$DOMAIN"
echo "   Email：$EMAIL"
echo ""
echo "   👉 如該網域已存在憑證，將會強制重新簽發（--force-renewal）"
echo ""

docker compose run --rm certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email "$EMAIL" \
  --agree-tos \
  --no-eff-email \
  --force-renewal \
  -d "$DOMAIN"

echo ""
echo "✅ 憑證申請 / 重新申請完成"
echo ""

# 9. 重新載入 nginx，套用新憑證
echo "🔄 重新載入 nginx 設定，套用新憑證..."
docker compose exec nginx nginx -s reload 2>/dev/null || docker compose restart nginx
echo "✅ nginx 已重新載入"
echo ""

# 10. 完成提示
echo "=========================================="
echo "🎉 HTTPS 設置完成！"
echo "=========================================="
echo ""
echo "📂 憑證位置（在容器內）："
echo "   /etc/letsencrypt/live/$DOMAIN/fullchain.pem"
echo "   /etc/letsencrypt/live/$DOMAIN/privkey.pem"
echo ""
echo "🔁 自動續期："
echo "   docker-compose.yml 中的 certbot 服務已設定每 12 小時執行 certbot renew"
echo "   並透過 webroot 模式續期。"
echo ""
echo "🧪 測試："
echo "   curl -I https://$DOMAIN"
echo "   curl -I https://$DOMAIN/personal/"
echo ""
