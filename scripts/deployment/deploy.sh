#!/bin/bash

# 自動部署腳本
# 用於在虛擬主機上自動部署整個項目

set -euo pipefail  # 遇到錯誤立即退出

echo "=========================================="
echo "開始部署 Docker Vue + Java + MySQL 項目"
echo "=========================================="

# 檢查 Docker 是否安裝
if ! command -v docker &> /dev/null; then
    echo "❌ 錯誤: Docker 未安裝"
    echo "請先安裝 Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

# 檢查 Docker Compose 是否安裝
if ! docker compose version >/dev/null 2>&1 && ! command -v docker-compose >/dev/null 2>&1; then
    echo "❌ 錯誤: Docker Compose 未安裝"
    echo "請先安裝 Docker Compose"
    exit 1
fi

echo "✅ Docker 環境檢查通過"

# 統一 docker compose 指令（支援 plugin 與舊版 docker-compose）
if docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD=(docker compose)
else
    COMPOSE_CMD=(docker-compose)
fi

# 檢查端口是否被佔用
echo ""
echo "檢查端口..."
if lsof -Pi :80 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo "⚠️  警告: 端口 80 已被佔用"
    if [[ -t 0 ]]; then
        read -r -p "是否繼續？(y/n) " -n 1 REPLY
        echo
        if [[ ! "${REPLY}" =~ ^[Yy]$ ]]; then
            exit 1
        fi
    else
        echo "ℹ️  非互動模式，將繼續部署並在後續執行 docker compose down 釋放端口"
    fi
fi

# 檢查必要的文件
echo ""
echo "檢查項目文件..."
if [ ! -f "docker-compose.yml" ]; then
    echo "❌ 錯誤: 找不到 docker-compose.yml"
    exit 1
fi

REQUIRED_PATHS=(
    "backend/personal"
    "backend/church"
    "backend/invest"
    "frontend/personal"
    "frontend/church"
    "frontend/church-admin"
    "frontend/invest-admin"
    "database/personal/schema/schema.sql"
    "database/church/schema/church-schema.sql"
    "database/invest/migrations/schema/01_create_invest_step1.sql"
)

for path in "${REQUIRED_PATHS[@]}"; do
    if [ ! -e "$path" ]; then
        echo "❌ 錯誤: 找不到必要路徑 $path"
        exit 1
    fi
done

echo "✅ 項目文件檢查通過"

# 檢查並創建 HTTPS 相關目錄
echo ""
echo "檢查 HTTPS 配置目錄..."
mkdir -p nginx/conf.d
mkdir -p certbot/conf
mkdir -p certbot/www
echo "✅ HTTPS 目錄檢查完成"

# 嘗試從備份目錄恢復證書（如果當前目錄沒有證書）
BACKUP_DIR="../docker-vue-java-mysql_backup"
if [ ! -f "certbot/conf/live/power-light-church.duckdns.org/fullchain.pem" ] && [ -d "$BACKUP_DIR/certbot/conf" ]; then
    echo ""
    echo "從備份目錄恢復 SSL 證書..."
    if [ -d "$BACKUP_DIR/certbot/conf/live" ]; then
        cp -r "$BACKUP_DIR/certbot/conf/live" certbot/conf/ 2>/dev/null || true
    fi
    if [ -d "$BACKUP_DIR/certbot/conf/archive" ]; then
        cp -r "$BACKUP_DIR/certbot/conf/archive" certbot/conf/ 2>/dev/null || true
    fi
    if [ -f "certbot/conf/live/power-light-church.duckdns.org/fullchain.pem" ]; then
        echo "✅ 證書已從備份恢復"
    fi
fi

# 根據憑證存在與否，切換 nginx 配置（避免無憑證時 nginx 啟動失敗）
echo ""
echo "選擇 Nginx 配置..."
if [ -f "certbot/conf/live/power-light-church.duckdns.org/fullchain.pem" ]; then
    if grep -q "ssl_certificate" nginx/nginx.conf 2>/dev/null; then
        echo "✅ 偵測到現有 nginx.conf 已含 HTTPS 設定，保留現有配置"
    elif [ -f "nginx/nginx-https.conf" ]; then
        cp nginx/nginx-https.conf nginx/nginx.conf
        echo "✅ 使用 HTTPS 配置 (nginx-https.conf)"
    else
        echo "⚠️  找不到 nginx/nginx-https.conf，沿用現有 nginx.conf"
    fi
else
    if [ -f "nginx/nginx-http-only.conf" ]; then
        cp nginx/nginx-http-only.conf nginx/nginx.conf
        echo "✅ 未檢測到憑證，已切換為 HTTP-only 配置"
    elif grep -q "ssl_certificate" nginx/nginx.conf 2>/dev/null; then
        echo "❌ 錯誤: 無憑證且 nginx.conf 仍包含 SSL 設定，nginx 將啟動失敗"
        echo "   請提供 nginx/nginx-http-only.conf 或先配置 SSL 憑證"
        exit 1
    else
        echo "⚠️  未檢測到憑證，且無 nginx-http-only.conf，沿用現有 nginx.conf"
    fi
fi

# 停止並刪除現有容器（如果存在）
# 注意：使用 docker compose down（不含 -v）會保留資料庫資料
# 只有使用 docker compose down -v 才會清空資料
echo ""
echo "停止並清理現有容器（資料庫資料會保留）..."
"${COMPOSE_CMD[@]}" down 2>/dev/null || true

# 強制刪除可能殘留的容器（避免名稱衝突）
# ⚠️ 注意：只刪除容器，不會刪除 volume（資料庫資料會保留）
echo "清理殘留容器（資料庫資料會保留）..."
docker rm -f \
    mysql_db \
    java_backend_personal java_backend_church java_backend_invest \
    vue_personal vue_frontend_church vue_frontend_church_admin vue_frontend_invest_admin \
    nginx_proxy nginx_invest certbot \
    2>/dev/null || true

# 構建並啟動所有服務
echo ""
echo "開始構建和啟動服務..."
echo "這可能需要幾分鐘時間，請耐心等待..."

# 使用新版本或舊版本的 docker compose 命令
"${COMPOSE_CMD[@]}" up -d --build

echo "🔒 設定 cron（集中管理，避免覆蓋）"
bash ./scripts/setup/setup-prevention.sh

# 等待服務啟動
echo ""
echo "等待服務啟動..."
sleep 10

# 檢查服務狀態
echo ""
echo "檢查服務狀態..."
"${COMPOSE_CMD[@]}" ps

# 關鍵：若有 Restarting/Exited/Unhealthy，部署應視為失敗
if "${COMPOSE_CMD[@]}" ps | grep -Eq "Restarting|Exited|unhealthy"; then
    echo ""
    echo "❌ 部署失敗：偵測到容器異常狀態（Restarting/Exited/Unhealthy）"
    echo "最近 Nginx 日誌："
    "${COMPOSE_CMD[@]}" logs --tail=120 nginx || true
    exit 1
fi

echo ""
echo "=========================================="
echo "✅ 部署完成！"
echo "=========================================="
echo ""
if [ -f "certbot/conf/live/power-light-church.duckdns.org/fullchain.pem" ]; then
    echo "服務訪問地址（HTTPS）："
    echo "  - 前端: https://power-light-church.duckdns.org"
    echo "  - 個人後端 API: https://power-light-church.duckdns.org/api/**"
    echo "  - 教會後端 API: https://power-light-church.duckdns.org/api/church/**"
    echo "  - Invest 後端 API: https://power-light-church.duckdns.org/api/invest/**"
    echo "  - 個人 LINE Webhook: https://power-light-church.duckdns.org/api/personal/line/webhook"
    echo "  - 教會 LINE Webhook: https://power-light-church.duckdns.org/api/church/line/webhook"
else
    echo "服務訪問地址（HTTP）："
    echo "  - 前端: http://power-light-church.duckdns.org"
    echo "  - 個人後端 API: http://power-light-church.duckdns.org/api/**"
    echo "  - 教會後端 API: http://power-light-church.duckdns.org/api/church/**"
    echo "  - Invest 後端 API: http://power-light-church.duckdns.org/api/invest/**"
    echo ""
    echo "⚠️  HTTPS 設置："
    echo "  如需設置 HTTPS，請執行："
    echo "    ./setup-https-on-server.sh"
fi
echo ""
echo "數據庫信息："
echo "  - 個人資料庫: qa_tracker"
echo "  - 教會資料庫: church"
echo "  - 用戶名: appuser"
echo "  - 密碼: apppassword"
echo "  - Root 密碼: rootpassword"
echo ""
echo "查看日誌："
echo "  docker compose logs -f"
echo ""
echo "停止服務："
echo "  docker compose down"
echo ""
