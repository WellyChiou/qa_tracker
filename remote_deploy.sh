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

    # 設置日誌目錄
    LOGS_DIR="/root/project/work/logs"
    echo "正在創建日誌目錄: $LOGS_DIR"

    # 創建日誌目錄（使用 sudo 確保有足夠權限）
    if ! sudo mkdir -p "$LOGS_DIR" 2>/dev/null; then
        echo "❌ 錯誤: 無法創建日誌目錄: $LOGS_DIR"
        echo "嘗試使用當前用戶創建..."
        mkdir -p "$LOGS_DIR" 2>/dev/null || {
            echo "❌ 嚴重錯誤: 無法創建日誌目錄，請手動執行以下命令："
            echo "  sudo mkdir -p $LOGS_DIR"
            echo "  sudo chown -R $(whoami) /root/project/work/"
            exit 1
        }
    fi
    
    # 設置正確的權限
    chmod -R 777 "$LOGS_DIR" 2>/dev/null || {
        echo "⚠️  警告: 無法設置日誌目錄權限，但將繼續執行..."
    }
    
    # 檢查目錄是否真的存在
    if [ ! -d "$LOGS_DIR" ]; then
        echo "❌ 錯誤: 日誌目錄創建失敗: $LOGS_DIR"
        echo "請手動執行以下命令："
        echo "  sudo mkdir -p $LOGS_DIR"
        echo "  sudo chown -R $(whoami) $LOGS_DIR"
        echo "  chmod -R 777 $LOGS_DIR"
        exit 1
    fi
    
    echo "✅ 日誌目錄已創建: $LOGS_DIR"
    echo "當前用戶: $(whoami)"
    echo "目錄權限:"
    ls -ld "$LOGS_DIR" 2>/dev/null || echo "無法獲取目錄權限信息"
    
    # 創建測試文件
    echo "創建測試文件..."
    if ! echo "測試日誌 $(date)" > "$LOGS_DIR/test_log_$(date +%Y%m%d_%H%M%S).log" 2>/dev/null; then
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
    
    # 獲取當前時間戳
    TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
    
    # 確保所有腳本有執行權限
    echo "設置腳本執行權限..."
    chmod +x monitor-frontend.sh monitor-system.sh cleanup-docker.sh fix-frontend.sh diagnose-frontend.sh setup-prevention.sh 2>/dev/null || true
    
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
            echo "在 Debian/Ubuntu 上: apt-get install -y cron"
            echo "在 CentOS/RHEL 上: yum install -y cronie && systemctl enable --now crond"
            exit 1
        fi
    fi
    
    # 確保 cron 服務正在運行
    if command -v systemctl &> /dev/null; then
        echo "確保 cron 服務正在運行..."
        systemctl enable cron 2>/dev/null || systemctl enable crond 2>/dev/null || true
        systemctl start cron 2>/dev/null || systemctl start crond 2>/dev/null || true
    fi
    
    # 創建帶時間戳的日誌文件
    echo "🛠️ 正在創建日誌文件..."
    
    # 日誌文件路徑（帶時間戳）
    FRONTEND_LOG="$LOGS_DIR/frontend-monitor_${TIMESTAMP}.log"
    SYSTEM_LOG="$LOGS_DIR/system-monitor_${TIMESTAMP}.log"
    
    # 創建符號鏈接（方便查找最新日誌）
    LATEST_FRONTEND_LOG="$LOGS_DIR/frontend-monitor_latest.log"
    LATEST_SYSTEM_LOG="$LOGS_DIR/system-monitor_latest.log"
    
    # 創建日誌文件
    touch "$FRONTEND_LOG" "$SYSTEM_LOG" 2>/dev/null || {
        echo "❌ 無法創建日誌文件"
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
MAX_SIZE=$((100 * 1024 * 1024))  # 100MB in bytes

# 檢查日誌文件大小並輪轉
rotate_log() {
    local logfile="$1"
    if [ -f "$logfile" ] && [ $(stat -f%z "$logfile" 2>/dev/null || stat -c%s "$logfile") -gt $MAX_SIZE ]; then
        local timestamp=$(date +"%Y%m%d_%H%M%S")
        mv "$logfile" "${logfile%.*}_${timestamp}.log"
        touch "$logfile"
        chmod 666 "$logfile"
    fi
}

# 輪轉日誌文件
for log in "$LOGS_DIR"/*.log; do
    if [[ "$log" != *"_"*.log ]]; then  # 跳過已經帶時間戳的文件
        rotate_log "$log"
    fi
done

# 清理符號鏈接並重新創建
rm -f "$LOGS_DIR/frontend-monitor_latest.log"
rm -f "$LOGS_DIR/system-monitor_latest.log"
ln -sf "$(ls -t "$LOGS_DIR"/frontend-monitor_*.log 2>/dev/null | head -1)" "$LOGS_DIR/frontend-monitor_latest.log" 2>/dev/null || true
ln -sf "$(ls -t "$LOGS_DIR"/system-monitor_*.log 2>/dev/null | head -1)" "$LOGS_DIR/system-monitor_latest.log" 2>/dev/null || true
EOF

    # 設置日誌輪轉腳本權限
    chmod +x "$PROJECT_DIR/logrotate.sh"
    
    echo "✅ 日誌系統已設置完成："
    echo "   - 日誌目錄: $LOGS_DIR"
    echo "   - 前端監控日誌: $FRONTEND_LOG"
    echo "   - 系統監控日誌: $SYSTEM_LOG"
    echo "   - 最新日誌鏈接: $LATEST_FRONTEND_LOG, $LATEST_SYSTEM_LOG"
    echo "   - 日誌輪轉: 自動清理7天前的日誌，單個文件超過100MB時自動輪轉"
    
    # 函數：更新或添加 cron 任務
    update_cron_job() {
        local job_name="$1"
        local schedule="$2"
        local command="$3"
        local temp_cron=$(mktemp)
        
        # 導出現有 cron 任務到臨時文件
        crontab -l 2>/dev/null | grep -v "$job_name" > "$temp_cron"
        
        # 添加新任務
        echo "$schedule $command" >> "$temp_cron"
        
        # 安裝更新後的 crontab
        crontab "$temp_cron"
        rm -f "$temp_cron"
        
        echo "✅ 已更新任務: $job_name"
    }
    
    # 設置前端監控（每 5 分鐘）
    echo "設置/更新前端監控（每 5 分鐘檢查一次）..."
    CRON_FRONTEND_CMD="cd $PROJECT_DIR && $PROJECT_DIR/monitor-frontend.sh >> $FRONTEND_LOG 2>&1"
    update_cron_job "frontend-monitor" "*/5 * * * *" "$CRON_FRONTEND_CMD"
    
    # 設置系統資源監控（每小時）
    echo "設置/更新系統資源監控（每小時檢查一次）..."
    CRON_SYSTEM_CMD="cd $PROJECT_DIR && $PROJECT_DIR/monitor-system.sh >> $SYSTEM_LOG 2>&1"
    update_cron_job "system-monitor" "0 * * * *" "$CRON_SYSTEM_CMD"
    
    # 設置定期 Docker 清理（每天凌晨 2 點）
    echo "設置/更新定期 Docker 清理（每天凌晨 2 點）..."
    CRON_CLEANUP_CMD="cd $PROJECT_DIR && docker system prune -f && docker image prune -f"
    update_cron_job "docker-cleanup" "0 2 * * *" "$CRON_CLEANUP_CMD"
    
    # 設置日誌輪轉（每小時檢查一次）
    echo "設置/更新日誌輪轉（每小時檢查一次）..."
    CRON_LOGROTATE_CMD="cd $PROJECT_DIR && $PROJECT_DIR/logrotate.sh"
    update_cron_job "log-rotate" "0 * * * *" "$CRON_LOGROTATE_CMD"
    
    # 設置日誌清理（每天凌晨 3 點）
    echo "設置/更新日誌清理（每天凌晨 3 點）..."
    CRON_LOGCLEAN_CMD="find $LOGS_DIR -name \"*.log\" -type f -mtime +7 -delete"
    update_cron_job "log-cleanup" "0 3 * * *" "$CRON_LOGCLEAN_CMD"
    
    # 顯示當前所有定時任務
    echo "\n📋 當前設定的定時任務："
    crontab -l 2>/dev/null || echo "(沒有定時任務)"
    
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
