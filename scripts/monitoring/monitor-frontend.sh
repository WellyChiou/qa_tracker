#!/bin/bash

# 前端監控腳本
# 定期檢查前端狀態，發現問題時自動修復
# 建議通過 cron 定期執行（例如每 5 分鐘）

set -e

# 獲取腳本所在目錄
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"

# 配置（保持你原本寫法）
LOG_DIR="/root/project/work/logs"
LOG_FILE="$LOG_DIR/frontend-monitor_$(date +'%Y%m%d').log"
DIAGNOSE_SCRIPT="$PROJECT_ROOT/diagnose-frontend.sh"
FIX_SCRIPT="$PROJECT_ROOT/fix-frontend.sh"
MAX_RETRIES=3

# 三個前端（對齊 docker-compose.yml）
FRONTEND_SERVICES=("frontend-personal" "frontend-church" "frontend-church-admin")

# 確保日誌目錄存在
mkdir -p "$LOG_DIR" 2>/dev/null || true
chmod 777 "$LOG_DIR" 2>/dev/null || true

# 日誌函數
log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log "開始前端監控檢查..."

# 切換到項目根目錄
cd "$PROJECT_ROOT" || {
  log "錯誤: 無法切換到項目根目錄: $PROJECT_ROOT"
  exit 1
}

# 檢查是否在正確的目錄
if [ ! -f "docker-compose.yml" ]; then
  log "錯誤: 在 $PROJECT_ROOT 中找不到 docker-compose.yml"
  exit 1
fi

# 檢查診斷腳本是否存在
if [ ! -f "$DIAGNOSE_SCRIPT" ]; then
  log "警告: 找不到診斷腳本 $DIAGNOSE_SCRIPT"
  log "跳過自動修復"
  exit 0
fi

# 先跑一次「全量診斷」（保留你原本行為：一次看完整體狀況）
log "執行診斷檢查（全量）..."
if bash "$DIAGNOSE_SCRIPT" >> "$LOG_FILE" 2>&1; then
  log "✅ 前端服務正常，無需修復"
  exit 0
fi

# 診斷發現問題，開始針對每個 service 嘗試修復
log "⚠️  診斷發現問題，開始自動修復（逐一前端）..."

if [ ! -f "$FIX_SCRIPT" ]; then
  log "錯誤: 找不到修復腳本 $FIX_SCRIPT"
  exit 1
fi

for SVC in "${FRONTEND_SERVICES[@]}"; do
  log "------ 目標前端：$SVC ------"

  # 如果該 service 診斷是 OK，就跳過修復
  if bash "$DIAGNOSE_SCRIPT" "$SVC" >> "$LOG_FILE" 2>&1; then
    log "✅ $SVC 正常，跳過修復"
    continue
  fi

  # 執行修復（最多重試）
  RETRY_COUNT=0
  while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    log "嘗試修復 $SVC (第 $((RETRY_COUNT + 1)) 次)..."

    if bash "$FIX_SCRIPT" "$SVC" >> "$LOG_FILE" 2>&1; then
      log "✅ 修復命令執行成功：$SVC"

      # 再次驗證
      sleep 5
      if bash "$DIAGNOSE_SCRIPT" "$SVC" >> "$LOG_FILE" 2>&1; then
        log "✅ 修復驗證成功：$SVC"
        break
      else
        log "⚠️  修復後驗證失敗：$SVC（可能需要手動檢查）"
      fi

      break
    else
      RETRY_COUNT=$((RETRY_COUNT + 1))
      if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
        log "修復失敗：$SVC，等待 30 秒後重試..."
        sleep 30
      fi
    fi
  done

  if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    log "❌ 修復失敗：$SVC，已達最大重試次數 ($MAX_RETRIES)"
    log "請手動檢查系統狀態"
    exit 1
  fi
done

# 最後再跑一次全量驗證（跟你原本「修完再驗證」的精神一致）:contentReference[oaicite:3]{index=3}
log "執行診斷檢查（最終全量驗證）..."
if bash "$DIAGNOSE_SCRIPT" >> "$LOG_FILE" 2>&1; then
  log "✅ 最終驗證成功，前端服務已恢復正常"
  exit 0
else
  log "⚠️  最終驗證仍失敗，可能需要手動檢查"
  exit 1
fi

