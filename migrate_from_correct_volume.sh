#!/bin/bash

# 從正確的 volume 遷移 MySQL 資料

set -e

PROJECT_DIR="/root/project/work/docker-vue-java-mysql"
TARGET_DIR="/root/project/work/mysql"
SOURCE_VOLUME="docker-vue-java-mysql_mysql_data"

echo "=========================================="
echo "從正確的 Volume 遷移 MySQL 資料"
echo "=========================================="
echo ""
echo "來源 Volume: $SOURCE_VOLUME"
echo "目標目錄: $TARGET_DIR"
echo ""

# 步驟 1: 停止 MySQL
echo "[1/6] 停止 MySQL 服務..."
cd "$PROJECT_DIR"
docker compose stop mysql || true
echo "✅ MySQL 已停止"
echo ""

# 步驟 2: 備份當前目錄（如果存在）
echo "[2/6] 備份當前目錄..."
if [ -d "$TARGET_DIR" ] && [ "$(ls -A $TARGET_DIR 2>/dev/null)" ]; then
    BACKUP_DIR="/root/project/work/mysql_backup_$(date +%Y%m%d_%H%M%S)"
    mv "$TARGET_DIR" "$BACKUP_DIR"
    echo "✅ 已備份到: $BACKUP_DIR"
else
    echo "ℹ️  目標目錄為空，無需備份"
fi
mkdir -p "$TARGET_DIR"
chown -R 999:999 "$TARGET_DIR"
chmod 755 "$TARGET_DIR"
echo ""

# 步驟 3: 遷移資料
echo "[3/6] 遷移資料（這可能需要一些時間）..."
echo "從: $SOURCE_VOLUME"
echo "到: $TARGET_DIR"
docker run --rm \
    -v "$SOURCE_VOLUME":/source:ro \
    -v "$TARGET_DIR":/target \
    alpine sh -c "cp -a /source/. /target/"

if [ $? -eq 0 ]; then
    echo "✅ 資料遷移成功"
else
    echo "❌ 資料遷移失敗"
    exit 1
fi
echo ""

# 步驟 4: 設置權限
echo "[4/6] 設置目錄權限..."
chown -R 999:999 "$TARGET_DIR"
chmod 755 "$TARGET_DIR"
echo "✅ 權限設置完成"
echo ""

# 步驟 5: 啟動 MySQL
echo "[5/6] 啟動 MySQL 服務..."
cd "$PROJECT_DIR"
docker compose up -d mysql

echo "等待 MySQL 啟動（約 30 秒）..."
sleep 30

if docker compose ps mysql | grep -q "Up"; then
    echo "✅ MySQL 已啟動"
else
    echo "❌ MySQL 啟動失敗，請檢查日誌："
    echo "   docker compose logs mysql"
    exit 1
fi
echo ""

# 步驟 6: 驗證資料
echo "[6/6] 驗證資料庫..."
echo ""

echo "資料庫列表："
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;" 2>/dev/null || {
    echo "⚠️  無法連接到 MySQL，請稍後再驗證"
    exit 0
}

echo ""
echo "檢查 qa_tracker 資料庫："
USER_COUNT=$(docker compose exec mysql mysql -u root -prootpassword qa_tracker -e "SELECT COUNT(*) as cnt FROM users;" 2>/dev/null | tail -1 || echo "0")
echo "  users 表資料數量: $USER_COUNT"

RECORD_COUNT=$(docker compose exec mysql mysql -u root -prootpassword qa_tracker -e "SELECT COUNT(*) as cnt FROM records;" 2>/dev/null | tail -1 || echo "0")
echo "  records 表資料數量: $RECORD_COUNT"

echo ""
echo "檢查 church 資料庫："
docker compose exec mysql mysql -u root -prootpassword church -e "SHOW TABLES;" 2>/dev/null || echo "  church 資料庫不存在或為空"

echo ""
echo "=========================================="
echo "✅ 遷移完成！"
echo "=========================================="
echo ""
echo "如果 church 資料庫不存在，可以執行："
echo "  docker compose exec -T mysql mysql -u root -prootpassword < ./mysql/church-schema.sql"

