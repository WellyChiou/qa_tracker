#!/usr/bin/env bash
set -e

# =====================================================
# deploy_local.sh
#
# Local Docker deployment helper for this project
#
# USAGE:
#   ./deploy_local.sh [mode]
#
# MODES:
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
# EXAMPLES:
#   ./deploy_local.sh
#   ./deploy_local.sh normal
#   ./deploy_local.sh build
#   ./deploy_local.sh clean
#
# =====================================================

# -----------------------------------------------------
# Help option
# -----------------------------------------------------
if [[ "$1" == "-h" || "$1" == "--help" ]]; then
  sed -n '2,60p' "$0"
  exit 0
fi

MODE="${1:-normal}"
PROJECT_NAME="docker-vue-java-mysql"

echo "=========================================="
echo "🚀 Local Deploy Script"
echo "Project : $PROJECT_NAME"
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
# 2. Execute by mode
# -----------------------------------------------------
case "$MODE" in
  normal)
    echo "▶ Mode: NORMAL"
    echo "▶ Only start containers (no rebuild)"
    echo "▶ Suitable for daily development"
    echo ""
    docker compose up -d
    
    # 等待前端容器完全啟動
    echo ""
    echo "⏳ 等待前端容器啟動..."
    sleep 3
    
    # 檢查前端容器是否正常運行
    if docker compose ps | grep -q "frontend-church-admin.*Up"; then
      echo "✅ 前端容器已啟動"
    else
      echo "⚠️  前端容器可能還在啟動中，請稍候..."
    fi
    ;;

  build)
    echo "▶ Mode: BUILD"
    echo "▶ Rebuild images using cache"
    echo "▶ Use when dependencies changed"
    echo ""
    docker compose build
    docker compose up -d
    
    # 等待前端容器完全啟動
    echo ""
    echo "⏳ 等待前端容器啟動..."
    sleep 5
    
    # 檢查前端容器是否正常運行
    if docker compose ps | grep -q "frontend-church-admin.*Up"; then
      echo "✅ 前端容器已啟動"
    else
      echo "⚠️  前端容器可能還在啟動中，請稍候..."
    fi
    ;;

  clean)
    echo "▶ Mode: CLEAN"
    echo "▶ Rebuild images without cache"
    echo "▶ Use when Dockerfile or base image changed"
    echo ""
    docker compose down
    docker compose build --no-cache
    docker compose up -d
    
    # 等待前端容器完全啟動
    echo ""
    echo "⏳ 等待前端容器啟動..."
    sleep 5
    
    # 檢查前端容器是否正常運行
    if docker compose ps | grep -q "frontend-church-admin.*Up"; then
      echo "✅ 前端容器已啟動"
    else
      echo "⚠️  前端容器可能還在啟動中，請稍候..."
    fi
    ;;

  *)
    echo "❌ Unknown mode: $MODE"
    echo ""
    echo "Available modes:"
    echo "  normal  (default)"
    echo "  build"
    echo "  clean"
    echo ""
    echo "Run './deploy_local.sh --help' for details"
    exit 1
    ;;
esac

# -----------------------------------------------------
# 3. Wait for containers to be healthy
# -----------------------------------------------------
echo ""
echo "⏳ 等待容器完全啟動..."

# 等待前端容器完全啟動（最多等待 30 秒）
MAX_WAIT=30
WAIT_COUNT=0
while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
  if docker compose ps | grep -q "frontend-church-admin.*Up"; then
    # 檢查容器是否真的可以訪問
    if docker compose exec -T frontend-church-admin wget -q --spider http://localhost/ 2>/dev/null || \
       docker compose exec -T frontend-church-admin test -f /usr/share/nginx/html/index.html 2>/dev/null; then
      echo "✅ 前端容器已完全啟動"
      break
    fi
  fi
  sleep 1
  WAIT_COUNT=$((WAIT_COUNT + 1))
  echo -n "."
done
echo ""

if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
  echo "⚠️  前端容器啟動超時，但部署繼續進行"
  echo "   如果遇到 404 錯誤，請稍候片刻後重新整理頁面"
fi

# 等待 Nginx 容器啟動
sleep 2

# -----------------------------------------------------
# 4. Show container status
# -----------------------------------------------------
echo ""
echo "📦 Container status:"
docker compose ps

echo ""
echo "✅ Local deploy completed successfully"
echo ""
echo "💡 提示：如果遇到 404 錯誤，請嘗試："
echo "   1. 清除瀏覽器快取（Ctrl+Shift+R 或 Cmd+Shift+R）"
echo "   2. 等待 10-20 秒後重新整理頁面"
echo "   3. 檢查容器日誌：docker compose logs frontend-church-admin"
