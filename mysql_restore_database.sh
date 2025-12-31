#!/usr/bin/env bash
set -euo pipefail

# =========================
# Config (可用環境變數覆蓋)
# =========================
MYSQL_CONTAINER="${MYSQL_CONTAINER:-mysql_db}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-rootpassword}"

# 備份資料夾：固定讀 restore_database（可覆蓋）
BACKUP_DIR="${BACKUP_DIR:-restore_database}"

CHARSET="${CHARSET:-utf8mb4}"
COLLATION="${COLLATION:-utf8mb4_unicode_ci}"

# 1 = 先 DROP + CREATE（整庫乾淨還原，推薦）
# 0 = 不重建（若 DB 已有表，可能失敗/半殘）
DROP_RECREATE="${DROP_RECREATE:-1}"

# 要處理的 DB（可自行增減）
DBS="qa_tracker church"

# =========================
# Helpers
# =========================
die() { echo "❌ $*" >&2; exit 1; }
info() { echo "✅ $*"; }
warn() { echo "⚠️  $*" >&2; }

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "找不到指令：$1"
}

docker_mysql_exec() {
  docker exec -i "$MYSQL_CONTAINER" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$@"
}

get_prefix() {
  # 依 db 名稱回傳檔案前綴
  case "$1" in
    qa_tracker) echo "qa_tracker_" ;;
    church)     echo "church_" ;;
    *)          return 1 ;;
  esac
}

pick_latest_gz_by_prefix() {
  # 依 prefix 在 BACKUP_DIR 下挑最新檔（依檔名日期排序）
  # 檔名格式：<prefix><YYYYMMDD>_<HHMMSS>.sql.gz
  prefix="$1"

  # 先列出候選（不存在就回傳空字串）
  # macOS bash 3.2 下用 ls + grep + sort 最穩
  file="$(ls -1 "$BACKUP_DIR/${prefix}"*.sql.gz 2>/dev/null \
    | grep -E "/${prefix}[0-9]{8}_[0-9]{6}\.sql\.gz$" \
    | sort -r \
    | head -n 1 || true)"

  echo "$file"
}

drop_recreate_db() {
  db="$1"
  info "重建資料庫：$db"
  docker_mysql_exec -e "
    DROP DATABASE IF EXISTS \`$db\`;
    CREATE DATABASE \`$db\` CHARACTER SET $CHARSET COLLATE $COLLATION;
  "
}

restore_db_from_gz() {
  gz="$1"
  db="$2"

  info "開始還原：$db <= $gz"
  gunzip -c "$gz" \
    | sed '/^mysqldump: \[Warning\]/d' \
    | docker exec -i "$MYSQL_CONTAINER" \
        mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --default-character-set="$CHARSET" "$db"
  info "還原完成：$db"
}

verify_tables() {
  db="$1"
  docker_mysql_exec -e "SELECT '$db' AS db, COUNT(*) AS tables
                        FROM information_schema.tables
                        WHERE table_schema='$db';"
}

# =========================
# Main
# =========================
need_cmd docker
need_cmd gunzip
need_cmd sed
need_cmd grep
need_cmd sort
need_cmd head
need_cmd ls

[[ -d "$BACKUP_DIR" ]] || die "找不到備份資料夾：$BACKUP_DIR"

# 檢查 container 是否存在
docker ps --format '{{.Names}}' | grep -qx "$MYSQL_CONTAINER" \
  || die "找不到正在執行的 container：$MYSQL_CONTAINER（請確認 MYSQL_CONTAINER 變數）"

info "使用 MySQL container：$MYSQL_CONTAINER"
info "備份資料夾：$BACKUP_DIR"
info "掃描：qa_tracker_*.sql.gz / church_*.sql.gz（各自挑最新）"

restored_any=0

for db in $DBS; do
  prefix="$(get_prefix "$db")" || die "未知的 db：$db"
  latest_gz="$(pick_latest_gz_by_prefix "$prefix")"

  if [[ -z "$latest_gz" ]]; then
    warn "找不到 $db 的備份（格式需符合 ${prefix}YYYYMMDD_HHMMSS.sql.gz），跳過"
    continue
  fi

  info "找到 $db 最新備份：$latest_gz"

  if [[ "$DROP_RECREATE" = "1" ]]; then
    warn "DROP_RECREATE=1：將先刪除並重建資料庫 $db"
    drop_recreate_db "$db"
  else
    warn "DROP_RECREATE=0：不重建 $db（若已存在表，可能失敗/半殘）"
  fi

  restore_db_from_gz "$latest_gz" "$db"
  info "驗證 $db table 數量："
  verify_tables "$db"

  restored_any=1
done

[[ "$restored_any" = "1" ]] || die "沒有找到任何可用備份檔（請確認 $BACKUP_DIR 內檔名格式）"

info "全部完成 🎉"
