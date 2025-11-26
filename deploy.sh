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

if [ ! -d "frontend" ]; then
    echo "❌ 錯誤: 找不到 frontend 目錄"
    exit 1
fi

if [ ! -f "mysql/schema.sql" ]; then
    echo "❌ 錯誤: 找不到 mysql/schema.sql"
    exit 1
fi

echo "✅ 項目文件檢查通過"

# 停止並刪除現有容器（如果存在）
# 注意：使用 docker compose down（不含 -v）會保留資料庫資料
# 只有使用 docker compose down -v 才會清空資料
echo ""
echo "停止並清理現有容器（資料庫資料會保留）..."
docker compose down 2>/dev/null || true

# 強制刪除可能殘留的容器（避免名稱衝突）
# ⚠️ 注意：只刪除容器，不會刪除 volume（資料庫資料會保留）
echo "清理殘留容器（資料庫資料會保留）..."
docker rm -f mysql_db java_backend vue_frontend 2>/dev/null || true

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

# 等待服務啟動
echo ""
echo "等待服務啟動..."
sleep 10

# 檢查服務狀態
echo ""
echo "檢查服務狀態..."
docker compose ps

echo ""
echo "=========================================="
echo "✅ 部署完成！"
echo "=========================================="
echo ""
echo "服務訪問地址："
echo "  - 前端: http://localhost (或您的域名)"
echo "  - 後端 API: http://localhost:8080/api/hello"
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

