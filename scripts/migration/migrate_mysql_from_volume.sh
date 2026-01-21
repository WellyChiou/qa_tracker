#!/bin/bash

# MySQL Volume 遷移腳本
# 從 Docker named volume 遷移到主機目錄 /root/project/work/mysql

set -e  # 遇到錯誤立即退出

PROJECT_DIR="/root/project/work/docker-vue-java-mysql"
TARGET_DIR="/root/project/work/mysql"

echo "=========================================="
echo "MySQL Volume 遷移腳本"
echo "=========================================="
echo ""

# 步驟 1: 檢查專案目錄
echo "[1/8] 檢查專案目錄..."
if [ ! -f "$PROJECT_DIR/docker-compose.yml" ]; then
    echo "❌ 錯誤：找不到 docker-compose.yml 在 $PROJECT_DIR"
    echo "請確認專案根目錄路徑是否正確"
    exit 1
fi
echo "✅ 專案目錄: $PROJECT_DIR"

# 步驟 2: 查找舊 volume
echo ""
echo "[2/8] 查找舊 MySQL volume..."
VOLUME_NAME=$(docker volume ls | grep mysql_data | awk '{print $2}' | head -1)

if [ -z "$VOLUME_NAME" ]; then
    echo "⚠️  找不到 mysql_data volume，嘗試查找其他可能的 volume..."
    docker volume ls | grep -i mysql || echo "沒有找到包含 mysql 的 volume"
    echo ""
    echo "請手動輸入 volume 名稱（或按 Enter 跳過遷移，直接初始化新資料庫）："
    read -r VOLUME_NAME
    if [ -z "$VOLUME_NAME" ]; then
        echo "跳過遷移，將初始化新資料庫"
        SKIP_MIGRATION=true
    fi
else
    echo "✅ 找到 volume: $VOLUME_NAME"
    SKIP_MIGRATION=false
fi

# 步驟 3: 停止 MySQL 服務
echo ""
echo "[3/8] 停止 MySQL 服務..."
cd "$PROJECT_DIR"
if docker compose ps mysql | grep -q "Up"; then
    docker compose stop mysql
    echo "✅ MySQL 已停止"
else
    echo "ℹ️  MySQL 已經停止"
fi

# 步驟 4: 創建目標目錄
echo ""
echo "[4/8] 創建目標目錄..."
mkdir -p "$TARGET_DIR"

# 檢查目錄是否為空
if [ "$(ls -A $TARGET_DIR 2>/dev/null)" ]; then
    echo "⚠️  目標目錄不為空，備份現有內容..."
    BACKUP_DIR="/root/project/work/mysql_backup_$(date +%Y%m%d_%H%M%S)"
    mv "$TARGET_DIR" "$BACKUP_DIR"
    echo "✅ 已備份到: $BACKUP_DIR"
    mkdir -p "$TARGET_DIR"
fi

chown -R 999:999 "$TARGET_DIR"
chmod 755 "$TARGET_DIR"
echo "✅ 目標目錄已創建: $TARGET_DIR"

# 步驟 5: 遷移資料（如果找到 volume）
if [ "$SKIP_MIGRATION" = false ] && [ -n "$VOLUME_NAME" ]; then
    echo ""
    echo "[5/8] 遷移資料（這可能需要一些時間）..."
    echo "從 volume: $VOLUME_NAME"
    echo "到目錄: $TARGET_DIR"
    
    if docker run --rm \
        -v "$VOLUME_NAME":/source:ro \
        -v "$TARGET_DIR":/target \
        alpine sh -c "cp -a /source/. /target/"; then
        echo "✅ 資料遷移成功"
    else
        echo "❌ 資料遷移失敗"
        echo "請檢查 volume 名稱是否正確"
        exit 1
    fi
else
    echo ""
    echo "[5/8] 跳過資料遷移（將初始化新資料庫）"
fi

# 步驟 6: 設置權限
echo ""
echo "[6/8] 設置目錄權限..."
chown -R 999:999 "$TARGET_DIR"
chmod 755 "$TARGET_DIR"
echo "✅ 權限設置完成"

# 步驟 7: 啟動 MySQL
echo ""
echo "[7/8] 啟動 MySQL 服務..."
cd "$PROJECT_DIR"
docker compose up -d mysql

echo "等待 MySQL 啟動（約 30 秒）..."
sleep 30

# 檢查 MySQL 是否啟動成功
if docker compose ps mysql | grep -q "Up"; then
    echo "✅ MySQL 已啟動"
else
    echo "❌ MySQL 啟動失敗，請檢查日誌："
    echo "   docker compose logs mysql"
    exit 1
fi

# 步驟 8: 驗證資料
echo ""
echo "[8/8] 驗證資料庫..."
echo ""

# 檢查資料庫
echo "資料庫列表："
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;" 2>/dev/null || {
    echo "⚠️  無法連接到 MySQL，請稍後再驗證"
    echo "   執行: docker compose exec mysql mysql -u root -prootpassword -e 'SHOW DATABASES;'"
    exit 0
}

echo ""
echo "檢查 qa_tracker 資料庫："
docker compose exec mysql mysql -u root -prootpassword qa_tracker -e "SHOW TABLES;" 2>/dev/null || echo "  qa_tracker 資料庫為空或不存在"

echo ""
echo "檢查 church 資料庫："
docker compose exec mysql mysql -u root -prootpassword church -e "SHOW TABLES;" 2>/dev/null || echo "  church 資料庫為空或不存在"

echo ""
echo "=========================================="
echo "✅ 遷移完成！"
echo "=========================================="
echo ""
echo "如果資料庫為空，可以執行初始化腳本："
echo "  docker compose exec -T mysql mysql -u root -prootpassword < ./mysql/personal/schema/schema.sql"
echo "  docker compose exec -T mysql mysql -u root -prootpassword < ./mysql/church/schema/church-schema.sql"
echo ""
echo "驗證命令："
echo "  docker compose exec mysql mysql -u root -prootpassword -e 'SHOW DATABASES;'"
echo "  docker compose exec mysql mysql -u root -prootpassword qa_tracker -e 'SHOW TABLES;'"
echo "  docker compose exec mysql mysql -u root -prootpassword church -e 'SHOW TABLES;'"

