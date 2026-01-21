#!/bin/bash
# 設置預防機制（unified / 不覆蓋其他 cron 任務）
# 會在 crontab 中建立一段「專案管理區塊」，確保每次部署都能穩定更新而不洗掉其他任務

set -e

REMOTE_PATH="/root/project/work"
PROJECT_NAME="docker-vue-java-mysql"
PROJECT_DIR="${REMOTE_PATH}/${PROJECT_NAME}"
LOG_DIR="${REMOTE_PATH}/logs"

mkdir -p "${LOG_DIR}" 2>/dev/null || true
chmod 777 "${LOG_DIR}" 2>/dev/null || true

CRON_BEGIN="# BEGIN project-work (managed)"
CRON_END="# END project-work (managed)"

# 你要的任務（可依需要調整）
CRON_FRONTEND="*/5 * * * * cd ${PROJECT_DIR} && bash ${PROJECT_DIR}/monitor-frontend.sh >> ${LOG_DIR}/frontend-monitor_latest.log 2>&1"
CRON_SYSTEM="0 * * * * cd ${PROJECT_DIR} && bash ${PROJECT_DIR}/monitor-system.sh >> ${LOG_DIR}/system-monitor_latest.log 2>&1"
CRON_CLEANUP="0 2 * * * cd ${PROJECT_DIR} && bash ${PROJECT_DIR}/cleanup-docker.sh >> ${LOG_DIR}/cleanup-docker_latest.log 2>&1"
CRON_LOGCLEAN="0 3 * * * find ${LOG_DIR} -name \"*.log\" -type f -mtime +7 -delete"

# 讀取現有 crontab（沒有也不會失敗）
EXISTING="$(crontab -l 2>/dev/null || true)"

# 移除舊的 managed 區塊（若存在）
FILTERED="$(printf "%s\n" "$EXISTING" | sed "/^${CRON_BEGIN}$/,/^${CRON_END}$/d")"

# 重新寫回：保留其他任務 + 更新 managed 區塊
{
  printf "%s\n" "$FILTERED" | sed '/^\s*$/d'
  echo "${CRON_BEGIN}"
  echo "${CRON_FRONTEND}"
  echo "${CRON_SYSTEM}"
  echo "${CRON_CLEANUP}"
  echo "${CRON_LOGCLEAN}"
  echo "${CRON_END}"
} | crontab -

echo "✅ 已更新 crontab（不覆蓋其他任務）"
echo
echo "📋 目前 crontab："
crontab -l
