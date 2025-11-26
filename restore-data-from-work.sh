#!/bin/bash

# 從 work_mysql_data volume 恢復資料到 docker-vue-java-mysql_mysql_data

set -e

SOURCE_VOLUME="work_mysql_data"
TARGET_VOLUME="docker-vue-java-mysql_mysql_data"

echo "=========================================="
echo "恢復資料庫資料"
echo "=========================================="
echo ""
echo "來源 Volume: $SOURCE_VOLUME"
echo "目標 Volume: $TARGET_VOLUME"
echo ""
read -p "確定要覆蓋目標 volume 的資料嗎？(yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "操作已取消"
    exit 0
fi

echo ""
echo "步驟 1: 停止 MySQL 容器..."
docker compose stop mysql 2>/dev/null || true

echo ""
echo "步驟 2: 檢查 volumes 是否存在..."
if ! docker volume inspect "$SOURCE_VOLUME" > /dev/null 2>&1; then
    echo "❌ 錯誤：來源 volume '$SOURCE_VOLUME' 不存在"
    exit 1
fi

if ! docker volume inspect "$TARGET_VOLUME" > /dev/null 2>&1; then
    echo "❌ 錯誤：目標 volume '$TARGET_VOLUME' 不存在"
    exit 1
fi

echo "✅ Volumes 檢查完成"

echo ""
echo "步驟 3: 備份目標 volume（可選）..."
BACKUP_NAME="${TARGET_VOLUME}_backup_$(date +%Y%m%d_%H%M%S)"
echo "創建備份: $BACKUP_NAME"
docker run --rm \
    -v "$TARGET_VOLUME":/source:ro \
    -v "$BACKUP_NAME":/backup \
    alpine tar czf /backup/backup.tar.gz -C /source . 2>/dev/null || echo "備份跳過（volume 可能為空）"

echo ""
echo "步驟 4: 複製資料..."
echo "這可能需要一些時間，請耐心等待..."

# 使用臨時容器複製資料
docker run --rm \
    -v "$SOURCE_VOLUME":/source:ro \
    -v "$TARGET_VOLUME":/target \
    alpine sh -c "
        echo '清空目標 volume...'
        rm -rf /target/*
        rm -rf /target/.* 2>/dev/null || true
        
        echo '複製資料...'
        cp -a /source/* /target/ 2>/dev/null || true
        cp -a /source/.* /target/ 2>/dev/null || true
        
        echo '設置權限...'
        chown -R 999:999 /target 2>/dev/null || true
        
        echo '✅ 資料複製完成'
    "

echo ""
echo "步驟 5: 驗證資料..."
# 檢查目標 volume 中是否有資料
DATA_EXISTS=$(docker run --rm -v "$TARGET_VOLUME":/data alpine sh -c "ls -A /data | wc -l" 2>/dev/null || echo "0")

if [ "$DATA_EXISTS" -gt "0" ]; then
    echo "✅ 資料已成功複製（找到 $DATA_EXISTS 個項目）"
else
    echo "⚠️  警告：目標 volume 似乎是空的"
fi

echo ""
echo "步驟 6: 啟動 MySQL 容器..."
docker compose up -d mysql

echo ""
echo "等待 MySQL 啟動..."
sleep 10

echo ""
echo "步驟 7: 驗證資料庫..."
docker compose exec mysql mysql -u root -prootpassword -e "
USE qa_tracker;
SELECT 'users' AS table_name, COUNT(*) AS count FROM users
UNION ALL SELECT 'records', COUNT(*) FROM records
UNION ALL SELECT 'expenses', COUNT(*) FROM expenses
UNION ALL SELECT 'assets', COUNT(*) FROM assets;" 2>/dev/null || echo "⚠️  無法連接到資料庫，請檢查容器日誌"

echo ""
echo "=========================================="
echo "恢復完成！"
echo "=========================================="
echo ""
echo "備份位置: $BACKUP_NAME (如果創建成功)"
echo ""
echo "如果資料有問題，可以從備份恢復："
echo "  docker volume create ${TARGET_VOLUME}_restore"
echo "  docker run --rm -v $BACKUP_NAME:/backup -v ${TARGET_VOLUME}_restore:/target alpine tar xzf /backup/backup.tar.gz -C /target"

