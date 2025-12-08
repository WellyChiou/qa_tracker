#!/bin/bash

# 前端診斷腳本
# 用於檢查前端容器狀態、文件完整性和系統資源

set -e

# 顏色輸出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=========================================="
echo "前端診斷工具"
echo "==========================================${NC}"
echo ""

# 1. 檢查 Docker 服務狀態
echo -e "${YELLOW}[1] 檢查 Docker 服務狀態...${NC}"
if ! systemctl is-active --quiet docker 2>/dev/null && ! pgrep -x dockerd > /dev/null; then
    echo -e "${RED}❌ Docker 服務未運行${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker 服務正常${NC}"
echo ""

# 2. 檢查容器狀態
echo -e "${YELLOW}[2] 檢查容器狀態...${NC}"
docker compose ps
echo ""

# 3. 檢查前端容器是否運行
echo -e "${YELLOW}[3] 檢查前端容器...${NC}"
FRONTEND_CONTAINER="vue_frontend"
FRONTEND_CHURCH_CONTAINER="vue_frontend_church"

check_container() {
    local container_name=$1
    local container_id=$(docker ps -q -f name=$container_name)
    
    if [ -z "$container_id" ]; then
        echo -e "${RED}❌ 容器 $container_name 未運行${NC}"
        return 1
    fi
    
    echo -e "${GREEN}✅ 容器 $container_name 正在運行 (ID: ${container_id:0:12})${NC}"
    
    # 檢查容器健康狀態
    local health=$(docker inspect --format='{{.State.Health.Status}}' $container_name 2>/dev/null || echo "no-healthcheck")
    if [ "$health" != "no-healthcheck" ]; then
        echo "   健康狀態: $health"
    fi
    
    # 檢查容器重啟次數
    local restart_count=$(docker inspect --format='{{.RestartCount}}' $container_name)
    echo "   重啟次數: $restart_count"
    
    # 檢查容器運行時間
    local uptime=$(docker inspect --format='{{.State.StartedAt}}' $container_name)
    echo "   啟動時間: $uptime"
    
    return 0
}

FRONTEND_OK=false
FRONTEND_CHURCH_OK=false

if check_container $FRONTEND_CONTAINER; then
    FRONTEND_OK=true
fi

if check_container $FRONTEND_CHURCH_CONTAINER; then
    FRONTEND_CHURCH_OK=true
fi

echo ""

# 4. 檢查前端容器內的文件
echo -e "${YELLOW}[4] 檢查前端容器內的文件完整性...${NC}"

check_files() {
    local container_name=$1
    local container_id=$(docker ps -q -f name=$container_name)
    
    if [ -z "$container_id" ]; then
        echo -e "${RED}❌ 容器 $container_name 未運行，無法檢查文件${NC}"
        return 1
    fi
    
    echo "檢查容器: $container_name"
    
    # 檢查 index.html 是否存在
    if docker exec $container_name test -f /usr/share/nginx/html/index.html 2>/dev/null; then
        local file_size=$(docker exec $container_name stat -c%s /usr/share/nginx/html/index.html 2>/dev/null || echo "0")
        if [ "$file_size" -gt 0 ]; then
            echo -e "  ${GREEN}✅ index.html 存在 (大小: ${file_size} bytes)${NC}"
        else
            echo -e "  ${RED}❌ index.html 存在但大小為 0${NC}"
            return 1
        fi
    else
        echo -e "  ${RED}❌ index.html 不存在${NC}"
        return 1
    fi
    
    # 檢查 assets 目錄
    if docker exec $container_name test -d /usr/share/nginx/html/assets 2>/dev/null; then
        local asset_count=$(docker exec $container_name find /usr/share/nginx/html/assets -type f 2>/dev/null | wc -l)
        echo -e "  ${GREEN}✅ assets 目錄存在 (包含 $asset_count 個文件)${NC}"
        
        if [ "$asset_count" -eq 0 ]; then
            echo -e "  ${RED}⚠️  警告: assets 目錄為空${NC}"
            return 1
        fi
    else
        echo -e "  ${RED}❌ assets 目錄不存在${NC}"
        return 1
    fi
    
    # 檢查 nginx 配置
    if docker exec $container_name test -f /etc/nginx/conf.d/default.conf 2>/dev/null; then
        echo -e "  ${GREEN}✅ nginx 配置文件存在${NC}"
    else
        echo -e "  ${RED}❌ nginx 配置文件不存在${NC}"
        return 1
    fi
    
    return 0
}

FILES_OK=true
if ! check_files $FRONTEND_CONTAINER; then
    FILES_OK=false
fi

if ! check_files $FRONTEND_CHURCH_CONTAINER; then
    FILES_OK=false
fi

echo ""

# 5. 檢查磁盤空間
echo -e "${YELLOW}[5] 檢查磁盤空間...${NC}"
df -h / | tail -1 | awk '{print "   總空間: " $2 ", 已用: " $3 " (" $5 "), 可用: " $4}'
DISK_USAGE=$(df / | tail -1 | awk '{print $5}' | sed 's/%//')
if [ "$DISK_USAGE" -gt 90 ]; then
    echo -e "  ${RED}⚠️  警告: 磁盤使用率超過 90%${NC}"
elif [ "$DISK_USAGE" -gt 80 ]; then
    echo -e "  ${YELLOW}⚠️  警告: 磁盤使用率超過 80%${NC}"
else
    echo -e "  ${GREEN}✅ 磁盤空間充足${NC}"
fi
echo ""

# 6. 檢查 Docker 磁盤使用
echo -e "${YELLOW}[6] 檢查 Docker 磁盤使用...${NC}"
docker system df
echo ""

# 7. 檢查容器日誌（最近 20 行）
echo -e "${YELLOW}[7] 檢查前端容器日誌（最近 20 行）...${NC}"
if [ "$FRONTEND_OK" = true ]; then
    echo -e "${BLUE}--- $FRONTEND_CONTAINER 日誌 ---${NC}"
    docker logs --tail 20 $FRONTEND_CONTAINER 2>&1 | head -20 || echo "無法讀取日誌"
    echo ""
fi

if [ "$FRONTEND_CHURCH_OK" = true ]; then
    echo -e "${BLUE}--- $FRONTEND_CHURCH_CONTAINER 日誌 ---${NC}"
    docker logs --tail 20 $FRONTEND_CHURCH_CONTAINER 2>&1 | head -20 || echo "無法讀取日誌"
    echo ""
fi

# 8. 檢查 Nginx 代理容器
echo -e "${YELLOW}[8] 檢查 Nginx 代理容器...${NC}"
NGINX_CONTAINER="nginx_proxy"
if check_container $NGINX_CONTAINER; then
    # 測試 nginx 配置
    if docker exec $NGINX_CONTAINER nginx -t 2>/dev/null; then
        echo -e "  ${GREEN}✅ Nginx 配置正確${NC}"
    else
        echo -e "  ${RED}❌ Nginx 配置有誤${NC}"
    fi
fi
echo ""

# 9. 測試前端可訪問性
echo -e "${YELLOW}[9] 測試前端可訪問性...${NC}"
if [ "$FRONTEND_OK" = true ]; then
    if docker exec $FRONTEND_CONTAINER wget -q -O - http://localhost/ 2>/dev/null | grep -q "<!DOCTYPE html\|<html"; then
        echo -e "  ${GREEN}✅ $FRONTEND_CONTAINER 可以正常響應${NC}"
    else
        echo -e "  ${RED}❌ $FRONTEND_CONTAINER 無法正常響應${NC}"
    fi
fi

if [ "$FRONTEND_CHURCH_OK" = true ]; then
    if docker exec $FRONTEND_CHURCH_CONTAINER wget -q -O - http://localhost/ 2>/dev/null | grep -q "<!DOCTYPE html\|<html"; then
        echo -e "  ${GREEN}✅ $FRONTEND_CHURCH_CONTAINER 可以正常響應${NC}"
    else
        echo -e "  ${RED}❌ $FRONTEND_CHURCH_CONTAINER 無法正常響應${NC}"
    fi
fi
echo ""

# 總結
echo -e "${BLUE}=========================================="
echo "診斷總結"
echo "==========================================${NC}"

ISSUES=0

if [ "$FRONTEND_OK" = false ]; then
    echo -e "${RED}❌ 前端容器未運行${NC}"
    ISSUES=$((ISSUES + 1))
fi

if [ "$FRONTEND_CHURCH_OK" = false ]; then
    echo -e "${RED}❌ 教會前端容器未運行${NC}"
    ISSUES=$((ISSUES + 1))
fi

if [ "$FILES_OK" = false ]; then
    echo -e "${RED}❌ 前端容器內文件不完整${NC}"
    ISSUES=$((ISSUES + 1))
fi

if [ "$ISSUES" -eq 0 ]; then
    echo -e "${GREEN}✅ 所有檢查通過，前端服務正常${NC}"
    exit 0
else
    echo -e "${RED}❌ 發現 $ISSUES 個問題${NC}"
    echo ""
    echo "建議執行修復腳本："
    echo "  ./fix-frontend.sh"
    exit 1
fi

