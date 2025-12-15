#!/bin/bash
# 立即修復 cron：只要執行一次，就會把監控/清理/刪 log 的任務都補回來
set -e
bash ./setup-prevention.unified.sh
