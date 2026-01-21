#!/usr/bin/env bash
set -euo pipefail

# =========================
# MySQL 資料庫還原腳本
# =========================
#
# 功能說明：
#   從 restore_database 資料夾中自動尋找最新的備份檔，並還原到指定的 MySQL 資料庫
#   支援多個資料庫（qa_tracker、church），會自動為每個資料庫選擇最新的備份檔
#
# 使用方法：
#   # 基本使用（使用預設值）
#   ./mysql_restore_database.sh
#
#   # 自訂 MySQL container 名稱
#   MYSQL_CONTAINER=my_mysql ./mysql_restore_database.sh
#
#   # 自訂備份資料夾
#   BACKUP_DIR=/path/to/backups ./mysql_restore_database.sh
#
#   # 自訂 MySQL 帳號密碼
#   MYSQL_USER=admin MYSQL_PASSWORD=mypassword ./mysql_restore_database.sh
#
#   # 不重建資料庫（保留現有資料，僅還原）
#   DROP_RECREATE=0 ./mysql_restore_database.sh
#
# 環境變數說明：
#   MYSQL_CONTAINER  - MySQL Docker container 名稱（預設：mysql_db）
#   MYSQL_USER       - MySQL 使用者名稱（預設：root）
#   MYSQL_PASSWORD   - MySQL 密碼（預設：rootpassword）
#   BACKUP_DIR       - 備份檔所在資料夾（預設：restore_database）
#   CHARSET          - 資料庫字元集（預設：utf8mb4）
#   COLLATION        - 資料庫排序規則（預設：utf8mb4_unicode_ci）
#   DROP_RECREATE    - 是否先刪除並重建資料庫（預設：1）
#                       1 = 先 DROP + CREATE（整庫乾淨還原，推薦）
#                       0 = 不重建（若 DB 已有表，可能失敗/半殘）
#
# 備份檔名格式要求：
#   腳本會自動尋找符合以下格式的備份檔：
#   - qa_tracker_YYYYMMDD_HHMMSS.sql.gz
#   - church_YYYYMMDD_HHMMSS.sql.gz
#   
#   例如：
#   - qa_tracker_20240115_143022.sql.gz
#   - church_20240115_143022.sql.gz
#
#   腳本會自動選擇每個資料庫的最新備份檔（依檔名日期時間排序）
#
# 注意事項：
#   1. 執行前請確認 MySQL container 正在運行
#   2. 預設會先刪除並重建資料庫（DROP_RECREATE=1），請確認資料已備份
#   3. 備份檔必須是 .sql.gz 格式（gzip 壓縮的 SQL 檔）
#   4. 需要具備 docker、gunzip、sed、grep、sort、head、ls 等指令
#   5. 需要對 MySQL container 有執行權限
#
# 範例：
#   # 還原到預設的 mysql_db container
#   ./mysql_restore_database.sh
#
#   # 還原到自訂 container，使用自訂備份資料夾
#   MYSQL_CONTAINER=my_mysql BACKUP_DIR=/backups ./mysql_restore_database.sh
#
#   # 僅還原 church 資料庫（需修改腳本中的 DBS 變數）
#   # 或使用環境變數覆蓋（需修改腳本支援）
#
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
