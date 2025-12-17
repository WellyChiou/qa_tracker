#!/usr/bin/env bash
set -e

# =====================================================
# verify_local.sh
# Quick local sanity checks for docker-compose stack
#
# Usage:
#   ./verify_local.sh
# =====================================================

echo "=========================================="
echo "🧪 Local Verify"
echo "=========================================="

# 1) Docker checks
if ! command -v docker >/dev/null 2>&1; then
  echo "❌ Docker not found"
  exit 1
fi
if ! docker compose version >/dev/null 2>&1; then
  echo "❌ docker compose not available"
  exit 1
fi
echo "✅ Docker & docker compose OK"

# 2) Compose containers
echo ""
echo "📦 docker compose ps"
docker compose ps || true

# 3) Basic health status summary
echo ""
echo "❤️  Container health summary"
docker ps --format 'table {{.Names}}\t{{.Status}}' || true

# 4) Optional: check common ports on localhost (only if exposed)
# If you don't expose ports locally, these will be skipped / not fail.
check_port() {
  local port="$1"
  if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "✅ Port $port is listening"
  else
    echo "ℹ️  Port $port not listening (may be normal if not exposed)"
  fi
}

echo ""
echo "🔌 Local ports (best-effort)"
check_port 80
check_port 443
check_port 8080
check_port 3306

# 5) Quick backend reachability (best-effort)
# Only if backend is exposed or nginx routes to it locally.
echo ""
echo "🌐 HTTP checks (best-effort)"
if command -v curl >/dev/null 2>&1; then
  if curl -fsS http://localhost/ >/dev/null 2>&1; then
    echo "✅ http://localhost/ reachable"
  else
    echo "ℹ️  http://localhost/ not reachable (may be normal if not exposed)"
  fi
else
  echo "ℹ️  curl not found on host, skipping HTTP checks"
fi

echo ""
echo "✅ verify_local finished"
