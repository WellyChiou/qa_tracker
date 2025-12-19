#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
node scripts/apply_ui_bonus_v2.js
if [ -f package-lock.json ]; then npm ci; else npm install; fi
npm run build
echo "✅ build ok"
