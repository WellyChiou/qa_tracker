#!/bin/bash

# 服務診斷腳本
# 用於檢查 Docker 容器狀態和日誌

echo "=========================================="
echo "Docker 服務診斷"
echo "=========================================="

echo ""
echo "1. 檢查容器狀態..."
docker compose ps

echo ""
echo "2. 檢查所有容器（包括停止的）..."
docker ps -a | grep -E "mysql_db|java_backend|vue_frontend"

echo ""
echo "3. 檢查後端容器狀態..."
docker compose ps backend

echo ""
echo "4. 檢查後端日誌（最後 50 行）..."
docker compose logs --tail=50 backend

echo ""
echo "5. 檢查後端容器是否運行..."
if docker ps | grep -q java_backend; then
    echo "✅ 後端容器正在運行"
    echo ""
    echo "6. 查看實時日誌（按 Ctrl+C 退出）..."
    echo "執行: docker compose logs -f backend"
else
    echo "❌ 後端容器未運行"
    echo ""
    echo "嘗試啟動後端..."
    docker compose up -d backend
    echo ""
    echo "等待 5 秒後查看日誌..."
    sleep 5
    docker compose logs --tail=20 backend
fi

echo ""
echo "=========================================="
echo "診斷完成"
echo "=========================================="

