#!/bin/bash

# 前端修復腳本
# 當前端出現問題時，自動重建前端容器

set -e

# 顏色輸出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 可指定單一 service 修復：frontend-personal / frontend-church / frontend-church-admin
TARGET_SERVICE="${1:-all}"

FRONTEND_SERVICES=("frontend-personal" "frontend-church" "frontend-church-admin")

service_to_container() {
  case "$1" in
    frontend-personal) echo "vue_personal" ;;
    frontend-church) echo "vue_frontend_church" ;;
    frontend-church-admin) echo "vue_frontend_church_admin" ;;
    *) echo "" ;;
  esac
}

# 決定這次要修哪一些 service
if [ "$TARGET_SERVICE" != "all" ]; then
  FRONTEND_SERVICES=("$TARGET_SERVICE")
fi

echo -e "${BLUE}=========================================="
echo "前端自動修復工具"
echo "==========================================${NC}"
echo ""

# 檢查是否在正確的目錄
if [ ! -f "docker-compose.yml" ]; then
  echo -e "${RED}❌ 錯誤: 找不到 docker-compose.yml${NC}"
  echo "請在專案根目錄執行此腳本"
  exit 1
fi

# 1. 停止前端容器
echo -e "${YELLOW}[1] 停止前端容器...${NC}"
docker compose stop "${FRONTEND_SERVICES[@]}" 2>/dev/null || true
echo -e "${GREEN}✅ 前端容器已停止${NC}"
echo ""

# 2. 刪除前端容器（保留映像）
echo -e "${YELLOW}[2] 刪除前端容器...${NC}"
docker compose rm -f "${FRONTEND_SERVICES[@]}" 2>/dev/null || true
echo -e "${GREEN}✅ 前端容器已刪除${NC}"
echo ""

# 3. 清理舊的前端映像（保留你原本互動功能；非互動則自動跳過）
echo -e "${YELLOW}[3] 清理舊的前端映像（釋放空間）...${NC}"

DO_CLEAN_IMAGES="n"
if [ -t 0 ]; then
  read -p "是否清理舊的前端映像？這可以釋放磁盤空間 (y/n): " -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    DO_CLEAN_IMAGES="y"
  fi
else
  echo "（非互動環境，跳過清理映像）"
fi

if [ "$DO_CLEAN_IMAGES" = "y" ]; then
  docker image prune -f || true
  docker images | grep -E "frontend|vue" | grep -v "REPOSITORY" | awk '{print $3}' | xargs -r docker rmi -f 2>/dev/null || true
  echo -e "${GREEN}✅ 舊映像已清理${NC}"
else
  echo "跳過清理映像"
fi
echo ""

# 4. 重建並啟動前端容器
echo -e "${YELLOW}[4] 重建並啟動前端容器...${NC}"
echo "這可能需要幾分鐘時間，請耐心等待..."
docker compose up -d --build "${FRONTEND_SERVICES[@]}"
echo -e "${GREEN}✅ 前端容器重建命令完成${NC}"
echo ""

# 5. 等待容器啟動
echo -e "${YELLOW}[5] 等待容器啟動...${NC}"
sleep 10

# 6. 檢查容器狀態
echo -e "${YELLOW}[6] 檢查容器狀態...${NC}"
docker compose ps "${FRONTEND_SERVICES[@]}" || true
echo ""

# 7. 驗證文件完整性（保留你原本 verify_container，但容器名對齊）
echo -e "${YELLOW}[7] 驗證文件完整性...${NC}"

verify_container() {
  local container_name=$1
  local container_id
  container_id=$(docker ps -q -f name=$container_name)

  if [ -z "$container_id" ]; then
    echo -e "  ${RED}❌ 容器 $container_name 未運行${NC}"
    return 1
  fi

  sleep 3

  if docker exec "$container_name" test -f /usr/share/nginx/html/index.html 2>/dev/null; then
    local file_size
    file_size=$(docker exec "$container_name" stat -c%s /usr/share/nginx/html/index.html 2>/dev/null || echo "0")
    if [ "$file_size" -gt 0 ]; then
      echo -e "  ${GREEN}✅ $container_name: index.html 正常 (${file_size} bytes)${NC}"
    else
      echo -e "  ${RED}❌ $container_name: index.html 大小為 0${NC}"
      return 1
    fi
  else
    echo -e "  ${RED}❌ $container_name: index.html 不存在${NC}"
    return 1
  fi

  local asset_count
  asset_count=$(docker exec "$container_name" find /usr/share/nginx/html/assets -type f 2>/dev/null | wc -l)
  if [ "$asset_count" -gt 0 ]; then
    echo -e "  ${GREEN}✅ $container_name: assets 目錄正常 (${asset_count} 個文件)${NC}"
  else
    echo -e "  ${YELLOW}⚠️  $container_name: assets 目錄為空或不存在${NC}"
  fi

  return 0
}

ALL_VERIFIED=true
for SVC in "${FRONTEND_SERVICES[@]}"; do
  CNAME="$(service_to_container "$SVC")"
  if [ -z "$CNAME" ]; then
    echo -e "  ${RED}❌ 未知 service: $SVC${NC}"
    ALL_VERIFIED=false
    continue
  fi
  if ! verify_container "$CNAME"; then
    ALL_VERIFIED=false
  fi
done

echo ""

# 8. 重啟 Nginx 代理（保留你原本）
echo -e "${YELLOW}[8] 重啟 Nginx 代理...${NC}"
docker compose restart nginx 2>/dev/null || true
sleep 3
echo -e "${GREEN}✅ Nginx 代理已重啟${NC}"
echo ""

# 總結
echo -e "${BLUE}=========================================="
echo "修復完成"
echo "==========================================${NC}"

if [ "$ALL_VERIFIED" = true ]; then
  echo -e "${GREEN}✅ 前端容器已成功修復並驗證${NC}"
  echo ""
  echo "如果問題仍然存在，請執行診斷腳本："
  echo "  ./diagnose-frontend.sh"
  exit 0
else
  echo -e "${YELLOW}⚠️  部分容器可能仍有問題${NC}"
  echo ""
  echo "建議執行診斷腳本進行詳細檢查："
  echo "  ./diagnose-frontend.sh"
  exit 1
fi
