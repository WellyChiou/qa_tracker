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
    ;;

  build)
    echo "▶ Mode: BUILD"
    echo "▶ Rebuild images using cache"
    echo "▶ Use when dependencies changed"
    echo ""
    docker compose build
    docker compose up -d
    ;;

  clean)
    echo "▶ Mode: CLEAN"
    echo "▶ Rebuild images without cache"
    echo "▶ Use when Dockerfile or base image changed"
    echo ""
    docker compose down
    docker compose build --no-cache
    docker compose up -d
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
# 3. Show container status
# -----------------------------------------------------
echo ""
echo "📦 Container status:"
docker compose ps

echo ""
echo "✅ Local deploy completed successfully"
