#!/bin/bash

# Docker 資源清理腳本
# 定期清理未使用的 Docker 資源，避免磁盤空間不足

set -e

# 顏色輸出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=========================================="
echo "Docker 資源清理工具"
echo "==========================================${NC}"
echo ""

# 顯示清理前的狀態
echo -e "${YELLOW}[1] 清理前的 Docker 資源使用情況：${NC}"
docker system df
echo ""

# 顯示磁盤使用情況
echo -e "${YELLOW}[2] 磁盤使用情況：${NC}"
df -h / | tail -1
echo ""

# 詢問清理選項
echo -e "${YELLOW}選擇清理選項：${NC}"
echo "1) 清理未使用的容器、網路、映像（安全，推薦）"
echo "2) 清理未使用的映像（包括 dangling 映像）"
echo "3) 清理所有未使用的資源（包括 volume，⚠️ 危險）"
echo "4) 僅清理構建緩存"
echo "5) 全部清理（最徹底，但最安全）"
echo ""
read -p "請選擇 (1-5，默認 1): " choice
choice=${choice:-1}

case $choice in
    1)
        echo -e "${YELLOW}[3] 清理未使用的容器、網路、映像...${NC}"
        docker system prune -f
        ;;
    2)
        echo -e "${YELLOW}[3] 清理未使用的映像...${NC}"
        docker image prune -a -f
        ;;
    3)
        echo -e "${RED}⚠️  警告: 這將清理所有未使用的資源，包括 volume！${NC}"
        read -p "確認繼續？(y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker system prune -a --volumes -f
        else
            echo "已取消"
            exit 0
        fi
        ;;
    4)
        echo -e "${YELLOW}[3] 清理構建緩存...${NC}"
        docker builder prune -f
        ;;
    5)
        echo -e "${YELLOW}[3] 執行全部清理...${NC}"
        # 清理未使用的容器、網路、映像
        docker system prune -f
        # 清理未使用的映像（包括 dangling）
        docker image prune -a -f
        # 清理構建緩存
        docker builder prune -f
        ;;
    *)
        echo -e "${RED}❌ 無效的選擇${NC}"
        exit 1
        ;;
esac

echo ""

# 顯示清理後的狀態
echo -e "${YELLOW}[4] 清理後的 Docker 資源使用情況：${NC}"
docker system df
echo ""

# 顯示清理後的磁盤使用情況
echo -e "${YELLOW}[5] 清理後的磁盤使用情況：${NC}"
df -h / | tail -1
echo ""

echo -e "${GREEN}✅ 清理完成！${NC}"

