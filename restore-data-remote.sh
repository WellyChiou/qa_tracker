#!/bin/bash

# 從遠程服務器的 work_mysql_data volume 恢復資料到 docker-vue-java-mysql_mysql_data
# 這個腳本在本地執行，通過 SSH 連接到遠程服務器

set -e

# 配置
SERVER_USER="root"
SERVER_HOST="38.54.89.136"
SERVER_PASSWORD="!Welly775"
REMOTE_PATH="/root/project/work/docker-vue-java-mysql"

SOURCE_VOLUME="work_mysql_data"
TARGET_VOLUME="docker-vue-java-mysql_mysql_data"

echo "=========================================="
echo "從遠程服務器恢復資料庫資料"
echo "=========================================="
echo ""
echo "服務器: ${SERVER_USER}@${SERVER_HOST}"
echo "來源 Volume: $SOURCE_VOLUME"
echo "目標 Volume: $TARGET_VOLUME"
echo ""
read -p "確定要覆蓋目標 volume 的資料嗎？(yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "操作已取消"
    exit 0
fi

# 檢查 sshpass
if ! command -v sshpass &> /dev/null; then
    echo "❌ 錯誤：需要安裝 sshpass"
    echo "   安裝方法: brew install hudochenkov/sshpass/sshpass"
    exit 1
fi

echo ""
echo "正在連接到遠程服務器..."

# 在遠程服務器上執行恢復
sshpass -p "${SERVER_PASSWORD}" ssh -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_HOST} << EOF
    set -e
    
    cd ${REMOTE_PATH} || {
        echo "❌ 錯誤：找不到專案目錄 ${REMOTE_PATH}"
        exit 1
    }
    
    echo ""
    echo "步驟 1: 檢查 volumes..."
    if ! docker volume inspect "$SOURCE_VOLUME" > /dev/null 2>&1; then
        echo "❌ 錯誤：來源 volume '$SOURCE_VOLUME' 不存在"
        echo "可用的 volumes:"
        docker volume ls | grep mysql || echo "沒有找到 MySQL volumes"
        exit 1
    fi
    
    if ! docker volume inspect "$TARGET_VOLUME" > /dev/null 2>&1; then
        echo "❌ 錯誤：目標 volume '$TARGET_VOLUME' 不存在"
        exit 1
    fi
    
    echo "✅ Volumes 檢查完成"
    
    echo ""
    echo "步驟 2: 停止 MySQL 容器..."
    docker compose stop mysql 2>/dev/null || true
    
    echo ""
    echo "步驟 3: 複製資料..."
    echo "這可能需要一些時間，請耐心等待..."
    
    docker run --rm \\
        -v "$SOURCE_VOLUME":/source:ro \\
        -v "$TARGET_VOLUME":/target \\
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
    echo "步驟 4: 驗證資料..."
    DATA_EXISTS=\$(docker run --rm -v "$TARGET_VOLUME":/data alpine sh -c "ls -A /data | wc -l" 2>/dev/null || echo "0")
    
    if [ "\$DATA_EXISTS" -gt "0" ]; then
        echo "✅ 資料已成功複製（找到 \$DATA_EXISTS 個項目）"
    else
        echo "⚠️  警告：目標 volume 似乎是空的"
    fi
    
    echo ""
    echo "步驟 5: 啟動 MySQL 容器..."
    docker compose up -d mysql
    
    echo ""
    echo "等待 MySQL 啟動..."
    sleep 10
    
    echo ""
    echo "步驟 6: 驗證資料庫..."
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
EOF

echo ""
echo "✅ 遠程恢復操作完成！"

