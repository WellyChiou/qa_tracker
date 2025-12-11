#!/bin/bash

# 設置預防機制腳本
# 自動設置監控、清理等預防措施

set -e

# 顏色輸出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=========================================="
echo "設置前端白屏預防機制"
echo "==========================================${NC}"
echo ""

# 檢查是否為 root 用戶
if [ "$EUID" -ne 0 ]; then 
    echo -e "${YELLOW}⚠️  建議使用 root 權限執行此腳本${NC}"
    read -p "是否繼續？(y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# 獲取專案目錄
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$SCRIPT_DIR"

echo -e "${YELLOW}[1] 檢查必要腳本...${NC}"

REQUIRED_SCRIPTS=(
    "monitor-frontend.sh"
    "monitor-system.sh"
    "cleanup-docker.sh"
    "fix-frontend.sh"
    "diagnose-frontend.sh"
)

for script in "${REQUIRED_SCRIPTS[@]}"; do
    if [ ! -f "$PROJECT_DIR/$script" ]; then
        echo -e "${RED}❌ 找不到腳本: $script${NC}"
        exit 1
    fi
    
    # 確保腳本有執行權限
    chmod +x "$PROJECT_DIR/$script"
done

echo -e "${GREEN}✅ 所有必要腳本已就緒${NC}"
echo ""

# 2. 設置前端監控（每 5 分鐘）
echo -e "${YELLOW}[2] 設置前端監控（每 5 分鐘檢查一次）...${NC}"

CRON_FRONTEND="*/5 * * * * cd $PROJECT_DIR && $PROJECT_DIR/monitor-frontend.sh"

# 移除現有的前端監控任務（如果存在）
temp_cron=$(mktemp)
crontab -l 2>/dev/null | grep -v "monitor-frontend.sh" | grep -v "^#" > "$temp_cron"

# 添加新的前端監控任務
echo "$CRON_FRONTEND" >> "$temp_cron"

# 安裝新的 crontab
crontab "$temp_cron"
rm -f "$temp_cron"

echo -e "${GREEN}✅ 前端監控任務已更新${NC}"
echo ""

# 3. 設置系統資源監控（每小時）
echo -e "${YELLOW}[3] 設置系統資源監控（每小時檢查一次）...${NC}"

CRON_SYSTEM="0 * * * * cd $PROJECT_DIR && $PROJECT_DIR/monitor-system.sh"

# 移除現有的系統監控任務（如果存在）
temp_cron=$(mktemp)
crontab -l 2>/dev/null | grep -v "monitor-system.sh" | grep -v "^#" > "$temp_cron"

# 添加新的系統監控任務
echo "$CRON_SYSTEM" >> "$temp_cron"

# 安裝新的 crontab
crontab "$temp_cron"
rm -f "$temp_cron"

echo -e "${GREEN}✅ 系統監控任務已更新${NC}"
echo ""

# 4. 設置定期 Docker 清理（每天凌晨 2 點）
# 注意：只清理未使用的資源，不會刪除正在使用的映像和容器
echo -e "${YELLOW}[4] 設置定期 Docker 清理（每天凌晨 2 點）...${NC}"

CRON_CLEANUP="0 2 * * * cd $PROJECT_DIR && docker system prune -f && docker image prune -f"

# 移除現有的 Docker 清理任務（如果存在）
temp_cron=$(mktemp)
crontab -l 2>/dev/null | grep -v "docker system prune" | grep -v "^#" > "$temp_cron"

# 添加新的 Docker 清理任務
echo "$CRON_CLEANUP" >> "$temp_cron"

# 安裝新的 crontab
crontab "$temp_cron"
rm -f "$temp_cron"

echo -e "${GREEN}✅ Docker 清理任務已更新${NC}"
echo ""

# 5. 創建日誌目錄和日誌管理腳本
echo -e "${YELLOW}[5] 設置日誌管理...${NC}"

# 創建日誌目錄
LOG_DIR="/root/project/work/logs"
mkdir -p "$LOG_DIR" 2>/dev/null || true

# 創建日誌管理腳本
cat > "$PROJECT_DIR/manage-logs.sh" << 'EOL'
#!/bin/bash

LOG_DIR="/root/project/work/logs"
MAX_SIZE=$((100*1024*1024))  # 100MB
RETENTION_DAYS=7
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# 確保日誌目錄存在
mkdir -p "$LOG_DIR"

# 1. 檢查並切割過大的日誌文件
find "$LOG_DIR" -name "*.log" -type f | while read -r logfile; do
    # 檢查文件大小
    if [ -f "$logfile" ] && [ $(stat -f%z "$logfile" 2>/dev/null || stat -c%s "$logfile" 2>/dev/null) -gt $MAX_SIZE ]; then
        # 獲取不帶路徑的文件名
        filename=$(basename "$logfile")
        # 移動並重命名當前日誌文件，添加時間戳
        mv "$logfile" "${logfile%.*}_${TIMESTAMP}.log"
        # 創建新的空日誌文件
        touch "$logfile"
        chmod 666 "$logfile" 2>/dev/null || true
        echo "$(date '+%Y-%m-%d %H:%M:%S') - 日誌文件 $filename 超過 100MB，已重命名為 ${filename%.*}_${TIMESTAMP}.log" >> "$LOG_DIR/log-rotation.log"
    fi
done

# 2. 刪除7天前的日誌文件
find "$LOG_DIR" -name "*.log" -type f -mtime +$RETENTION_DAYS -delete 2>/dev/null || true

echo "$(date '+%Y-%m-%d %H:%M:%S') - 日誌清理完成" >> "$LOG_DIR/log-rotation.log"
EOL

# 設置執行權限
chmod +x "$PROJECT_DIR/manage-logs.sh"

# 創建日誌文件並設置權限
CURRENT_TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
touch "$LOG_DIR/frontend-monitor_${CURRENT_TIMESTAMP}.log"
touch "$LOG_DIR/system-monitor_${CURRENT_TIMESTAMP}.log"
touch "$LOG_DIR/log-rotation.log"
chmod 666 "$LOG_DIR"/*.log 2>/dev/null || true

# 更新監控腳本中的日誌路徑
sed -i.bak "s|/var/log/frontend-monitor.log|$LOG_DIR/frontend-monitor_$(date +\%Y\%m\%d_\%H\%M\%S).log|g" "$PROJECT_DIR/monitor-frontend.sh"
sed -i.bak "s|/var/log/system-monitor.log|$LOG_DIR/system-monitor_$(date +\%Y\%m\%d_\%H\%M\%S).log|g" "$PROJECT_DIR/monitor-system.sh"

# 添加日誌管理定時任務（每小時執行一次）
CRON_LOG_MANAGEMENT="0 * * * * $PROJECT_DIR/manage-logs.sh"
temp_cron=$(mktemp)
crontab -l 2>/dev/null | grep -v "manage-logs.sh" | grep -v "^#" > "$temp_cron"
echo "$CRON_LOG_MANAGEMENT" >> "$temp_cron"
crontab "$temp_cron"
rm -f "$temp_cron"

echo -e "${GREEN}✅ 日誌管理系統已設置${NC}"
echo -e "   - 日誌目錄: $LOG_DIR"
echo -e "   - 自動清理: 7天前的日誌"
echo -e "   - 日誌切割: 當文件大於100MB時"
echo -e "   - 日誌輪轉: 每小時檢查一次"
echo ""

# 6. 顯示當前 crontab
echo -e "${YELLOW}[6] 當前設置的定時任務：${NC}"
{
    echo "# 系統監控任務 (由 $PROJECT_NAME 管理)"
    crontab -l 2>/dev/null | grep -E "monitor|cleanup|docker|manage-logs" | grep -v "^#"
    echo ""
    echo "# 其他系統定時任務"
    crontab -l 2>/dev/null | grep -vE "monitor|cleanup|docker|manage-logs|^#" || echo "（無其他任務）"
} | grep -v "^$" || echo "（無定時任務）"
echo ""

# 7. 測試腳本
echo -e "${YELLOW}[7] 測試監控腳本...${NC}"
read -p "是否現在執行一次前端監控測試？(y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "執行前端監控測試..."
    cd "$PROJECT_DIR"
    bash monitor-frontend.sh
fi
echo ""

echo -e "${BLUE}=========================================="
echo "✅ 預防機制設置完成！"
echo "==========================================${NC}"
echo ""
echo "已更新的監控任務："
echo "  - 前端監控: 每 5 分鐘檢查一次（已更新）"
echo "  - 系統資源監控: 每小時檢查一次（已更新）"
echo "  - Docker 清理: 每天凌晨 2 點執行（已更新）"
echo ""
echo "查看日誌："
echo "  tail -f /var/log/frontend-monitor.log"
echo "  tail -f /var/log/system-monitor.log"
echo ""
echo "手動執行："
echo "  ./monitor-frontend.sh    # 前端監控"
echo "  ./monitor-system.sh      # 系統監控"
echo "  ./cleanup-docker.sh      # Docker 清理"
echo "  ./diagnose-frontend.sh   # 前端診斷"
echo "  ./fix-frontend.sh        # 前端修復"
echo ""
echo "查看定時任務："
echo "  crontab -l"
echo ""

