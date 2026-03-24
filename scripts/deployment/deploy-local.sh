#!/usr/bin/env bash
set -e

# =====================================================
# deploy_local.sh
#
# Local Docker deployment helper for this project
#
# USAGE:
#   ./deploy_local.sh [action] [mode]
#
# ACTIONS:
#   (no arg) | normal
#       - Default mode
#       - Use when you only changed application code
#         (frontend .js/.vue/.css, backend .java/.yml)
#       - Will NOT rebuild images
#
#   build
#       - Use when dependencies changed
#         (package.json, package-lock.json, pom.xml)
#       - Will rebuild images using Docker cache
#
#   clean
#       - Use when Dockerfile or base image changed
#       - Or when Docker cache is broken
#       - Will rebuild images WITHOUT cache
#
#   down | stop | restart
#       - 常用容器控制動作
#
# MODES:
#   all (default)
#       - 原本整包行為（personal + church + invest）
#
#   invest
#       - invest-only 行為（只使用 docker-compose.invest.yml）
#
# EXAMPLES:
#   ./deploy_local.sh
#   ./deploy_local.sh normal
#   ./deploy_local.sh build
#   ./deploy_local.sh build invest
#   ./deploy_local.sh down invest
#   ./deploy_local.sh clean
#
# =====================================================

# -----------------------------------------------------
# Help option
# -----------------------------------------------------
if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  sed -n '2,60p' "$0"
  exit 0
fi

ACTION="${1:-normal}"
MODE="${2:-all}"
PROJECT_NAME="docker-vue-java-mysql"
INVEST_COMPOSE_FILE="docker-compose.invest.yml"

# 向後相容：若第一參數直接傳 invest/all，視為 normal + mode
if [[ "$ACTION" == "invest" || "$ACTION" == "all" ]]; then
  MODE="$ACTION"
  ACTION="normal"
fi

if [[ "$MODE" != "all" && "$MODE" != "invest" ]]; then
  echo "❌ Unknown mode: $MODE"
  echo "Available modes: all, invest"
  exit 1
fi

if [[ "$MODE" == "invest" && ! -f "$INVEST_COMPOSE_FILE" ]]; then
  echo "❌ 找不到 $INVEST_COMPOSE_FILE"
  exit 1
fi

COMPOSE_ARGS=()
if [[ "$MODE" == "invest" ]]; then
  # invest-only 允許使用獨立 env 檔覆蓋 secrets/開關
  if [[ -f ".env.invest.prod" ]]; then
    COMPOSE_ARGS+=(--env-file .env.invest.prod)
  fi
  COMPOSE_ARGS+=(-f "$INVEST_COMPOSE_FILE")
fi

dc() {
  docker compose "${COMPOSE_ARGS[@]}" "$@"
}

echo "=========================================="
echo "🚀 Local Deploy Script"
echo "Project : $PROJECT_NAME"
echo "Action  : $ACTION"
echo "Mode    : $MODE"
echo "=========================================="
echo ""

# -----------------------------------------------------
# 1. Environment checks
# -----------------------------------------------------
echo "🔍 Checking environment..."

if ! command -v docker >/dev/null 2>&1; then
  echo "❌ Docker not found"
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "❌ docker compose not available"
  exit 1
fi

echo "✅ Docker & Docker Compose OK"
echo ""

# -----------------------------------------------------
# 2. Execute by action
# -----------------------------------------------------
case "$ACTION" in
  normal)
    echo "▶ Action: NORMAL"
    echo "▶ Only start containers (no rebuild)"
    echo "▶ Compose mode: $MODE"
    echo ""
    dc up -d
    ;;

  build)
    echo "▶ Action: BUILD"
    echo "▶ Rebuild images using cache"
    echo "▶ Compose mode: $MODE"
    echo ""
    dc build
    dc up -d
    ;;

  clean)
    echo "▶ Action: CLEAN"
    echo "▶ Rebuild images without cache"
    echo "▶ Compose mode: $MODE"
    echo ""
    dc down
    dc build --no-cache
    dc up -d
    ;;

  down)
    echo "▶ Action: DOWN"
    echo "▶ Compose mode: $MODE"
    echo ""
    dc down
    ;;

  stop)
    echo "▶ Action: STOP"
    echo "▶ Compose mode: $MODE"
    echo ""
    dc stop
    ;;

  restart)
    echo "▶ Action: RESTART"
    echo "▶ Compose mode: $MODE"
    echo ""
    dc restart
    ;;

  *)
    echo "❌ Unknown action: $ACTION"
    echo ""
    echo "Available actions:"
    echo "  normal  (default)"
    echo "  build"
    echo "  clean"
    echo "  down"
    echo "  stop"
    echo "  restart"
    echo ""
    echo "Run './deploy_local.sh --help' for details"
    exit 1
    ;;
esac

# down / stop 不需要做啟動後檢查
if [[ "$ACTION" == "down" || "$ACTION" == "stop" ]]; then
  echo ""
  echo "✅ Local deploy completed successfully"
  exit 0
fi

# -----------------------------------------------------
# 3. Wait for containers to be healthy
# -----------------------------------------------------
echo ""
echo "⏳ 等待容器完全啟動..."

# 檢查前端容器是否就緒的函數
check_frontend_ready() {
  local service_name=$1
  
  # 檢查容器是否運行
  if ! dc ps | grep -q "${service_name}.*Up"; then
    return 1
  fi
  
  # 檢查容器內的 index.html 是否存在（表示構建完成且檔案已就緒）
  if dc exec -T "${service_name}" test -f /usr/share/nginx/html/index.html 2>/dev/null; then
    return 0
  fi
  
  return 1
}

# 等待所有前端容器完全啟動（最多等待 60 秒）
MAX_WAIT=90
WAIT_COUNT=0
FRONTENDS_READY=0

echo "檢查前端容器狀態..."

FRONTEND_SERVICES=()
if [[ "$MODE" == "invest" ]]; then
  FRONTEND_SERVICES=("frontend-invest-admin")
else
  FRONTEND_SERVICES=("frontend-personal" "frontend-church" "frontend-church-admin")
fi
TOTAL_FRONTENDS=${#FRONTEND_SERVICES[@]}

while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
  FRONTENDS_READY=0
  
  for svc in "${FRONTEND_SERVICES[@]}"; do
    if check_frontend_ready "$svc"; then
      FRONTENDS_READY=$((FRONTENDS_READY + 1))
    fi
  done
  
  if [ $FRONTENDS_READY -eq $TOTAL_FRONTENDS ]; then
    echo ""
    echo "✅ 所有前端容器檔案已就緒，等待服務完全啟動..."
    # 額外等待 3 秒確保容器內的 nginx 服務完全啟動
    sleep 3
    echo "✅ 所有前端容器已完全啟動"
    break
  fi
  
  sleep 1
  WAIT_COUNT=$((WAIT_COUNT + 1))
  if [ $((WAIT_COUNT % 5)) -eq 0 ]; then
    echo -n " [${FRONTENDS_READY}/${TOTAL_FRONTENDS}]"
  else
    echo -n "."
  fi
done
echo ""

if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
  echo "⚠️  前端容器啟動超時（${FRONTENDS_READY}/${TOTAL_FRONTENDS} 就緒），但部署繼續進行"
  echo "   如果遇到 404 錯誤，請稍候片刻後重新整理頁面"
fi

# 等待 Nginx 容器啟動並重載配置
echo "⏳ 等待 Nginx 容器啟動..."
sleep 3

# 檢查 nginx 容器是否運行
if dc ps | grep -q "nginx.*Up\|nginx_proxy.*Up"; then
  echo "🔄 重載 Nginx 配置以確保路由正確..."
  # 等待一下確保 nginx 完全啟動
  sleep 2
  dc exec -T nginx nginx -s reload 2>/dev/null || {
    echo "⚠️  Nginx 重載失敗，將重啟容器..."
    dc restart nginx
    sleep 3
  }
  echo "✅ Nginx 配置已更新"
else
  echo "⚠️  Nginx 容器未運行，請檢查日誌"
fi

# -----------------------------------------------------
# 4. Show container status
# -----------------------------------------------------
echo ""
echo "📦 Container status:"
dc ps

echo ""
echo "✅ Local deploy completed successfully"
echo ""
echo "💡 提示：如果遇到 404 錯誤，請嘗試："
echo "   1. 清除瀏覽器快取（Ctrl+Shift+R 或 Cmd+Shift+R）"
echo "   2. 等待 10-20 秒後重新整理頁面"
if [[ "$MODE" == "invest" ]]; then
  echo "   3. 檢查容器日誌：docker compose -f docker-compose.invest.yml logs frontend-invest-admin backend-invest nginx"
else
  echo "   3. 檢查容器日誌：docker compose logs frontend-church-admin"
fi
