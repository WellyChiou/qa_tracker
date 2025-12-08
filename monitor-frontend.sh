#!/bin/bash

# 前端監控腳本
# 定期檢查前端狀態，發現問題時自動修復
# 建議通過 cron 定期執行（例如每 5 分鐘）

set -e

# 配置
LOG_FILE="/var/log/frontend-monitor.log"
DIAGNOSE_SCRIPT="./diagnose-frontend.sh"
FIX_SCRIPT="./fix-frontend.sh"
MAX_RETRIES=3

# 確保日誌目錄存在
mkdir -p "$(dirname "$LOG_FILE")" 2>/dev/null || true

# 日誌函數
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log "開始前端監控檢查..."

# 檢查是否在正確的目錄
if [ ! -f "docker-compose.yml" ]; then
    log "錯誤: 找不到 docker-compose.yml，請在專案根目錄執行"
    exit 1
fi

# 檢查診斷腳本是否存在
if [ ! -f "$DIAGNOSE_SCRIPT" ]; then
    log "警告: 找不到診斷腳本 $DIAGNOSE_SCRIPT"
    log "跳過自動修復"
    exit 0
fi

# 執行診斷
log "執行診斷檢查..."
if bash "$DIAGNOSE_SCRIPT" >> "$LOG_FILE" 2>&1; then
    log "✅ 前端服務正常，無需修復"
    exit 0
fi

# 診斷發現問題，執行修復
log "⚠️  診斷發現問題，開始自動修復..."

if [ ! -f "$FIX_SCRIPT" ]; then
    log "錯誤: 找不到修復腳本 $FIX_SCRIPT"
    exit 1
fi

# 執行修復
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    log "嘗試修復 (第 $((RETRY_COUNT + 1)) 次)..."
    
    if bash "$FIX_SCRIPT" >> "$LOG_FILE" 2>&1; then
        log "✅ 修復成功"
        
        # 再次驗證
        sleep 5
        if bash "$DIAGNOSE_SCRIPT" >> "$LOG_FILE" 2>&1; then
            log "✅ 修復驗證成功，前端服務已恢復正常"
            exit 0
        else
            log "⚠️  修復後驗證失敗，可能需要手動檢查"
        fi
        break
    else
        RETRY_COUNT=$((RETRY_COUNT + 1))
        if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
            log "修復失敗，等待 30 秒後重試..."
            sleep 30
        fi
    fi
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    log "❌ 修復失敗，已達到最大重試次數 ($MAX_RETRIES)"
    log "請手動檢查系統狀態"
    exit 1
fi

exit 0

