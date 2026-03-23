#!/usr/bin/env bash
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

BASE_URL="${BASE_URL:-https://localhost}"
BASE_URL="${BASE_URL%/}"
WORKDIR="${WORKDIR:-${PROJECT_ROOT}}"
ENV_FILE="${ENV_FILE:-.env.invest.prod}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.invest.yml}"
USE_CURL_K="${USE_CURL_K:-true}"

ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"
VIEWER_USERNAME="${VIEWER_USERNAME:-viewer}"
VIEWER_PASSWORD="${VIEWER_PASSWORD:-viewer123}"

EXPECTED_JOBS=(
  "PRICE_UPDATE_HOLDINGS"
  "DAILY_PORTFOLIO_RISK_REPORT"
  "PORTFOLIO_ALERT_POLLING"
  "MARKET_ANALYSIS"
)

FAIL_STEPS=()
PASS_COUNT=0
FAIL_COUNT=0

print_usage() {
  cat <<'EOF'
用法：
  ./scripts/deployment/smoke-invest-scheduler.sh [options]

Options:
  --base-url <url>         API 基底位址（預設: https://localhost）
  --workdir <path>         專案根目錄（預設: 自動偵測）
  --env-file <path>        migration runner 使用的 env 檔（預設: .env.invest.prod）
  --compose-file <path>    migration runner compose 檔（預設: docker-compose.invest.yml）
  --no-k                   關閉 curl -k（預設開啟）
  --admin-user <username>  管理員帳號（預設: admin）
  --admin-pass <password>  管理員密碼（預設: admin123）
  --viewer-user <username> viewer 帳號（預設: viewer）
  --viewer-pass <password> viewer 密碼（預設: viewer123）
  --help                   顯示說明

環境變數也可覆蓋同名參數：
  BASE_URL, WORKDIR, ENV_FILE, COMPOSE_FILE, USE_CURL_K,
  ADMIN_USERNAME, ADMIN_PASSWORD, VIEWER_USERNAME, VIEWER_PASSWORD
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-url)
      BASE_URL="${2:-}"
      BASE_URL="${BASE_URL%/}"
      shift 2
      ;;
    --workdir)
      WORKDIR="${2:-}"
      shift 2
      ;;
    --env-file)
      ENV_FILE="${2:-}"
      shift 2
      ;;
    --compose-file)
      COMPOSE_FILE="${2:-}"
      shift 2
      ;;
    --no-k)
      USE_CURL_K="false"
      shift
      ;;
    --admin-user)
      ADMIN_USERNAME="${2:-}"
      shift 2
      ;;
    --admin-pass)
      ADMIN_PASSWORD="${2:-}"
      shift 2
      ;;
    --viewer-user)
      VIEWER_USERNAME="${2:-}"
      shift 2
      ;;
    --viewer-pass)
      VIEWER_PASSWORD="${2:-}"
      shift 2
      ;;
    --help|-h)
      print_usage
      exit 0
      ;;
    *)
      echo "❌ 未知參數：$1"
      print_usage
      exit 1
      ;;
  esac
done

CURL_COMMON_OPTS=(-sS --connect-timeout 15 --max-time 90)
if [[ "${USE_CURL_K}" == "true" ]]; then
  CURL_COMMON_OPTS+=(-k)
fi

step() {
  echo
  echo "=================================================="
  echo "[STEP] $1"
  echo "=================================================="
}

pass() {
  PASS_COUNT=$((PASS_COUNT + 1))
  echo "[PASS] $1"
}

fail() {
  FAIL_COUNT=$((FAIL_COUNT + 1))
  FAIL_STEPS+=("$1")
  echo "[FAIL] $1"
}

require_cmd() {
  local cmd="$1"
  if ! command -v "${cmd}" >/dev/null 2>&1; then
    fail "缺少必要指令：${cmd}"
    return 1
  fi
  pass "必要指令存在：${cmd}"
  return 0
}

api_request() {
  local method="$1"
  local url="$2"
  local token="${3:-}"
  local payload="${4:-}"
  local response_file="$5"
  local http_code_var="$6"
  local status

  local curl_cmd=(
    curl
    "${CURL_COMMON_OPTS[@]}"
    -o "${response_file}"
    -w "%{http_code}"
    -X "${method}"
    -H "Accept: application/json"
  )

  if [[ -n "${token}" ]]; then
    curl_cmd+=(-H "Authorization: Bearer ${token}")
  fi

  if [[ -n "${payload}" ]]; then
    curl_cmd+=(-H "Content-Type: application/json" --data "${payload}")
  fi

  curl_cmd+=("${url}")

  status="$("${curl_cmd[@]}" 2>/dev/null || true)"
  printf -v "${http_code_var}" '%s' "${status}"
}

is_json_success_true() {
  local body_file="$1"
  jq -e '.success == true' "${body_file}" >/dev/null 2>&1
}

extract_token() {
  local body_file="$1"
  jq -r '.data.accessToken // empty' "${body_file}" 2>/dev/null
}

resolve_env_file_arg() {
  if [[ -z "${ENV_FILE}" ]]; then
    return 0
  fi

  if [[ -f "${ENV_FILE}" ]]; then
    return 0
  fi

  if [[ -f "${WORKDIR}/${ENV_FILE}" ]]; then
    ENV_FILE="${WORKDIR}/${ENV_FILE}"
    return 0
  fi

  echo "❌ 找不到 ENV_FILE：${ENV_FILE}"
  return 1
}

run_migrations() {
  step "執行 Invest migration runner"

  local cmd=(
    ./scripts/migration/run-invest-migrations.sh
    --compose-file "${COMPOSE_FILE}"
  )

  if ! resolve_env_file_arg; then
    fail "migration runner：env file 不存在"
    return 0
  fi

  if [[ -n "${ENV_FILE}" ]]; then
    cmd+=(--env-file "${ENV_FILE}")
  fi

  if [[ ! -d "${WORKDIR}" ]]; then
    fail "migration runner：WORKDIR 不存在 (${WORKDIR})"
    return 0
  fi

  if [[ ! -x "${WORKDIR}/scripts/migration/run-invest-migrations.sh" ]]; then
    fail "migration runner：找不到可執行腳本 scripts/migration/run-invest-migrations.sh"
    return 0
  fi

  if (cd "${WORKDIR}" && "${cmd[@]}"); then
    pass "migration runner 執行成功"
  else
    fail "migration runner 執行失敗"
  fi
}

login_and_get_token() {
  local username="$1"
  local password="$2"
  local token_var="$3"
  local label="$4"
  local response_file
  local http_code
  local token

  response_file="$(mktemp)"
  api_request "POST" "${BASE_URL}/api/invest/auth/login" "" \
    "{\"username\":\"${username}\",\"password\":\"${password}\"}" \
    "${response_file}" http_code

  if [[ "${http_code}" != "200" ]]; then
    fail "${label} 登入失敗（HTTP ${http_code}）"
    echo "      回應：$(cat "${response_file}")"
    rm -f "${response_file}"
    printf -v "${token_var}" '%s' ""
    return 0
  fi

  if ! is_json_success_true "${response_file}"; then
    fail "${label} 登入失敗（success!=true）"
    echo "      回應：$(cat "${response_file}")"
    rm -f "${response_file}"
    printf -v "${token_var}" '%s' ""
    return 0
  fi

  token="$(extract_token "${response_file}")"
  rm -f "${response_file}"

  if [[ -z "${token}" ]]; then
    fail "${label} 登入失敗（未取得 accessToken）"
    printf -v "${token_var}" '%s' ""
    return 0
  fi

  pass "${label} 登入成功，已取得 token"
  printf -v "${token_var}" '%s' "${token}"
}

verify_jobs_list_contains_all() {
  local token="$1"
  local response_file
  local http_code
  local job

  step "查詢 jobs list 並驗證 4 個 job"
  response_file="$(mktemp)"
  api_request "GET" "${BASE_URL}/api/invest/system/scheduler/jobs" "${token}" "" "${response_file}" http_code

  if [[ "${http_code}" != "200" ]]; then
    fail "查 jobs list 失敗（HTTP ${http_code}）"
    echo "      回應：$(cat "${response_file}")"
    rm -f "${response_file}"
    return 0
  fi

  if ! is_json_success_true "${response_file}"; then
    fail "查 jobs list 失敗（success!=true）"
    echo "      回應：$(cat "${response_file}")"
    rm -f "${response_file}"
    return 0
  fi

  pass "查 jobs list 成功"

  for job in "${EXPECTED_JOBS[@]}"; do
    if jq -e --arg job "${job}" '.data | map(.jobCode) | index($job) != null' "${response_file}" >/dev/null 2>&1; then
      pass "jobs list 包含 ${job}"
    else
      fail "jobs list 缺少 ${job}"
    fi
  done

  rm -f "${response_file}"
}

run_job_now_and_verify() {
  local token="$1"
  local job_code="$2"
  local response_file
  local http_code

  response_file="$(mktemp)"
  api_request "POST" "${BASE_URL}/api/invest/system/scheduler/jobs/${job_code}/run-now" "${token}" "" "${response_file}" http_code

  if [[ "${http_code}" != "200" ]]; then
    fail "Run Now ${job_code} 失敗（HTTP ${http_code}）"
    echo "      回應：$(cat "${response_file}")"
    rm -f "${response_file}"
    return 0
  fi

  if ! is_json_success_true "${response_file}"; then
    fail "Run Now ${job_code} 失敗（success!=true）"
    echo "      回應：$(cat "${response_file}")"
    rm -f "${response_file}"
    return 0
  fi

  pass "Run Now ${job_code} 成功"
  rm -f "${response_file}"
}

verify_job_logs_paged() {
  local token="$1"
  local job_code="$2"
  local response_file
  local http_code
  local attempts=0
  local max_attempts=5

  while [[ "${attempts}" -lt "${max_attempts}" ]]; do
    attempts=$((attempts + 1))
    response_file="$(mktemp)"
    api_request "GET" "${BASE_URL}/api/invest/system/scheduler/jobs/${job_code}/logs/paged?page=0&size=10" \
      "${token}" "" "${response_file}" http_code

    if [[ "${http_code}" == "200" ]] \
      && is_json_success_true "${response_file}" \
      && jq -e '(.data.content | length) > 0' "${response_file}" >/dev/null 2>&1; then
      pass "Logs ${job_code} 有資料（第 ${attempts} 次）"
      rm -f "${response_file}"
      return 0
    fi

    if [[ "${attempts}" -lt "${max_attempts}" ]]; then
      sleep 1
    fi
    rm -f "${response_file}"
  done

  fail "Logs ${job_code} 無資料或查詢失敗（重試 ${max_attempts} 次）"
}

verify_run_now_and_logs_for_all_jobs() {
  local token="$1"
  local job

  step "依序執行 4 個 job 的 Run Now"
  for job in "${EXPECTED_JOBS[@]}"; do
    run_job_now_and_verify "${token}" "${job}"
  done

  step "依序驗證 4 個 job 的 logs/paged"
  for job in "${EXPECTED_JOBS[@]}"; do
    verify_job_logs_paged "${token}" "${job}"
  done
}

verify_viewer_permission_behavior() {
  local admin_token="$1"
  local viewer_token=""
  local response_file
  local http_code

  step "驗證 viewer 權限（可 VIEW、不可 RUN）"
  login_and_get_token "${VIEWER_USERNAME}" "${VIEWER_PASSWORD}" viewer_token "viewer"
  if [[ -z "${viewer_token}" ]]; then
    fail "viewer 權限驗證中止：無法登入 viewer 帳號（請確認帳號存在且有 VIEW 權限）"
    return 0
  fi

  response_file="$(mktemp)"
  api_request "GET" "${BASE_URL}/api/invest/system/scheduler/jobs" "${viewer_token}" "" "${response_file}" http_code
  if [[ "${http_code}" == "200" ]] && is_json_success_true "${response_file}"; then
    pass "viewer 查 jobs list 為 200"
  else
    fail "viewer 查 jobs list 非 200/success（HTTP ${http_code}）"
    echo "      回應：$(cat "${response_file}")"
  fi
  rm -f "${response_file}"

  response_file="$(mktemp)"
  api_request "POST" "${BASE_URL}/api/invest/system/scheduler/jobs/PRICE_UPDATE_HOLDINGS/run-now" "${viewer_token}" "" "${response_file}" http_code
  if [[ "${http_code}" == "403" ]]; then
    pass "viewer 執行 Run Now 正確被拒絕（403）"
  else
    fail "viewer 執行 Run Now 未回 403（HTTP ${http_code}）"
    echo "      回應：$(cat "${response_file}")"
  fi
  rm -f "${response_file}"

  if [[ -n "${admin_token}" ]]; then
    pass "管理員 token 仍可用（viewer 測試不影響 admin）"
  fi
}

main() {
  echo "=========================================="
  echo "Invest System Scheduler Smoke Test"
  echo "BASE_URL    : ${BASE_URL}"
  echo "WORKDIR     : ${WORKDIR}"
  echo "ENV_FILE    : ${ENV_FILE}"
  echo "COMPOSE_FILE: ${COMPOSE_FILE}"
  echo "curl -k     : ${USE_CURL_K}"
  echo "=========================================="

  step "檢查必要工具"
  require_cmd bash
  require_cmd curl
  require_cmd jq
  require_cmd docker

  run_migrations

  step "admin 登入取得 token"
  local admin_token=""
  login_and_get_token "${ADMIN_USERNAME}" "${ADMIN_PASSWORD}" admin_token "admin"

  if [[ -z "${admin_token}" ]]; then
    fail "admin token 取得失敗，後續 API 測試無法執行"
  else
    verify_jobs_list_contains_all "${admin_token}"
    verify_run_now_and_logs_for_all_jobs "${admin_token}"
    verify_viewer_permission_behavior "${admin_token}"
  fi

  echo
  echo "=========================================="
  if [[ "${FAIL_COUNT}" -eq 0 ]]; then
    echo "PASS：排程管理 smoke 驗收全部成功"
    echo "通過數：${PASS_COUNT}"
    echo "失敗數：${FAIL_COUNT}"
    echo "=========================================="
    exit 0
  fi

  echo "FAIL：排程管理 smoke 驗收有失敗"
  echo "通過數：${PASS_COUNT}"
  echo "失敗數：${FAIL_COUNT}"
  echo "失敗步驟："
  for item in "${FAIL_STEPS[@]}"; do
    echo " - ${item}"
  done
  echo "=========================================="
  exit 1
}

main "$@"
