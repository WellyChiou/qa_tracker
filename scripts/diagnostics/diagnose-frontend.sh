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

# 允許指定單一 service：frontend-personal / frontend-church / frontend-church-admin
TARGET_SERVICE="${1:-all}"

# 對齊 docker-compose.yml 的 service -> container_name :contentReference[oaicite:5]{index=5}
service_to_container() {
  case "$1" in
    frontend-personal) echo "vue_personal" ;;
    frontend-church) echo "vue_frontend_church" ;;
    frontend-church-admin) echo "vue_frontend_church_admin" ;;
    *) echo "" ;;
  esac
}

FRONTEND_SERVICES=("frontend-personal" "frontend-church" "frontend-church-admin")

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

# 2. 檢查容器狀態（全量模式才需要）
if [ "$TARGET_SERVICE" = "all" ]; then
  echo -e "${YELLOW}[2] 檢查容器狀態...${NC}"
  docker compose ps
  echo ""
fi

# 通用：檢查容器是否運行（保留你原本輸出資訊）
check_container() {
  local container_name=$1
  local container_id
  container_id=$(docker ps -q -f name=$container_name)

  if [ -z "$container_id" ]; then
    echo -e "${RED}❌ 容器 $container_name 未運行${NC}"
    return 1
  fi

  echo -e "${GREEN}✅ 容器 $container_name 正在運行 (ID: ${container_id:0:12})${NC}"

  local health
  health=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "no-healthcheck")
  if [ "$health" != "no-healthcheck" ]; then
    echo "   健康狀態: $health"
  fi

  local restart_count
  restart_count=$(docker inspect --format='{{.RestartCount}}' "$container_name")
  echo "   重啟次數: $restart_count"

  local uptime
  uptime=$(docker inspect --format='{{.State.StartedAt}}' "$container_name")
  echo "   啟動時間: $uptime"

  return 0
}

# 3. 檢查前端容器是否運行（整合成迴圈）
echo -e "${YELLOW}[3] 檢查前端容器...${NC}"

declare -A OK_MAP
for SVC in "${FRONTEND_SERVICES[@]}"; do
  if [ "$TARGET_SERVICE" != "all" ] && [ "$TARGET_SERVICE" != "$SVC" ]; then
    continue
  fi

  CNAME="$(service_to_container "$SVC")"
  if [ -z "$CNAME" ]; then
    echo -e "${RED}❌ 未知 service: $SVC${NC}"
    OK_MAP["$SVC"]=false
    continue
  fi

  if check_container "$CNAME"; then
    OK_MAP["$SVC"]=true
  else
    OK_MAP["$SVC"]=false
  fi

  echo ""
done

# 4. 檢查前端容器內的文件（整合成迴圈）
echo -e "${YELLOW}[4] 檢查前端容器內的文件完整性...${NC}"

check_files() {
  local container_name=$1
  local container_id
  container_id=$(docker ps -q -f name=$container_name)

  if [ -z "$container_id" ]; then
    echo -e "${RED}❌ 容器 $container_name 未運行，無法檢查文件${NC}"
    return 1
  fi

  echo "檢查容器: $container_name"

  # index.html
  if docker exec "$container_name" test -f /usr/share/nginx/html/index.html 2>/dev/null; then
    local file_size
    file_size=$(docker exec "$container_name" stat -c%s /usr/share/nginx/html/index.html 2>/dev/null || echo "0")
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

  # assets
  if docker exec "$container_name" test -d /usr/share/nginx/html/assets 2>/dev/null; then
    local asset_count
    asset_count=$(docker exec "$container_name" find /usr/share/nginx/html/assets -type f 2>/dev/null | wc -l)
    echo -e "  ${GREEN}✅ assets 目錄存在 (包含 $asset_count 個文件)${NC}"
    if [ "$asset_count" -eq 0 ]; then
      echo -e "  ${RED}⚠️  警告: assets 目錄為空${NC}"
      return 1
    fi
  else
    echo -e "  ${RED}❌ assets 目錄不存在${NC}"
    return 1
  fi

  # nginx conf
  if docker exec "$container_name" test -f /etc/nginx/conf.d/default.conf 2>/dev/null; then
    echo -e "  ${GREEN}✅ nginx 配置文件存在${NC}"
  else
    echo -e "  ${RED}❌ nginx 配置文件不存在${NC}"
    return 1
  fi

  return 0
}

FILES_OK=true
for SVC in "${FRONTEND_SERVICES[@]}"; do
  if [ "$TARGET_SERVICE" != "all" ] && [ "$TARGET_SERVICE" != "$SVC" ]; then
    continue
  fi

  if [ "${OK_MAP[$SVC]}" = "true" ]; then
    CNAME="$(service_to_container "$SVC")"
    if ! check_files "$CNAME"; then
      FILES_OK=false
    fi
    echo ""
  fi
done

# 5~6（磁碟 / docker df）只在全量模式跑，避免 monitor 逐一修復時重複刷
if [ "$TARGET_SERVICE" = "all" ]; then
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

  echo -e "${YELLOW}[6] 檢查 Docker 磁盤使用...${NC}"
  docker system df
  echo ""
fi

# 7. 檢查容器日誌（最近 20 行）—保留，但整合成迴圈
echo -e "${YELLOW}[7] 檢查前端容器日誌（最近 20 行）...${NC}"
for SVC in "${FRONTEND_SERVICES[@]}"; do
  if [ "$TARGET_SERVICE" != "all" ] && [ "$TARGET_SERVICE" != "$SVC" ]; then
    continue
  fi
  if [ "${OK_MAP[$SVC]}" = "true" ]; then
    CNAME="$(service_to_container "$SVC")"
    echo -e "${BLUE}--- $CNAME 日誌 ---${NC}"
    docker logs --tail 20 "$CNAME" 2>&1 | head -20 || echo "無法讀取日誌"
    echo ""
  fi
done

# 8. 檢查 Nginx 代理容器（保留原本）
echo -e "${YELLOW}[8] 檢查 Nginx 代理容器...${NC}"
NGINX_CONTAINER="nginx_proxy"
if check_container "$NGINX_CONTAINER"; then
  if docker exec "$NGINX_CONTAINER" nginx -t 2>/dev/null; then
    echo -e "  ${GREEN}✅ Nginx 配置正確${NC}"
  else
    echo -e "  ${RED}❌ Nginx 配置有誤${NC}"
  fi
fi
echo ""

# 9. 測試前端可訪問性（保留，但整合成迴圈）
echo -e "${YELLOW}[9] 測試前端可訪問性...${NC}"
for SVC in "${FRONTEND_SERVICES[@]}"; do
  if [ "$TARGET_SERVICE" != "all" ] && [ "$TARGET_SERVICE" != "$SVC" ]; then
    continue
  fi
  if [ "${OK_MAP[$SVC]}" = "true" ]; then
    CNAME="$(service_to_container "$SVC")"
    if docker exec "$CNAME" sh -c 'exec 3<>/dev/tcp/127.0.0.1/80 && echo -e "GET / HTTP/1.0\r\n\r\n" >&3 && head -n 1 <&3' \
      | grep -q "200"; then
      echo -e "  ${GREEN}✅ $CNAME 可以正常響應${NC}"
    else
      echo -e "  ${RED}❌ $CNAME 無法正常響應${NC}"
    fi

  fi
done
echo ""

# 總結（保留原本精神，但會包含三個前端）
echo -e "${BLUE}=========================================="
echo "診斷總結"
echo "==========================================${NC}"

ISSUES=0
for SVC in "${FRONTEND_SERVICES[@]}"; do
  if [ "$TARGET_SERVICE" != "all" ] && [ "$TARGET_SERVICE" != "$SVC" ]; then
    continue
  fi
  if [ "${OK_MAP[$SVC]}" != "true" ]; then
    echo -e "${RED}❌ $SVC 容器未運行或異常${NC}"
    ISSUES=$((ISSUES + 1))
  fi
done

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
