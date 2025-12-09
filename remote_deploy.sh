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
    
    echo "✅ 遠程部署完成！"
    
    # 步驟 5: 設置預防機制
    # 注意：暫時關閉 set -e，避免預防機制設置失敗影響部署
    set +e
    
    echo ""
    echo "=========================================="
    echo "設置前端白屏預防機制"
    echo "=========================================="
    
    # 獲取當前專案目錄的絕對路徑
    PROJECT_DIR="$(pwd)"
    
    # 確保所有腳本有執行權限
    echo "設置腳本執行權限..."
    chmod +x monitor-frontend.sh monitor-system.sh cleanup-docker.sh fix-frontend.sh diagnose-frontend.sh setup-prevention.sh 2>/dev/null || true
    
    # 設置前端監控（每 5 分鐘）
    echo "設置前端監控（每 5 分鐘檢查一次）..."
    CRON_FRONTEND="*/5 * * * * cd $PROJECT_DIR && $PROJECT_DIR/monitor-frontend.sh"
    if crontab -l 2>/dev/null | grep -q "monitor-frontend.sh"; then
        echo "⚠️  前端監控任務已存在，跳過"
    else
        (crontab -l 2>/dev/null; echo "$CRON_FRONTEND") | crontab -
        echo "✅ 前端監控任務已添加"
    fi
    
    # 設置系統資源監控（每小時）
    echo "設置系統資源監控（每小時檢查一次）..."
    CRON_SYSTEM="0 * * * * cd $PROJECT_DIR && $PROJECT_DIR/monitor-system.sh"
    if crontab -l 2>/dev/null | grep -q "monitor-system.sh"; then
        echo "⚠️  系統監控任務已存在，跳過"
    else
        (crontab -l 2>/dev/null; echo "$CRON_SYSTEM") | crontab -
        echo "✅ 系統監控任務已添加"
    fi
    
    # 設置定期 Docker 清理（每天凌晨 2 點）
    # 注意：只清理未使用的資源，不會刪除正在使用的映像和容器
    echo "設置定期 Docker 清理（每天凌晨 2 點）..."
    CRON_CLEANUP="0 2 * * * cd $PROJECT_DIR && docker system prune -f && docker image prune -f"
    if crontab -l 2>/dev/null | grep -q "docker system prune"; then
        echo "⚠️  Docker 清理任務已存在，跳過"
    else
        (crontab -l 2>/dev/null; echo "$CRON_CLEANUP") | crontab -
        echo "✅ Docker 清理任務已添加"
    fi
    
    # 創建日誌目錄
    echo "創建日誌目錄..."
    mkdir -p /var/log 2>/dev/null || true
    touch /var/log/frontend-monitor.log 2>/dev/null || true
    touch /var/log/system-monitor.log 2>/dev/null || true
    chmod 666 /var/log/frontend-monitor.log /var/log/system-monitor.log 2>/dev/null || true
    echo "✅ 日誌目錄已創建"
    
    echo ""
    echo "✅ 預防機制設置完成！"
    echo ""
    echo "已設置的監控任務："
    echo "  - 前端監控: 每 5 分鐘檢查一次，自動修復問題"
    echo "  - 系統資源監控: 每小時檢查一次，自動清理資源"
    echo "  - Docker 清理: 每天凌晨 2 點執行"
    echo ""
    
    # 恢復 set -e（如果之前有設置）
    set -e
else
    echo "Error: Cannot find $PROJECT_NAME directory after extraction"
    exit 1
fi
