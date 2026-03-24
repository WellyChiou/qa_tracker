#!/usr/bin/env bash
set -euo pipefail

# ==========================================
# 一鍵部署腳本 (Mac/Linux 版本)
# 功能：打包專案 -> 上傳到伺服器 -> 遠端部署
#
# 模式：
# - all (預設)：維持既有整包部署流程（remote_deploy.sh）
# - personal：僅部署 personal（backend-personal + frontend-personal）
# - church：僅部署 church（backend-church + frontend-church + frontend-church-admin）
# - invest：invest-only 最小同步 + docker-compose.invest.yml 啟動
# ==========================================

# ====== 配置區（需要改就改這裡）======
SERVER_IP="38.54.89.136"
SERVER_USER="root"
# 下面這行可留空（建議用 SSH Key）；若你有 sshpass 也可填密碼
SERVER_PASSWORD=""   # 例如：SERVER_PASSWORD="your_password"
REMOTE_PATH="/root/project/work"
PROJECT_NAME="docker-vue-java-mysql"
MODE=""
ARCHIVE_NAME="${PROJECT_NAME}.tar.gz"
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

select_mode_interactive() {
  echo "=========================================="
  echo "請選擇部署模式："
  echo "1) all（全部專案）"
  echo "2) personal"
  echo "3) church（frontend-church + frontend-church-admin + backend-church）"
  echo "4) invest"
  echo "=========================================="
  read -r -p "請輸入選項 [1-4，預設 1]: " choice

  if [[ -z "${choice}" ]]; then
    MODE="all"
    return
  fi

  case "${choice}" in
    1) MODE="all" ;;
    2) MODE="personal" ;;
    3) MODE="church" ;;
    4) MODE="invest" ;;
    *)
      echo "❌ 無效選擇：${choice}"
      exit 1
      ;;
  esac
}

if [[ $# -ge 1 ]]; then
  MODE="$1"
else
  select_mode_interactive
  echo "你選擇的是：${MODE}"
fi

# shellcheck disable=SC2001
MODE="$(echo "${MODE}" | tr '[:upper:]' '[:lower:]')"

if [[ "$MODE" != "all" && "$MODE" != "personal" && "$MODE" != "church" && "$MODE" != "invest" ]]; then
  echo "❌ 不支援的 mode: ${MODE}"
  echo "用法："
  echo "  ./scripts/deployment/deploy-to-server-v1.1.sh"
  echo "  ./scripts/deployment/deploy-to-server-v1.1.sh personal"
  echo "  ./scripts/deployment/deploy-to-server-v1.1.sh church"
  echo "  ./scripts/deployment/deploy-to-server-v1.1.sh invest"
  exit 1
fi

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

ssh_run_script() {
  if [[ -n "${SERVER_PASSWORD}" && "${HAS_SSHPASS}" -eq 1 ]]; then
    sshpass -p "${SERVER_PASSWORD}" ssh ${SSH_OPTS} "${SERVER_USER}@${SERVER_IP}" bash -s -- "$@"
  else
    ssh ${SSH_OPTS} "${SERVER_USER}@${SERVER_IP}" bash -s -- "$@"
  fi
}

echo "=========================================="
echo "🚀 開始一鍵部署（Mac/Linux - 同步版）"
echo "=========================================="
echo "Server: ${SERVER_USER}@${SERVER_IP}"
echo "Remote Path: ${REMOTE_PATH}"
echo "Project: ${PROJECT_NAME}"
echo "Mode: ${MODE}"
echo "開始部署模式：${MODE}"
echo

# ====== 取得腳本位置 / 專案根 ======
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 自動查找專案根目錄（向上查找直到找到 docker-compose.yml）
PROJECT_ROOT="${SCRIPT_DIR}"
while [[ ! -f "${PROJECT_ROOT}/docker-compose.yml" ]] && [[ "${PROJECT_ROOT}" != "/" ]]; do
  PROJECT_ROOT="$(cd "${PROJECT_ROOT}/.." && pwd)"
done

if [[ ! -f "${PROJECT_ROOT}/docker-compose.yml" ]]; then
  echo "❌ 找不到 docker-compose.yml。請確認在專案目錄中執行。"
  echo "   目前: ${SCRIPT_DIR}"
  exit 1
fi

PARENT_DIR="$(cd "${PROJECT_ROOT}/.." && pwd)"

if [[ "${MODE}" == "all" && ! -f "${PROJECT_ROOT}/scripts/deployment/remote_deploy.sh" ]]; then
  echo "❌ 找不到 ${PROJECT_ROOT}/scripts/deployment/remote_deploy.sh"
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

if [[ "${MODE}" == "all" ]]; then
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
    --exclude="${PROJECT_NAME}/frontend/personal/dist" \
    --exclude="${PROJECT_NAME}/frontend/church/dist" \
    --exclude="${PROJECT_NAME}/frontend/church-admin/dist" \
    --exclude="${PROJECT_NAME}/frontend/invest-admin/dist" \
    --exclude="${PROJECT_NAME}/**/dist" \
    --exclude="${PROJECT_NAME}/*.tar.gz" \
    --exclude="${PROJECT_NAME}/local-letsencrypt" \
    --exclude="${PROJECT_NAME}/local-letsencrypt/**" \
    --exclude="${PROJECT_NAME}/docker-compose.local.yml" \
    --exclude="${PROJECT_NAME}/docker-compose.override.yml" \
    "${PROJECT_NAME}"
else
  ARCHIVE_NAME="${PROJECT_NAME}-${MODE}.tar.gz"
  if [[ -f "${ARCHIVE_NAME}" ]]; then
    rm -f "${ARCHIVE_NAME}"
  fi

  TAR_PATHS=()
  if [[ "${MODE}" == "invest" ]]; then
    REQUIRED_INVEST_PATHS=(
      "pom.xml"
      "backend/auth-core"
      "backend/invest"
      "frontend/invest-admin"
      "shared"
      "database/invest"
      "scripts/migration"
      "docker-compose.invest.yml"
      "nginx/nginx-invest.conf"
    )
    OPTIONAL_PARTIAL_PATHS=(
      ".dockerignore"
      ".env.invest.prod"
      ".env.invest.prod.example"
    )

    for path in "${REQUIRED_INVEST_PATHS[@]}"; do
      if [[ ! -e "${PROJECT_ROOT}/${path}" ]]; then
        echo "❌ ${MODE} 模式缺少必要檔案：${path}"
        exit 1
      fi
      TAR_PATHS+=("${PROJECT_NAME}/${path}")
    done
  else
    COMMON_PARTIAL_PATHS=(
      "pom.xml"
      "backend/auth-core"
      "shared"
      "database/personal"
      "database/church"
      "database/invest"
      "docker-compose.yml"
    )

    OPTIONAL_PARTIAL_PATHS=(
      ".dockerignore"
      ".env.invest.prod"
      ".env.invest.prod.example"
    )

    REQUIRED_MODE_PATHS=()
    case "${MODE}" in
      personal)
        REQUIRED_MODE_PATHS=(
          "backend/personal"
          "frontend/personal"
        )
        ;;
      church)
        REQUIRED_MODE_PATHS=(
          "backend/church"
          "frontend/church"
          "frontend/church-admin"
        )
        ;;
      *)
        echo "❌ 未支援的模式：${MODE}"
        exit 1
        ;;
    esac

    for path in "${COMMON_PARTIAL_PATHS[@]}" "${REQUIRED_MODE_PATHS[@]}"; do
      if [[ ! -e "${PROJECT_ROOT}/${path}" ]]; then
        echo "❌ ${MODE} 模式缺少必要檔案：${path}"
        exit 1
      fi
      TAR_PATHS+=("${PROJECT_NAME}/${path}")
    done
  fi

  for path in "${OPTIONAL_PARTIAL_PATHS[@]}"; do
    if [[ ! -e "${PROJECT_ROOT}/${path}" ]]; then
      continue
    fi
    TAR_PATHS+=("${PROJECT_NAME}/${path}")
  done

  tar --format=ustar -czf "${ARCHIVE_NAME}" "${TAR_PATHS[@]}"
fi

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
echo "Step 3/4: 上傳壓縮檔..."
scp_put "${PARENT_DIR}/${ARCHIVE_NAME}" "${REMOTE_PATH}/"
if [[ "${MODE}" == "all" ]]; then
  scp_put "${PROJECT_ROOT}/scripts/deployment/remote_deploy.sh" "${REMOTE_DEPLOY_REMOTE}"
fi
echo "✅ 上傳完成"
echo

# ====== Step 4: 遠端部署 ======
if [[ "${MODE}" == "all" ]]; then
  echo "Step 4/4: 遠端部署中（remote_deploy.sh）..."
  # 兼容：若 remote_deploy.sh 被 CRLF 汙染，也會修掉。
  # 注意：不要把 bash 失敗碼吞掉，否則會出現「失敗卻顯示成功」。
  ssh_run "set -euo pipefail; sed -i 's/\r$//' ${REMOTE_DEPLOY_REMOTE} 2>/dev/null || true; chmod +x ${REMOTE_DEPLOY_REMOTE}; bash ${REMOTE_DEPLOY_REMOTE}"
  ssh_run "rm -f ${REMOTE_DEPLOY_REMOTE} || true"
elif [[ "${MODE}" == "invest" ]]; then
  echo "Step 4/4: 遠端部署中（invest-only）..."
  ssh_run_script "${REMOTE_PATH}" "${PROJECT_NAME}" "${ARCHIVE_NAME}" << 'EOS'
set -euo pipefail

REMOTE_PATH="$1"
PROJECT_NAME="$2"
ARCHIVE_NAME="$3"
BACKUP_DIR="${PROJECT_NAME}_backup_invest"

cd "${REMOTE_PATH}"

if [[ -d "${PROJECT_NAME}" ]]; then
  rm -rf "${BACKUP_DIR}" || true
  mv "${PROJECT_NAME}" "${BACKUP_DIR}"
fi

tar -xzf "${ARCHIVE_NAME}" 2>&1 | grep -v "Ignoring unknown extended header" || true

REMOTE_DIR="$(tar -tzf "${ARCHIVE_NAME}" | head -1 | cut -d'/' -f1)"
if [[ -d "${REMOTE_DIR}" && "${REMOTE_DIR}" != "${PROJECT_NAME}" ]]; then
  mv "${REMOTE_DIR}" "${PROJECT_NAME}"
fi

if [[ ! -d "${PROJECT_NAME}" ]]; then
  echo "❌ 解壓後找不到 ${PROJECT_NAME}"
  exit 1
fi

cd "${PROJECT_NAME}"
find . -type f -name "*.sh" -exec sed -i 's/\r$//' {} \; || true
find . -type f -name "*.sh" -exec chmod +x {} \; || true

COMPOSE_ENV_ARGS=()
if [[ -f ".env.invest.prod" ]]; then
  COMPOSE_ENV_ARGS+=(--env-file .env.invest.prod)
fi

# invest-only 強制使用獨立 project name，避免與 all 模式共用預設 project 造成混淆
COMPOSE_ARGS=("${COMPOSE_ENV_ARGS[@]}" -p invest -f docker-compose.invest.yml)

echo "== invest-only compose project =="
echo "docker compose ${COMPOSE_ARGS[*]}"
echo "== invest-only services =="
docker compose "${COMPOSE_ARGS[@]}" config --services

docker compose "${COMPOSE_ARGS[@]}" down || true
docker compose "${COMPOSE_ARGS[@]}" up -d mysql

MIGRATION_ARGS=(--compose-file docker-compose.invest.yml)
MIGRATION_ARGS+=(--project-name invest)
if [[ -f ".env.invest.prod" ]]; then
  MIGRATION_ARGS+=(--env-file .env.invest.prod)
fi

if [[ ! -x "./scripts/migration/run-invest-migrations.sh" ]]; then
  chmod +x ./scripts/migration/run-invest-migrations.sh
fi

./scripts/migration/run-invest-migrations.sh "${MIGRATION_ARGS[@]}"

docker compose "${COMPOSE_ARGS[@]}" up -d --build mysql backend-invest frontend-invest-admin nginx
docker compose "${COMPOSE_ARGS[@]}" ps

echo "== invest-only quick health =="
docker ps --format '{{.Names}}' | grep -E '^backend_invest$|^frontend_invest_admin$|^nginx_invest$|^mysql_invest_db$' || {
  echo "❌ invest-only 容器檢查失敗（backend_invest/frontend_invest_admin/nginx_invest/mysql_invest_db）"
  exit 1
}
echo "✅ invest-only 部署完成（project=invest）"
EOS
else
  echo "Step 4/4: 遠端部署中（${MODE}-only）..."
  ssh_run_script "${REMOTE_PATH}" "${PROJECT_NAME}" "${ARCHIVE_NAME}" "${MODE}" << 'EOS'
set -euo pipefail

REMOTE_PATH="$1"
PROJECT_NAME="$2"
ARCHIVE_NAME="$3"
MODE="$4"

cd "${REMOTE_PATH}"

if [[ ! -d "${PROJECT_NAME}" ]]; then
  echo "❌ 找不到既有專案目錄 ${PROJECT_NAME}，請先執行 all 模式初始化部署。"
  exit 1
fi

tar -xzf "${ARCHIVE_NAME}" 2>&1 | grep -v "Ignoring unknown extended header" || true

if [[ ! -d "${PROJECT_NAME}" ]]; then
  echo "❌ 解壓後找不到 ${PROJECT_NAME}"
  exit 1
fi

cd "${PROJECT_NAME}"
find . -type f -name "*.sh" -exec sed -i 's/\r$//' {} \; || true
find . -type f -name "*.sh" -exec chmod +x {} \; || true

SERVICES=()
case "${MODE}" in
  personal)
    SERVICES=("backend-personal" "frontend-personal")
    ;;
  church)
    SERVICES=("backend-church" "frontend-church" "frontend-church-admin")
    ;;
  *)
    echo "❌ 不支援的模式：${MODE}"
    exit 1
    ;;
esac

docker compose build "${SERVICES[@]}"
docker compose up -d "${SERVICES[@]}"
docker compose ps "${SERVICES[@]}"
EOS
fi
echo
echo "=========================================="
echo "✅ 完成：${MODE} 模式部署成功"
echo "=========================================="
echo
echo "查看服務狀態："
echo "  ssh ${SERVER_USER}@${SERVER_IP}"
echo "  cd ${REMOTE_PATH}/${PROJECT_NAME}"
if [[ "${MODE}" == "invest" ]]; then
  echo "  docker compose -f docker-compose.invest.yml ps"
else
  echo "  docker compose ps"
fi
echo
if [[ "${MODE}" == "all" ]]; then
  echo "查看最新監控日誌："
  echo "  ssh ${SERVER_USER}@${SERVER_IP} 'tail -f /root/project/work/logs/frontend-monitor_latest.log'"
  echo "  ssh ${SERVER_USER}@${SERVER_IP} 'tail -f /root/project/work/logs/system-monitor_latest.log'"
  echo
fi
