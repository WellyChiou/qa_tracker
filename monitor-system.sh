#!/bin/bash

# 系統資源監控腳本
# 監控磁盤空間、Docker 資源使用情況，並在需要時自動清理

set -e

# 配置
LOG_FILE="/var/log/system-monitor.log"
DISK_WARNING_THRESHOLD=80  # 磁盤使用率警告閾值（%）
DISK_CRITICAL_THRESHOLD=90  # 磁盤使用率嚴重警告閾值（%）
DOCKER_WARNING_THRESHOLD=5  # Docker 未使用資源大小警告閾值（GB）

# 確保日誌目錄存在
mkdir -p "$(dirname "$LOG_FILE")" 2>/dev/null || true

# 日誌函數
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log "開始系統資源監控檢查..."

# 1. 檢查磁盤空間
log "檢查磁盤空間..."
DISK_USAGE=$(df / | tail -1 | awk '{print $5}' | sed 's/%//')
DISK_AVAILABLE=$(df -h / | tail -1 | awk '{print $4}')

if [ "$DISK_USAGE" -ge "$DISK_CRITICAL_THRESHOLD" ]; then
    log "❌ 嚴重警告: 磁盤使用率 ${DISK_USAGE}%，可用空間僅剩 ${DISK_AVAILABLE}"
    log "立即執行 Docker 資源清理..."
    
    # 自動清理 Docker 資源
    cd "$(dirname "$0")" || exit 1
    if [ -f "./cleanup-docker.sh" ]; then
        # 執行自動清理（只清理未使用的資源，不會刪除正在使用的映像）
        docker system prune -f >> "$LOG_FILE" 2>&1
        docker image prune -f >> "$LOG_FILE" 2>&1
        log "✅ Docker 資源清理完成（已保留正在使用的映像）"
    fi
    
    # 再次檢查磁盤空間
    DISK_USAGE_AFTER=$(df / | tail -1 | awk '{print $5}' | sed 's/%//')
    if [ "$DISK_USAGE_AFTER" -ge "$DISK_CRITICAL_THRESHOLD" ]; then
        log "⚠️  警告: 清理後磁盤使用率仍為 ${DISK_USAGE_AFTER}%，請手動檢查"
    else
        log "✅ 清理後磁盤使用率降至 ${DISK_USAGE_AFTER}%"
    fi
elif [ "$DISK_USAGE" -ge "$DISK_WARNING_THRESHOLD" ]; then
    log "⚠️  警告: 磁盤使用率 ${DISK_USAGE}%，可用空間 ${DISK_AVAILABLE}"
    log "建議執行清理: ./cleanup-docker.sh"
else
    log "✅ 磁盤空間充足: 使用率 ${DISK_USAGE}%，可用空間 ${DISK_AVAILABLE}"
fi

# 2. 檢查 Docker 資源使用
log "檢查 Docker 資源使用..."
DOCKER_INFO=$(docker system df --format "table {{.Type}}\t{{.TotalCount}}\t{{.Size}}" 2>/dev/null || echo "")

if [ -n "$DOCKER_INFO" ]; then
    # 檢查未使用的資源大小
    UNUSED_SIZE=$(docker system df | grep -E "Images|Containers|Local Volumes" | awk '{sum+=$3} END {print sum}' 2>/dev/null || echo "0")
    
    # 簡化檢查：如果 Docker 總使用超過一定大小，建議清理
    DOCKER_TOTAL=$(docker system df | tail -1 | awk '{print $3}' | sed 's/[^0-9.]//g' || echo "0")
    
    if [ -n "$DOCKER_TOTAL" ] && [ "$(echo "$DOCKER_TOTAL > $DOCKER_WARNING_THRESHOLD" | bc 2>/dev/null || echo 0)" -eq 1 ]; then
        log "⚠️  警告: Docker 資源使用較大，建議執行清理"
    else
        log "✅ Docker 資源使用正常"
    fi
fi

# 3. 檢查容器狀態
log "檢查容器狀態..."
cd "$(dirname "$0")" || exit 1
if [ -f "docker-compose.yml" ]; then
    # 檢查是否有容器異常退出
    EXITED_CONTAINERS=$(docker ps -a --filter "status=exited" --format "{{.Names}}" 2>/dev/null | grep -E "vue_frontend|vue_frontend_church" || true)
    
    if [ -n "$EXITED_CONTAINERS" ]; then
        log "⚠️  警告: 發現異常退出的前端容器: $EXITED_CONTAINERS"
        log "嘗試自動重啟..."
        docker compose restart frontend frontend-church 2>/dev/null || true
        sleep 5
        
        # 檢查重啟後狀態
        STILL_EXITED=$(docker ps -a --filter "status=exited" --format "{{.Names}}" 2>/dev/null | grep -E "vue_frontend|vue_frontend_church" || true)
        if [ -n "$STILL_EXITED" ]; then
            log "❌ 容器重啟失敗，可能需要重建"
        else
            log "✅ 容器已成功重啟"
        fi
    else
        log "✅ 所有容器運行正常"
    fi
fi

log "系統資源監控檢查完成"
exit 0

