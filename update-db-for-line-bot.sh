#!/bin/bash

# LINE Bot 資料庫遷移腳本
# 此腳本幫助更新資料庫結構以支援 LINE Bot 功能

echo "🔄 開始更新資料庫以支援 LINE Bot 功能..."

# 檢查 Docker 是否運行
if ! docker ps | grep -q mysql; then
    echo "❌ MySQL 容器未運行，請先啟動應用程式："
    echo "   docker compose up -d mysql"
    echo "   # 或使用舊版命令：docker-compose up -d mysql"
    exit 1
fi

echo "📊 檢查當前 users 表結構..."
if command -v "docker-compose" &> /dev/null; then
    docker-compose exec mysql mysql -u appuser -papppassword qa_tracker -e "DESCRIBE users;"
elif command -v "docker" &> /dev/null && docker compose version &> /dev/null; then
    docker compose exec mysql mysql -u appuser -papppassword qa_tracker -e "DESCRIBE users;"
else
    echo "❌ 找不到 Docker 命令。請確保 Docker 已安裝並運行。"
    echo "   或者手動執行以下命令："
    echo "   docker-compose exec mysql mysql -u appuser -papppassword qa_tracker -e \"DESCRIBE users;\""
    exit 1
fi

echo ""
echo "🔧 執行資料庫遷移..."

# 複製並執行遷移腳本
if command -v "docker-compose" &> /dev/null; then
    docker-compose cp mysql/check-and-update-users-table.sql mysql:/tmp/
    docker-compose exec mysql mysql -u appuser -papppassword qa_tracker < mysql/check-and-update-users-table.sql
elif command -v "docker" &> /dev/null && docker compose version &> /dev/null; then
    docker compose cp mysql/check-and-update-users-table.sql mysql:/tmp/
    docker compose exec mysql mysql -u appuser -papppassword qa_tracker < mysql/check-and-update-users-table.sql
else
    echo "❌ 找不到 Docker 命令。請確保 Docker 已安裝並運行。"
    echo "   或者手動執行資料庫遷移："
    echo "   1. docker-compose cp mysql/check-and-update-users-table.sql mysql:/tmp/"
    echo "   2. docker-compose exec mysql mysql -u appuser -papppassword qa_tracker < mysql/check-and-update-users-table.sql"
    exit 1
fi

echo ""
echo "✅ 驗證更新後的表結構..."
if command -v "docker-compose" &> /dev/null; then
    docker-compose exec mysql mysql -u appuser -papppassword qa_tracker -e "DESCRIBE users;"
elif command -v "docker" &> /dev/null && docker compose version &> /dev/null; then
    docker compose exec mysql mysql -u appuser -papppassword qa_tracker -e "DESCRIBE users;"
else
    echo "❌ 找不到 Docker 命令。請確保 Docker 已安裝並運行。"
    echo "   或者手動驗證："
    echo "   docker-compose exec mysql mysql -u appuser -papppassword qa_tracker -e \"DESCRIBE users;\""
fi

echo ""
echo "🎉 資料庫遷移完成！"
echo "   現在可以重啟應用程式以使用 LINE Bot 功能："
echo "   docker compose restart backend frontend"
echo "   # 或使用舊版命令：docker-compose restart backend frontend"
