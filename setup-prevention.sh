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

# 檢查是否已存在
if crontab -l 2>/dev/null | grep -q "monitor-frontend.sh"; then
    echo -e "${YELLOW}⚠️  前端監控任務已存在，跳過${NC}"
else
    (crontab -l 2>/dev/null; echo "$CRON_FRONTEND") | crontab -
    echo -e "${GREEN}✅ 前端監控任務已添加${NC}"
fi
echo ""

# 3. 設置系統資源監控（每小時）
echo -e "${YELLOW}[3] 設置系統資源監控（每小時檢查一次）...${NC}"

CRON_SYSTEM="0 * * * * cd $PROJECT_DIR && $PROJECT_DIR/monitor-system.sh"

# 檢查是否已存在
if crontab -l 2>/dev/null | grep -q "monitor-system.sh"; then
    echo -e "${YELLOW}⚠️  系統監控任務已存在，跳過${NC}"
else
    (crontab -l 2>/dev/null; echo "$CRON_SYSTEM") | crontab -
    echo -e "${GREEN}✅ 系統監控任務已添加${NC}"
fi
echo ""

# 4. 設置定期 Docker 清理（每天凌晨 2 點）
echo -e "${YELLOW}[4] 設置定期 Docker 清理（每天凌晨 2 點）...${NC}"

CRON_CLEANUP="0 2 * * * cd $PROJECT_DIR && docker system prune -f && docker image prune -a -f"

# 檢查是否已存在
if crontab -l 2>/dev/null | grep -q "docker system prune"; then
    echo -e "${YELLOW}⚠️  Docker 清理任務已存在，跳過${NC}"
else
    (crontab -l 2>/dev/null; echo "$CRON_CLEANUP") | crontab -
    echo -e "${GREEN}✅ Docker 清理任務已添加${NC}"
fi
echo ""

# 5. 創建日誌目錄
echo -e "${YELLOW}[5] 創建日誌目錄...${NC}"
mkdir -p /var/log 2>/dev/null || true
touch /var/log/frontend-monitor.log 2>/dev/null || true
touch /var/log/system-monitor.log 2>/dev/null || true
chmod 666 /var/log/frontend-monitor.log /var/log/system-monitor.log 2>/dev/null || true
echo -e "${GREEN}✅ 日誌目錄已創建${NC}"
echo ""

# 6. 顯示當前 crontab
echo -e "${YELLOW}[6] 當前設置的定時任務：${NC}"
crontab -l 2>/dev/null | grep -E "monitor|cleanup|docker" || echo "（無相關任務）"
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
echo "已設置的監控任務："
echo "  - 前端監控: 每 5 分鐘檢查一次"
echo "  - 系統資源監控: 每小時檢查一次"
echo "  - Docker 清理: 每天凌晨 2 點執行"
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

