#!/usr/bin/env bash
set -euo pipefail

# ==========================================
# 一鍵部署腳本 (Mac/Linux 版本 - 與 Windows 版同步)
# 功能：打包專案 -> 上傳到伺服器 -> 由遠端 remote_deploy.sh 負責解壓/部署/預防機制
#
# ✅ 同步原則（Single Source of Truth）：
# - Mac/Linux 端只負責：打包 + 上傳 + 觸發 remote_deploy.sh
# - 真正部署流程只放在：remote_deploy.sh（遠端執行）
# ==========================================

# ====== 配置區（需要改就改這裡）======
SERVER_IP="38.54.89.136"
SERVER_USER="root"
# 下面這行可留空（建議用 SSH Key）；若你有 sshpass 也可填密碼
SERVER_PASSWORD=""   # 例如：SERVER_PASSWORD="your_password"
REMOTE_PATH="/root/project/work"
PROJECT_NAME="docker-vue-java-mysql"
ARCHIVE_NAME="${PROJECT_NAME}.tar.gz"
REMOTE_DEPLOY_LOCAL="./remote_deploy.sh"   # 本機專案中的 remote_deploy.sh
REMOTE_DEPLOY_REMOTE="/tmp/remote_deploy.sh"
# =====================================

# ====== 安全/一致性選項 ======
SSH_OPTS="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"

# ====== 工具檢查 ======
need_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "❌ 缺少必要指令: $1"
    exit 1
  }
}

need_cmd tar
need_cmd ssh
need_cmd scp

# sshpass 非必須：有就可用密碼免輸入，沒有就走互動輸入或 SSH key
HAS_SSHPASS=0
if command -v sshpass >/dev/null 2>&1; then
  HAS_SSHPASS=1
fi

ssh_run() {
  if [[ -n "${SERVER_PASSWORD}" && "${HAS_SSHPASS}" -eq 1 ]]; then
    sshpass -p "${SERVER_PASSWORD}" ssh ${SSH_OPTS} "${SERVER_USER}@${SERVER_IP}" "$@"
  else
    ssh ${SSH_OPTS} "${SERVER_USER}@${SERVER_IP}" "$@"
  fi
}

scp_put() {
  # scp_put <local> <remote_path_or_file>
  local src="$1"
  local dst="$2"
  if [[ -n "${SERVER_PASSWORD}" && "${HAS_SSHPASS}" -eq 1 ]]; then
    sshpass -p "${SERVER_PASSWORD}" scp ${SSH_OPTS} "$src" "${SERVER_USER}@${SERVER_IP}:$dst"
  else
    scp ${SSH_OPTS} "$src" "${SERVER_USER}@${SERVER_IP}:$dst"
  fi
}

echo "=========================================="
echo "🚀 開始一鍵部署（Mac/Linux - 同步版）"
echo "=========================================="
echo "Server: ${SERVER_USER}@${SERVER_IP}"
echo "Remote Path: ${REMOTE_PATH}"
echo "Project: ${PROJECT_NAME}"
echo

# ====== 取得腳本位置 / 專案根 ======
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${SCRIPT_DIR}"              # 預設腳本放在專案根
PARENT_DIR="$(cd "${PROJECT_ROOT}/.." && pwd)"

if [[ ! -f "${PROJECT_ROOT}/docker-compose.yml" ]]; then
  echo "❌ 找不到 docker-compose.yml。請把本腳本放在專案根目錄執行。"
  echo "   目前: ${PROJECT_ROOT}"
  exit 1
fi

if [[ ! -f "${PROJECT_ROOT}/remote_deploy.sh" ]]; then
  echo "❌ 找不到 ${PROJECT_ROOT}/remote_deploy.sh"
  echo "   這是同步部署的核心（遠端部署入口），請確認檔案存在。"
  exit 1
fi

# ====== Step 1: 打包（與 Windows 排除策略對齊 + 更保險）======
echo "Step 1/4: 打包專案..."
cd "${PARENT_DIR}"

# 清除舊壓縮檔
if [[ -f "${ARCHIVE_NAME}" ]]; then
  rm -f "${ARCHIVE_NAME}"
fi

# 使用 ustar 讓 Linux 端解壓更穩
# 排除：VCS、node_modules、target、log、dist、tar.gz、自家系統檔
tar --format=ustar -czf "${ARCHIVE_NAME}" \
  --exclude="${PROJECT_NAME}/.git" \
  --exclude="${PROJECT_NAME}/node_modules" \
  --exclude="${PROJECT_NAME}/**/node_modules" \
  --exclude="${PROJECT_NAME}/target" \
  --exclude="${PROJECT_NAME}/**/target" \
  --exclude="${PROJECT_NAME}/.DS_Store" \
  --exclude="${PROJECT_NAME}/**/.DS_Store" \
  --exclude="${PROJECT_NAME}/*.log" \
  --exclude="${PROJECT_NAME}/**/*.log" \
  --exclude="${PROJECT_NAME}/frontend/dist" \
  --exclude="${PROJECT_NAME}/frontend-personal/dist" \
  --exclude="${PROJECT_NAME}/frontend-church/dist" \
  --exclude="${PROJECT_NAME}/frontend-church-admin/dist" \
  --exclude="${PROJECT_NAME}/**/dist" \
  --exclude="${PROJECT_NAME}/*.tar.gz" \
  --exclude="${PROJECT_NAME}/local-letsencrypt" \
  --exclude="${PROJECT_NAME}/local-letsencrypt/**" \
  --exclude="${PROJECT_NAME}/docker-compose.local.yml" \
  --exclude="${PROJECT_NAME}/docker-compose.override.yml" \
  "${PROJECT_NAME}"

if [[ ! -f "${ARCHIVE_NAME}" ]]; then
  echo "❌ 打包失敗：找不到 ${ARCHIVE_NAME}"
  exit 1
fi

ARCHIVE_SIZE="$(du -h "${ARCHIVE_NAME}" | awk '{print $1}')"
echo "✅ 打包完成：${ARCHIVE_NAME}（${ARCHIVE_SIZE}）"
echo

# ====== Step 2: 確保遠端目錄存在 ======
echo "Step 2/4: 建立遠端目錄..."
ssh_run "mkdir -p ${REMOTE_PATH}"
echo "✅ 遠端目錄 OK"
echo

# ====== Step 3: 上傳壓縮檔 + remote_deploy.sh ======
echo "Step 3/4: 上傳壓縮檔與遠端部署腳本..."
scp_put "${ARCHIVE_NAME}" "${REMOTE_PATH}/"
scp_put "${PROJECT_ROOT}/remote_deploy.sh" "${REMOTE_DEPLOY_REMOTE}"
echo "✅ 上傳完成"
echo

# ====== Step 4: 遠端執行 remote_deploy.sh ======
echo "Step 4/4: 遠端部署中（remote_deploy.sh）..."
# 兼容：若 remote_deploy.sh 被 CRLF 汙染，也會修掉
ssh_run "sed -i 's/\r$//' ${REMOTE_DEPLOY_REMOTE} 2>/dev/null || true; chmod +x ${REMOTE_DEPLOY_REMOTE}; bash ${REMOTE_DEPLOY_REMOTE}; rm -f ${REMOTE_DEPLOY_REMOTE} || true"
echo
echo "=========================================="
echo "✅ 完成：Mac/Linux 與 Windows 已使用同一套遠端部署流程"
echo "=========================================="
echo
echo "查看服務狀態："
echo "  ssh ${SERVER_USER}@${SERVER_IP}"
echo "  cd ${REMOTE_PATH}/${PROJECT_NAME}"
echo "  docker compose ps"
echo
echo "查看最新監控日誌："
echo "  ssh ${SERVER_USER}@${SERVER_IP} 'tail -f /root/project/work/logs/frontend-monitor_latest.log'"
echo "  ssh ${SERVER_USER}@${SERVER_IP} 'tail -f /root/project/work/logs/system-monitor_latest.log'"
echo
