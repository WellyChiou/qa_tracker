#!/bin/bash
set -e

REMOTE_PATH="/root/project/work"
PROJECT_NAME="docker-vue-java-mysql"
ARCHIVE_NAME="docker-vue-java-mysql.tar.gz"

cd "$REMOTE_PATH"

# 備份證書目錄（如果存在）
CERT_BACKUP_DIR="/tmp/${PROJECT_NAME}_cert_backup"
if [ -d "$PROJECT_NAME/certbot/conf" ]; then
    echo "備份 SSL 證書..."
    rm -rf "$CERT_BACKUP_DIR"
    cp -r "$PROJECT_NAME/certbot/conf" "$CERT_BACKUP_DIR" 2>/dev/null || true
    echo "✅ 證書已備份"
fi

if [ -d "$PROJECT_NAME" ]; then
    if [ -d "${PROJECT_NAME}_backup" ]; then
        rm -rf "${PROJECT_NAME}_backup"
    fi
    mv "$PROJECT_NAME" "${PROJECT_NAME}_backup"
fi

echo "Extracting archive..."
tar -xzf "$ARCHIVE_NAME" 2>&1 | grep -v "Ignoring unknown extended header" || true

REMOTE_DIR=$(tar -tzf "$ARCHIVE_NAME" | head -1 | cut -d'/' -f1)
if [ -d "$REMOTE_DIR" ] && [ "$REMOTE_DIR" != "$PROJECT_NAME" ]; then
    echo "Renaming directory from $REMOTE_DIR to $PROJECT_NAME"
    mv "$REMOTE_DIR" "$PROJECT_NAME"
fi

if [ -d "$PROJECT_NAME" ]; then
    cd "$PROJECT_NAME"
    
    # 恢復證書目錄（如果備份存在且新目錄中沒有證書）
    if [ -d "$CERT_BACKUP_DIR" ] && [ ! -d "certbot/conf/live" ]; then
        echo "恢復 SSL 證書..."
        mkdir -p certbot/conf
        cp -r "$CERT_BACKUP_DIR"/* certbot/conf/ 2>/dev/null || true
        echo "✅ 證書已恢復"
    fi
    
    # Fix Windows line ending issue
    sed -i 's/\r$//' deploy.sh
    chmod +x deploy.sh
    ./deploy.sh
else
    echo "Error: Cannot find $PROJECT_NAME directory after extraction"
    exit 1
fi
