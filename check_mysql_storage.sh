#!/bin/bash

# 檢查 MySQL 存儲配置和資料安全

echo "=========================================="
echo "MySQL 存儲配置和安全檢查"
echo "=========================================="
echo ""

# 檢查 1: docker-compose.yml 配置
echo "[1/5] 檢查 docker-compose.yml 配置..."
if [ -f "docker-compose.yml" ]; then
    if grep -q "/root/project/work/mysql:/var/lib/mysql" docker-compose.yml; then
        echo "✅ 配置正確：使用主機目錄 /root/project/work/mysql"
    else
        echo "❌ 配置錯誤：可能仍在使用 volume"
        echo "當前配置："
        grep -A 2 "volumes:" docker-compose.yml | grep mysql
    fi
else
    echo "❌ 找不到 docker-compose.yml"
fi

echo ""

# 檢查 2: 主機目錄
echo "[2/5] 檢查主機目錄資料..."
if [ -d "/root/project/work/mysql" ]; then
    SIZE=$(du -sh /root/project/work/mysql 2>/dev/null | cut -f1)
    FILE_COUNT=$(find /root/project/work/mysql -type f 2>/dev/null | wc -l)
    echo "✅ 主機目錄存在"
    echo "   大小: $SIZE"
    echo "   文件數: $FILE_COUNT"
    
    # 檢查是否有 MySQL 資料文件
    if [ -f "/root/project/work/mysql/ibdata1" ]; then
        echo "✅ 找到 MySQL 資料文件 (ibdata1)"
    else
        echo "⚠️  未找到 ibdata1（可能目錄為空）"
    fi
    
    # 檢查是否有資料庫目錄
    if [ -d "/root/project/work/mysql/qa_tracker" ]; then
        echo "✅ 找到 qa_tracker 資料庫目錄"
    fi
else
    echo "❌ 主機目錄不存在"
fi

echo ""

# 檢查 3: 容器狀態
echo "[3/5] 檢查 MySQL 容器狀態..."
CONTAINER_NAME=$(docker compose ps mysql --format "{{.Name}}" 2>/dev/null | head -1)
if [ -n "$CONTAINER_NAME" ]; then
    echo "✅ 找到容器: $CONTAINER_NAME"
    
    # 檢查容器使用的存儲
    echo "   檢查容器掛載點..."
    MOUNT_INFO=$(docker inspect "$CONTAINER_NAME" 2>/dev/null | grep -A 20 '"Mounts"' | grep -E '(Source|Destination)' | head -4)
    if echo "$MOUNT_INFO" | grep -q "/root/project/work/mysql"; then
        echo "✅ 容器使用主機目錄: /root/project/work/mysql"
    else
        echo "⚠️  無法確認容器使用的存儲位置"
        echo "   掛載信息:"
        echo "$MOUNT_INFO" | head -4
    fi
else
    echo "⚠️  MySQL 容器未運行"
    echo "   執行 'docker compose ps' 查看所有容器"
fi

echo ""

# 檢查 4: 資料庫可訪問性
echo "[4/5] 檢查資料庫可訪問性..."
if docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;" &>/dev/null; then
    echo "✅ 資料庫可以正常查詢"
    
    # 檢查資料庫列表
    DB_LIST=$(docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;" 2>/dev/null | grep -v "Database\|information_schema\|mysql\|performance_schema\|sys" || echo "")
    if [ -n "$DB_LIST" ]; then
        echo "   資料庫列表:"
        echo "$DB_LIST" | sed 's/^/     /'
    fi
    
    # 檢查資料數量
    USER_COUNT=$(docker compose exec mysql mysql -u root -prootpassword qa_tracker -e "SELECT COUNT(*) as cnt FROM users;" 2>/dev/null | tail -1 || echo "0")
    echo "   qa_tracker.users 資料數量: $USER_COUNT"
else
    echo "⚠️  無法查詢資料庫（可能 MySQL 未運行）"
fi

echo ""

# 檢查 5: Volume 使用情況
echo "[5/5] 檢查 volume 使用情況..."
VOLUME_NAME="qa_tracker_mysql_data"
USING_CONTAINERS=$(docker ps -a --filter volume=$VOLUME_NAME --format "{{.ID}}" 2>/dev/null | wc -l)
if [ "$USING_CONTAINERS" -eq 0 ]; then
    echo "✅ 沒有容器使用 volume: $VOLUME_NAME"
    echo "   可以安全刪除"
else
    echo "⚠️  有 $USING_CONTAINERS 個容器使用該 volume"
    echo "   容器列表:"
    docker ps -a --filter volume=$VOLUME_NAME --format "  {{.ID}} {{.Names}} {{.Status}}"
fi

echo ""
echo "=========================================="
echo "總結"
echo "=========================================="
echo ""

# 判斷是否可以安全刪除
if [ -d "/root/project/work/mysql" ] && [ -f "/root/project/work/mysql/ibdata1" ]; then
    if grep -q "/root/project/work/mysql:/var/lib/mysql" docker-compose.yml 2>/dev/null; then
        if [ "$USING_CONTAINERS" -eq 0 ]; then
            echo "✅ 可以安全刪除 volume"
            echo ""
            echo "執行以下命令："
            echo "  docker compose down"
            echo "  docker volume rm $VOLUME_NAME"
            echo "  docker compose up -d"
        else
            echo "⚠️  需要先停止使用該 volume 的容器"
            echo "   執行: docker compose down"
        fi
    else
        echo "⚠️  請確認 docker-compose.yml 配置使用主機目錄"
    fi
else
    echo "❌ 主機目錄資料不完整，請勿刪除 volume"
fi

echo ""
echo "建議：刪除前先備份主機目錄"
echo "  tar -czf /root/project/work/mysql_safety_backup_\$(date +%Y%m%d_%H%M%S).tar.gz -C /root/project/work mysql"

