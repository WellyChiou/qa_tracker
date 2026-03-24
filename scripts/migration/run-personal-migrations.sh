#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="docker-compose.yml"
MYSQL_SERVICE="mysql"
ENV_FILE=""
DB_NAME="qa_tracker"
HISTORY_TABLE="schema_migration_history"
PROJECT_NAME=""

usage() {
  cat <<'EOF'
用法：
  ./scripts/migration/run-personal-migrations.sh [options]

Options:
  --compose-file <path>    docker compose 檔案（預設: docker-compose.yml）
  --env-file <path>        docker compose env file（選填）
  --mysql-service <name>   MySQL service name（預設: mysql）
  --db-name <name>         目標資料庫（預設: qa_tracker）
  --project-name <name>    docker compose project name（選填）
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
    --project-name)
      PROJECT_NAME="${2:-}"
      shift 2
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

COMPOSE_ARGS=("-f" "${COMPOSE_FILE}")
if [[ -n "${ENV_FILE}" ]]; then
  COMPOSE_ARGS=(--env-file "${ENV_FILE}" "${COMPOSE_ARGS[@]}")
fi
if [[ -n "${PROJECT_NAME}" ]]; then
  COMPOSE_ARGS=("-p" "${PROJECT_NAME}" "${COMPOSE_ARGS[@]}")
fi

dc() {
  docker compose "${COMPOSE_ARGS[@]}" "$@"
}

mysql_exec() {
  local sql="$1"
  printf '%s\n' "${sql}" | dc exec -T "${MYSQL_SERVICE}" sh -lc 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD"'
}

mysql_query_scalar() {
  local sql="$1"
  printf '%s\n' "${sql}" | dc exec -T "${MYSQL_SERVICE}" sh -lc 'mysql --default-character-set=utf8mb4 -N -uroot -p"$MYSQL_ROOT_PASSWORD"' | tr -d '\r' | head -n1
}

mysql_run_file() {
  local file="$1"
  cat "${file}" | dc exec -T "${MYSQL_SERVICE}" sh -lc 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD"'
}

wait_mysql_ready() {
  local max_retry=30
  local retry=1

  while [[ "${retry}" -le "${max_retry}" ]]; do
    if dc exec -T "${MYSQL_SERVICE}" sh -lc 'mysqladmin ping -h localhost -uroot -p"$MYSQL_ROOT_PASSWORD" --silent' >/dev/null 2>&1; then
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

escape_sql() {
  local value="$1"
  echo "${value//\'/\'\'}"
}

run_one() {
  local version="$1"
  local script_name="$2"
  local file="${PROJECT_ROOT}/${script_name}"

  if [[ ! -f "${file}" ]]; then
    echo "❌ migration 檔案不存在：${script_name}"
    exit 1
  fi

  local escaped_version
  escaped_version="$(escape_sql "${version}")"
  local executed_count
  executed_count="$(mysql_query_scalar "USE ${DB_NAME}; SELECT COUNT(1) FROM ${HISTORY_TABLE} WHERE version='${escaped_version}';")"
  executed_count="${executed_count:-0}"

  if [[ "${executed_count}" != "0" ]]; then
    echo "⏭️  跳過已執行：${version} (${script_name})"
    return 0
  fi

  echo "▶ 執行：${version} (${script_name})"
  if ! mysql_run_file "${file}"; then
    echo "❌ migration 失敗：${script_name}"
    exit 1
  fi

  local escaped_description
  escaped_description="$(escape_sql "$(basename "${script_name}")")"
  local escaped_script
  escaped_script="$(escape_sql "${script_name}")"
  mysql_exec "USE ${DB_NAME}; INSERT INTO ${HISTORY_TABLE}(version, description, script_name) VALUES ('${escaped_version}', '${escaped_description}', '${escaped_script}');"
  echo "✅ 完成：${version}"
}

echo "=========================================="
echo "🧱 Personal Migration Runner"
echo "Compose file : ${COMPOSE_FILE}"
if [[ -n "${ENV_FILE}" ]]; then
  echo "Env file     : ${ENV_FILE}"
fi
echo "MySQL svc    : ${MYSQL_SERVICE}"
echo "DB           : ${DB_NAME}"
echo "=========================================="

cd "${PROJECT_ROOT}"
wait_mysql_ready

mysql_exec "CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ${DB_NAME};
CREATE TABLE IF NOT EXISTS ${HISTORY_TABLE} (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  version VARCHAR(64) NOT NULL,
  description VARCHAR(255) NOT NULL,
  script_name VARCHAR(255) NOT NULL,
  executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_${HISTORY_TABLE}_version (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;"

# 注意：只納入已驗證可重複執行且屬 baseline 的 personal SQL
run_one "P001" "database/personal/schema/schema.sql"
run_one "P002" "database/personal/setup/personal-admin-setup.sql"
run_one "P003" "database/personal/permissions/personal-permissions.sql"
run_one "P004" "database/personal/migrations/20260317_fix_url_permissions.sql"

echo "=========================================="
echo "✅ Personal migrations 完成"
echo "=========================================="
