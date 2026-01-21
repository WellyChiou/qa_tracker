#!/bin/bash
set -e

# 設置基本路徑和名稱
REMOTE_PATH="/root/project/work"
PROJECT_NAME="docker-vue-java-mysql"
ARCHIVE_NAME="${PROJECT_NAME}.tar.gz"

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
    
    # Fix Windows line ending issue and make scripts executable
    echo "Fixing line endings and setting permissions..."
    # 使用 sed 移除 Windows 風格的換行符 (\r)
    find . -type f -name "*.sh" -exec sed -i 's/\r$//' {} \;
    # 設置執行權限
    find . -type f -name "*.sh" -exec chmod +x {} \;
    
    # 確保 deploy.sh 有執行權限
    if [ -f "scripts/deployment/deploy.sh" ]; then
        chmod +x scripts/deployment/deploy.sh
        # 執行部署
        ./scripts/deployment/deploy.sh
    else
        echo "❌ 錯誤: 找不到 scripts/deployment/deploy.sh 文件"
        exit 1
    fi
    
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

    # 設置日誌目錄和日誌管理
    LOGS_DIR="/root/project/work/logs"
    mkdir -p "$LOGS_DIR" 2>/dev/null || true
    chmod -R 777 "$LOGS_DIR" 2>/dev/null || true
    
    echo "✅ 日誌目錄已創建: $LOGS_DIR"
    echo "當前用戶: $(whoami)"
    echo "目錄權限:"
    ls -ld "$LOGS_DIR" 2>/dev/null || echo "無法獲取目錄權限信息"
    
    # 創建測試文件
    echo "創建測試文件..."
    if ! echo "測試日誌 $(date)" > "$LOGS_DIR/test_log_$(date +%Y%m%d).log" 2>/dev/null; then
        echo "❌ 錯誤: 無法在日誌目錄中創建測試文件"
        echo "請檢查目錄權限："
        ls -ld "$LOGS_DIR"
        echo ""
        echo "請手動執行以下命令："
        echo "  sudo chown -R $(whoami) $LOGS_DIR"
        echo "  chmod -R 777 $LOGS_DIR"
    else
        echo "✅ 測試文件創建成功"
        echo "測試文件列表："
        ls -la "$LOGS_DIR/"
    fi
    
    # 獲取當前日期（只用日期，不用時間）
    CURRENT_DATE=$(date +"%Y%m%d")
    
    # 確保所有腳本有執行權限
    echo "設置腳本執行權限..."
    chmod +x scripts/monitoring/monitor-frontend.sh scripts/monitoring/monitor-system.sh scripts/maintenance/cleanup-docker.sh scripts/diagnostics/fix-frontend.sh scripts/diagnostics/diagnose-frontend.sh scripts/setup/setup-prevention.sh 2>/dev/null || true
    
    # 檢查並安裝 cron 服務
    echo "檢查並安裝 cron 服務..."
    if ! command -v crontab &> /dev/null; then
        echo "cron 未安裝，嘗試安裝..."
        if command -v apt-get &> /dev/null; then
            echo "使用 apt 安裝 cron..."
            apt-get update && apt-get install -y cron
        elif command -v yum &> /dev/null; then
            echo "使用 yum 安裝 cronie..."
            yum install -y cronie
            systemctl enable crond
            systemctl start crond
        else
            echo "⚠️  無法自動安裝 cron 服務，請手動安裝"
            exit 1
        fi
    fi
    
    # 確保 cron 服務正在運行
    if command -v systemctl &> /dev/null; then
        echo "確保 cron 服務正在運行..."
        systemctl enable cron 2>/dev/null || systemctl enable crond 2>/dev/null || true
        systemctl start cron 2>/dev/null || systemctl start crond 2>/dev/null || true
    fi

    # 創建日誌文件（每天只有一個文件，如果已存在則不重新創建）
    echo "🛠️ 正在創建日誌文件..."
    
    # 日誌文件路徑（只用日期）
    FRONTEND_LOG="$LOGS_DIR/frontend-monitor_${CURRENT_DATE}.log"
    SYSTEM_LOG="$LOGS_DIR/system-monitor_${CURRENT_DATE}.log"
    
    # 創建符號鏈接（方便查找最新日誌）
    LATEST_FRONTEND_LOG="$LOGS_DIR/frontend-monitor_latest.log"
    LATEST_SYSTEM_LOG="$LOGS_DIR/system-monitor_latest.log"
    
    # 創建日誌文件（如果不存在才創建，避免重複創建）
    [ -f "$FRONTEND_LOG" ] || touch "$FRONTEND_LOG" 2>/dev/null || {
        echo "❌ 無法創建前端日誌文件"
        exit 1
    }
    [ -f "$SYSTEM_LOG" ] || touch "$SYSTEM_LOG" 2>/dev/null || {
        echo "❌ 無法創建系統日誌文件"
        exit 1
    }
    
    # 創建符號鏈接
    ln -sf "$FRONTEND_LOG" "$LATEST_FRONTEND_LOG"
    ln -sf "$SYSTEM_LOG" "$LATEST_SYSTEM_LOG"
    
    # 設置日誌文件權限
    chmod 666 "$FRONTEND_LOG" "$SYSTEM_LOG" 2>/dev/null || {
        echo "⚠️  無法設置日誌文件權限，但會繼續執行..."
    }
    
    # 清理7天前的日誌文件
    echo "🧹 正在清理7天前的日誌文件..."
    find "$LOGS_DIR" -name "*.log" -type f -mtime +7 -delete 2>/dev/null || {
        echo "⚠️  清理舊日誌時出錯，但會繼續執行..."
    }
    
    # 創建日誌輪轉腳本
    cat > "$PROJECT_DIR/logrotate.sh" << 'EOF'
#!/bin/bash

LOGS_DIR="/root/project/work/logs"
BACKUP_DIR="$LOGS_DIR/backup"
MAX_SIZE=$((100 * 1024 * 1024))  # 100MB in bytes

# 確保備份目錄存在
mkdir -p "$BACKUP_DIR" 2>/dev/null || true

# 檢查日誌文件大小並輪轉（確保每天只有一個文件）
rotate_log() {
    local logfile="$1"
    if [ -f "$logfile" ] && [ $(stat -f%z "$logfile" 2>/dev/null || stat -c%s "$logfile") -gt $MAX_SIZE ]; then
        local current_date=$(date +"%Y%m%d")
        local filename=$(basename "$logfile")
        local backup_file="$BACKUP_DIR/${filename}.backup"
        
        # 將舊文件移動到備份目錄（如果備份文件已存在，則覆蓋）
        mv "$logfile" "$backup_file" 2>/dev/null || true
        
        # 創建新的空文件
        touch "$logfile"
        chmod 666 "$logfile"
        
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] 日誌文件 $filename 已輪轉到 $backup_file"
    fi
}

# 輪轉日誌文件（只處理當天的日誌文件，格式為 *_YYYYMMDD.log）
for log in "$LOGS_DIR"/*.log; do
    if [[ "$log" =~ _[0-9]{8}\.log$ ]] && [[ "$log" != *_latest.log ]] && [[ "$log" != *.backup ]]; then
        # 檢查是否是今天的日誌文件
        local log_date=$(basename "$log" | grep -oE '[0-9]{8}' | head -1)
        local today=$(date +"%Y%m%d")
        if [ "$log_date" = "$today" ]; then
            rotate_log "$log"
        fi
    fi
done

# 清理符號鏈接並重新創建
rm -f "$LOGS_DIR/frontend-monitor_latest.log"
rm -f "$LOGS_DIR/system-monitor_latest.log"
ln -sf "$(ls -t "$LOGS_DIR"/frontend-monitor_*.log 2>/dev/null | grep -v backup | head -1)" "$LOGS_DIR/frontend-monitor_latest.log" 2>/dev/null || true
ln -sf "$(ls -t "$LOGS_DIR"/system-monitor_*.log 2>/dev/null | grep -v backup | head -1)" "$LOGS_DIR/system-monitor_latest.log" 2>/dev/null || true
EOF

    # 設置日誌輪轉腳本權限
    chmod +x "$PROJECT_DIR/logrotate.sh"
    
    echo "✅ 日誌系統已設置完成："
    echo "   - 日誌目錄: $LOGS_DIR"
    echo "   - 前端監控日誌: $FRONTEND_LOG"
    echo "   - 系統監控日誌: $SYSTEM_LOG"
    echo "   - 最新日誌鏈接: $LATEST_FRONTEND_LOG, $LATEST_SYSTEM_LOG"
    echo "   - 日誌輪轉: 自動清理7天前的日誌，單個文件超過100MB時自動輪轉"
    
    echo "✅ 日誌目錄已創建"
    
    # 注意：crontab 任務由 deploy.sh 調用的 setup-prevention.sh 統一管理
    # 這裡不再設置 crontab，避免與 setup-prevention.sh 產生衝突和重複任務
    echo ""
    echo "✅ 日誌系統設置完成！"
    echo ""
    echo "注意：crontab 監控任務將由 deploy.sh 中的 setup-prevention.sh 統一設置"
    echo ""
    
    # 恢復 set -e（如果之前有設置）
    set -e
else
    echo "Error: Cannot find $PROJECT_NAME directory after extraction"
    exit 1
fi
