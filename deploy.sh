#!/bin/bash

# 自動部署腳本
# 用於在虛擬主機上自動部署整個項目

set -e  # 遇到錯誤立即退出

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
if ! command -v docker compose &> /dev/null && ! command -v docker-compose &> /dev/null; then
    echo "❌ 錯誤: Docker Compose 未安裝"
    echo "請先安裝 Docker Compose"
    exit 1
fi

echo "✅ Docker 環境檢查通過"

# 檢查端口是否被佔用
echo ""
echo "檢查端口..."
if lsof -Pi :80 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo "⚠️  警告: 端口 80 已被佔用"
    read -p "是否繼續？(y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo "⚠️  警告: 端口 8080 已被佔用"
    read -p "是否繼續？(y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# 檢查必要的文件
echo ""
echo "檢查項目文件..."
if [ ! -f "docker-compose.yml" ]; then
    echo "❌ 錯誤: 找不到 docker-compose.yml"
    exit 1
fi

if [ ! -d "backend" ]; then
    echo "❌ 錯誤: 找不到 backend 目錄"
    exit 1
fi

if [ ! -d "frontend-personal" ]; then
    echo "❌ 錯誤: 找不到 frontend-personal 目錄"
    exit 1
fi

if [ ! -f "mysql/schema.sql" ]; then
    echo "❌ 錯誤: 找不到 mysql/schema.sql"
    exit 1
fi

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

# 停止並刪除現有容器（如果存在）
# 注意：使用 docker compose down（不含 -v）會保留資料庫資料
# 只有使用 docker compose down -v 才會清空資料
echo ""
echo "停止並清理現有容器（資料庫資料會保留）..."
docker compose down 2>/dev/null || true

# 強制刪除可能殘留的容器（避免名稱衝突）
# ⚠️ 注意：只刪除容器，不會刪除 volume（資料庫資料會保留）
echo "清理殘留容器（資料庫資料會保留）..."
docker rm -f mysql_db java_backend vue_personal nginx_proxy certbot 2>/dev/null || true

# 構建並啟動所有服務
echo ""
echo "開始構建和啟動服務..."
echo "這可能需要幾分鐘時間，請耐心等待..."

# 使用新版本或舊版本的 docker compose 命令
if command -v docker compose &> /dev/null; then
    docker compose up -d --build
else
    docker-compose up -d --build
fi

echo "🔒 設定 cron（集中管理，避免覆蓋）"
bash ./setup-prevention.sh

# 等待服務啟動
echo ""
echo "等待服務啟動..."
sleep 10

# 自動檢測並應用 HTTPS 配置
echo ""
echo "檢查 HTTPS 證書..."
if [ -f "certbot/conf/live/power-light-church.duckdns.org/fullchain.pem" ]; then
    echo "✅ 檢測到 SSL 證書"
    
    # 檢查當前的 nginx.conf 是否已經包含 HTTPS 配置
    if grep -q "listen 443 ssl" nginx/nginx.conf 2>/dev/null; then
        echo "✅ nginx.conf 已包含 HTTPS 配置，無需切換"
    elif [ -f "nginx/nginx-https.conf" ]; then
        cp nginx/nginx-https.conf nginx/nginx.conf
        echo "✅ 已切換到 HTTPS 配置"
    elif [ -f "nginx/nginx-https.conf.bak" ]; then
        cp nginx/nginx-https.conf.bak nginx/nginx.conf
        echo "✅ 已從備份文件切換到 HTTPS 配置"
    else
        echo "⚠️  警告: 找不到 nginx-https.conf，但 nginx.conf 可能已包含 HTTPS 配置"
        echo "   請確認 nginx.conf 是否正確配置了 HTTPS"
    fi
else
    echo "ℹ️  未檢測到 SSL 證書，使用 HTTP 配置"
    echo "   如需設置 HTTPS，請執行: ./setup-https-on-server.sh"
fi

# 檢查服務狀態
echo ""
echo "檢查服務狀態..."
docker compose ps

echo ""
echo "=========================================="
echo "✅ 部署完成！"
echo "=========================================="
echo ""
if [ -f "certbot/conf/live/power-light-church.duckdns.org/fullchain.pem" ]; then
    echo "服務訪問地址（HTTPS）："
    echo "  - 前端: https://power-light-church.duckdns.org"
    echo "  - 後端 API: https://power-light-church.duckdns.org/api"
    echo "  - LINE Bot Webhook: https://power-light-church.duckdns.org/api/line/webhook"
else
    echo "服務訪問地址（HTTP）："
    echo "  - 前端: http://power-light-church.duckdns.org"
    echo "  - 後端 API: http://power-light-church.duckdns.org/api"
    echo ""
    echo "⚠️  HTTPS 設置："
    echo "  如需設置 HTTPS，請執行："
    echo "    ./setup-https-on-server.sh"
fi
echo ""
echo "數據庫信息："
echo "  - 數據庫名: qa_tracker"
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

