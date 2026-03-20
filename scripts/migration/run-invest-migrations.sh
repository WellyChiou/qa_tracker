#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="docker-compose.invest.yml"
MYSQL_SERVICE="mysql"
INCLUDE_DEMO="false"
ENV_FILE=""
DB_NAME="invest"
HISTORY_TABLE="schema_migration_history"

usage() {
  cat <<'EOF'
用法：
  ./scripts/migration/run-invest-migrations.sh [options]

Options:
  --compose-file <path>    docker compose 檔案（預設: docker-compose.invest.yml）
  --env-file <path>        docker compose env file（選填）
  --mysql-service <name>   MySQL service name（預設: mysql）
  --db-name <name>         目標資料庫（預設: invest）
  --include-demo           額外執行 seed-demo（預設不執行）
  --help                   顯示說明

說明：
  - 只會執行「未記錄於 schema_migration_history」的 migration。
  - migration 失敗會立即停止，exit 1。
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --compose-file)
      COMPOSE_FILE="${2:-}"
      shift 2
      ;;
    --env-file)
      ENV_FILE="${2:-}"
      shift 2
      ;;
    --mysql-service)
      MYSQL_SERVICE="${2:-}"
      shift 2
      ;;
    --db-name)
      DB_NAME="${2:-}"
      shift 2
      ;;
    --include-demo)
      INCLUDE_DEMO="true"
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "❌ 未知參數：$1"
      usage
      exit 1
      ;;
  esac
done

if [[ -z "${COMPOSE_FILE}" ]]; then
  echo "❌ --compose-file 不可為空"
  exit 1
fi

if [[ ! -f "${PROJECT_ROOT}/${COMPOSE_FILE}" ]]; then
  echo "❌ 找不到 compose file: ${PROJECT_ROOT}/${COMPOSE_FILE}"
  exit 1
fi

if [[ -n "${ENV_FILE}" && ! -f "${PROJECT_ROOT}/${ENV_FILE}" ]]; then
  echo "❌ 找不到 env file: ${PROJECT_ROOT}/${ENV_FILE}"
  exit 1
fi

MIGRATION_SCHEMA_DIR="${PROJECT_ROOT}/database/invest/migrations/schema"
MIGRATION_BASELINE_DIR="${PROJECT_ROOT}/database/invest/migrations/seed-baseline"
MIGRATION_DEMO_DIR="${PROJECT_ROOT}/database/invest/migrations/seed-demo"

for dir in "${MIGRATION_SCHEMA_DIR}" "${MIGRATION_BASELINE_DIR}"; do
  if [[ ! -d "${dir}" ]]; then
    echo "❌ migration 目錄不存在：${dir}"
    exit 1
  fi
done

COMPOSE_ARGS=("-f" "${COMPOSE_FILE}")
if [[ -n "${ENV_FILE}" ]]; then
  COMPOSE_ARGS=(--env-file "${ENV_FILE}" "${COMPOSE_ARGS[@]}")
fi

dc() {
  docker compose "${COMPOSE_ARGS[@]}" "$@"
}

mysql_exec() {
  local sql="$1"
  printf '%s\n' "${sql}" | dc exec -T "${MYSQL_SERVICE}" sh -lc 'mysql --default-character-set=utf8mb4 -u"$MYSQL_USER" -p"$MYSQL_PASSWORD"'
}

mysql_query_scalar() {
  local sql="$1"
  printf '%s\n' "${sql}" | dc exec -T "${MYSQL_SERVICE}" sh -lc 'mysql --default-character-set=utf8mb4 -N -u"$MYSQL_USER" -p"$MYSQL_PASSWORD"' | tr -d '\r' | head -n1
}

mysql_run_file() {
  local file="$1"
  cat "${file}" | dc exec -T "${MYSQL_SERVICE}" sh -lc 'mysql --default-character-set=utf8mb4 -u"$MYSQL_USER" -p"$MYSQL_PASSWORD"'
}

wait_mysql_ready() {
  local max_retry=30
  local retry=1

  while [[ "${retry}" -le "${max_retry}" ]]; do
    if dc exec -T "${MYSQL_SERVICE}" sh -lc 'mysqladmin ping -h localhost -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --silent' >/dev/null 2>&1; then
      echo "✅ MySQL 已就緒"
      return 0
    fi
    echo "⏳ 等待 MySQL 就緒 (${retry}/${max_retry})..."
    retry=$((retry + 1))
    sleep 2
  done

  echo "❌ MySQL 未在預期時間內就緒"
  exit 1
}

extract_version() {
  local prefix="$1"
  local base_name="$2"
  local num
  local decimal_num
  num="$(echo "${base_name}" | sed -E 's/^([0-9]+).*/\1/')"
  if [[ ! "${num}" =~ ^[0-9]+$ ]]; then
    echo "${prefix}_$(echo "${base_name}" | sed -E 's/\.sql$//' | tr -cd '[:alnum:]_')"
    return
  fi
  decimal_num=$((10#${num}))
  printf "%s%03d" "${prefix}" "${decimal_num}"
}

extract_description() {
  local base_name="$1"
  local no_ext
  no_ext="${base_name%.sql}"
  echo "${no_ext}" | sed -E 's/^[0-9]+_//'
}

escape_sql() {
  local value="$1"
  echo "${value//\'/\'\'}"
}

run_group() {
  local group_prefix="$1"
  local group_dir="$2"
  local group_name="$3"
  local files=()

  while IFS= read -r file; do
    files+=("${file}")
  done < <(find "${group_dir}" -maxdepth 1 -type f -name '*.sql' | sort)
  if [[ "${#files[@]}" -eq 0 ]]; then
    echo "ℹ️ ${group_name} 無 migration"
    return 0
  fi

  echo "== 執行 ${group_name} migrations =="
  for file in "${files[@]}"; do
    local base_name
    base_name="$(basename "${file}")"
    local version
    version="$(extract_version "${group_prefix}" "${base_name}")"
    local description
    description="$(extract_description "${base_name}")"
    local script_name
    script_name="${group_name}/${base_name}"

    local escaped_version
    escaped_version="$(escape_sql "${version}")"
    local executed_count
    executed_count="$(mysql_query_scalar "USE ${DB_NAME}; SELECT COUNT(1) FROM ${HISTORY_TABLE} WHERE version='${escaped_version}';")"
    executed_count="${executed_count:-0}"

    if [[ "${executed_count}" != "0" ]]; then
      echo "⏭️  跳過已執行：${version} (${script_name})"
      continue
    fi

    echo "▶ 執行：${version} (${script_name})"
    if ! mysql_run_file "${file}"; then
      echo "❌ migration 失敗：${script_name}"
      exit 1
    fi

    local escaped_description
    escaped_description="$(escape_sql "${description}")"
    local escaped_script
    escaped_script="$(escape_sql "${script_name}")"
    mysql_exec "USE ${DB_NAME}; INSERT INTO ${HISTORY_TABLE}(version, description, script_name) VALUES ('${escaped_version}', '${escaped_description}', '${escaped_script}');"
    echo "✅ 完成：${version}"
  done
}

echo "=========================================="
echo "🧱 Invest Migration Runner"
echo "Compose file : ${COMPOSE_FILE}"
if [[ -n "${ENV_FILE}" ]]; then
  echo "Env file     : ${ENV_FILE}"
fi
echo "MySQL svc    : ${MYSQL_SERVICE}"
echo "DB           : ${DB_NAME}"
echo "Include demo : ${INCLUDE_DEMO}"
echo "=========================================="

cd "${PROJECT_ROOT}"
wait_mysql_ready

mysql_exec "USE ${DB_NAME};
CREATE TABLE IF NOT EXISTS ${HISTORY_TABLE} (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  version VARCHAR(64) NOT NULL,
  description VARCHAR(255) NOT NULL,
  script_name VARCHAR(255) NOT NULL,
  executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_${HISTORY_TABLE}_version (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;"

run_group "S" "${MIGRATION_SCHEMA_DIR}" "schema"
run_group "B" "${MIGRATION_BASELINE_DIR}" "seed-baseline"

if [[ "${INCLUDE_DEMO}" == "true" ]]; then
  if [[ -d "${MIGRATION_DEMO_DIR}" ]]; then
    run_group "D" "${MIGRATION_DEMO_DIR}" "seed-demo"
  else
    echo "ℹ️ seed-demo 目錄不存在，略過"
  fi
else
  echo "ℹ️ 預設略過 seed-demo（正式部署不執行 demo 資料）"
fi

echo "=========================================="
echo "✅ Invest migrations 完成"
echo "=========================================="
