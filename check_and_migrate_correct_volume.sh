#!/bin/bash

# 檢查所有 MySQL volumes 並找出包含實際資料的 volume

set -e

PROJECT_DIR="/root/project/work/docker-vue-java-mysql"
TARGET_DIR="/root/project/work/mysql"

echo "=========================================="
echo "檢查所有 MySQL Volumes 並找出有資料的"
echo "=========================================="
echo ""

# 定義要檢查的 volumes
VOLUMES=(
    "docker-vue-java-mysql_mysql_data"
    "qa_tracker_mysql_data"
    "work_mysql_data"
    "docker-vue-java-mysql_backup_mysql_data"
)

echo "檢查以下 volumes："
for vol in "${VOLUMES[@]}"; do
    echo "  - $vol"
done
echo ""

# 檢查每個 volume
declare -A VOLUME_SIZES
declare -A VOLUME_HAS_DATA

for volume in "${VOLUMES[@]}"; do
    echo "檢查 volume: $volume"
    
    # 檢查 volume 是否存在
    if ! docker volume inspect "$volume" &>/dev/null; then
        echo "  ⚠️  Volume 不存在，跳過"
        echo ""
        continue
    fi
    
    # 檢查 volume 大小
    SIZE=$(docker run --rm -v "$volume":/data alpine sh -c "du -sh /data 2>/dev/null | cut -f1" || echo "0")
    VOLUME_SIZES["$volume"]=$SIZE
    echo "  大小: $SIZE"
    
    # 檢查是否有 MySQL 資料文件
    echo "  檢查 MySQL 文件..."
    FILE_COUNT=$(docker run --rm -v "$volume":/data alpine sh -c "find /data -type f 2>/dev/null | wc -l" || echo "0")
    echo "  文件數量: $FILE_COUNT"
    
    # 檢查是否有 ibdata1（InnoDB 資料文件）
    HAS_IBDATA=$(docker run --rm -v "$volume":/data alpine sh -c "test -f /data/ibdata1 && echo 'yes' || echo 'no'" || echo "no")
    echo "  有 ibdata1: $HAS_IBDATA"
    
    # 檢查是否有資料庫目錄
    DB_COUNT=$(docker run --rm -v "$volume":/data alpine sh -c "ls -d /data/*/ 2>/dev/null | grep -E '(qa_tracker|church)' | wc -l" || echo "0")
    echo "  資料庫目錄數: $DB_COUNT"
    
    if [ "$FILE_COUNT" -gt 10 ] && [ "$HAS_IBDATA" = "yes" ]; then
        VOLUME_HAS_DATA[$volume]="yes"
        echo "  ✅ 這個 volume 看起來有資料"
    else
        VOLUME_HAS_DATA[$volume]="no"
        echo "  ❌ 這個 volume 可能是空的"
    fi
    echo ""
done

echo "=========================================="
echo "總結"
echo "=========================================="
echo ""

# 找出有資料的 volume
FOUND_VOLUME=""
for volume in "${VOLUMES[@]}"; do
    if [ "${VOLUME_HAS_DATA[$volume]:-no}" = "yes" ]; then
        echo "✅ $volume - 大小: ${VOLUME_SIZES[$volume]:-0} - 有資料"
        if [ -z "$FOUND_VOLUME" ]; then
            FOUND_VOLUME=$volume
        fi
    else
        echo "❌ $volume - 大小: ${VOLUME_SIZES[$volume]:-0} - 無資料或空"
    fi
done

echo ""

if [ -z "$FOUND_VOLUME" ]; then
    echo "⚠️  沒有找到包含資料的 volume"
    echo "可能所有 volumes 都是空的，或者需要檢查其他位置"
    exit 1
fi

echo "=========================================="
echo "找到有資料的 volume: $FOUND_VOLUME"
echo "=========================================="
echo ""
echo "是否要從這個 volume 遷移資料？(y/n)"
read -r CONFIRM

if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
    echo "取消遷移"
    exit 0
fi

# 執行遷移
echo ""
echo "開始遷移..."
echo ""

# 停止 MySQL
cd "$PROJECT_DIR"
docker compose stop mysql || true

# 備份當前目錄
if [ -d "$TARGET_DIR" ] && [ "$(ls -A $TARGET_DIR 2>/dev/null)" ]; then
    BACKUP_DIR="/root/project/work/mysql_backup_$(date +%Y%m%d_%H%M%S)"
    mv "$TARGET_DIR" "$BACKUP_DIR"
    echo "已備份當前目錄到: $BACKUP_DIR"
fi

mkdir -p "$TARGET_DIR"
chown -R 999:999 "$TARGET_DIR"
chmod 755 "$TARGET_DIR"

# 遷移資料
echo "從 $FOUND_VOLUME 遷移資料..."
docker run --rm \
    -v "$FOUND_VOLUME":/source:ro \
    -v "$TARGET_DIR":/target \
    alpine sh -c "cp -a /source/. /target/"

chown -R 999:999 "$TARGET_DIR"

# 啟動 MySQL
echo "啟動 MySQL..."
docker compose up -d mysql
sleep 30

# 驗證
echo "驗證資料..."
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;" || true
docker compose exec mysql mysql -u root -prootpassword qa_tracker -e "SELECT COUNT(*) as user_count FROM users;" 2>/dev/null || true
docker compose exec mysql mysql -u root -prootpassword church -e "SHOW TABLES;" 2>/dev/null || true

echo ""
echo "✅ 遷移完成！"

