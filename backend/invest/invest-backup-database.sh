#!/bin/sh

# ============================================
# Invest 系統資料庫手動備份腳本（容器內版本）
# ============================================

log_info() {
    echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo "[ERROR] $(date '+%Y-%m-%d %H:%M:%S') - $1" >&2
}

log_warn() {
    echo "[WARN] $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

MYSQL_HOST=${MYSQL_HOST:-mysql}
MYSQL_PORT=${MYSQL_PORT:-3306}
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:-rootpassword}
BACKUP_DIR=${BACKUP_DIR:-/app/backups}
RETENTION_DAYS=${RETENTION_DAYS:-7}
BACKUP_ENABLED=${BACKUP_ENABLED:-true}
DATABASE_NAME=${DATABASE_NAME:-invest}

if [ "$BACKUP_ENABLED" != "true" ]; then
    log_warn "備份功能已停用，跳過備份"
    exit 0
fi

mkdir -p "$BACKUP_DIR"
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')

backup_database() {
    local db_name=$1
    # BACKUP_DIR 已由 compose 指向系統專屬目錄（例如 ./backups/invest），
    # 檔案直接落在 BACKUP_DIR，避免產生 invest/invest 雙層路徑。
    local backup_file="${BACKUP_DIR}/${db_name}_${TIMESTAMP}.sql"
    local compressed_file="${backup_file}.gz"

    log_info "開始備份資料庫: $db_name"

    if ! command -v mysqldump >/dev/null 2>&1; then
        log_error "mysqldump 命令不存在，請確認已安裝 mysql-client"
        return 1
    fi

    if [ ! -w "$BACKUP_DIR" ]; then
        log_error "備份目錄無寫入權限: $BACKUP_DIR"
        return 1
    fi

    mysqldump -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u root -p"$MYSQL_ROOT_PASSWORD" \
        --ssl-mode=DISABLED \
        --single-transaction \
        --routines \
        --triggers \
        --events \
        "$db_name" > "$backup_file" 2>&1
    local exit_code=$?

    if [ $exit_code -ne 0 ]; then
        log_error "備份失敗: $db_name (exit=$exit_code)"
        if [ -s "$backup_file" ]; then
            log_error "錯誤輸出:"
            head -50 "$backup_file" | while IFS= read -r line || [ -n "$line" ]; do
                log_error "  $line"
            done
        fi
        rm -f "$backup_file"
        return 1
    fi

    if [ ! -s "$backup_file" ]; then
        log_error "備份檔案為空: $backup_file"
        rm -f "$backup_file"
        return 1
    fi

    if ! grep -qiE "(CREATE TABLE|INSERT INTO|DROP TABLE)" "$backup_file"; then
        log_error "備份檔案不含有效 SQL 內容"
        head -20 "$backup_file" | while IFS= read -r line || [ -n "$line" ]; do
            log_error "  $line"
        done
        rm -f "$backup_file"
        return 1
    fi

    if gzip "$backup_file"; then
        local size
        size=$(du -h "$compressed_file" | cut -f1)
        log_info "備份成功: $compressed_file ($size)"
        return 0
    else
        log_error "壓縮備份檔案失敗: $backup_file"
        rm -f "$backup_file"
        return 1
    fi
}

cleanup_old_backups() {
    log_info "清理 ${RETENTION_DAYS} 天前舊備份"

    local deleted_count
    deleted_count=$(find "$BACKUP_DIR" -name "*.sql.gz" -type f -mtime +$RETENTION_DAYS 2>/dev/null | wc -l)

    if [ "$deleted_count" -gt 0 ]; then
        find "$BACKUP_DIR" -name "*.sql.gz" -type f -mtime +$RETENTION_DAYS -delete 2>/dev/null
        log_info "已刪除 $deleted_count 份舊備份"
    else
        log_info "沒有舊備份需要清理"
    fi
}

main() {
    log_info "============================================"
    log_info "開始 Invest 系統資料庫備份"
    log_info "備份目錄: $BACKUP_DIR"
    log_info "保留天數: $RETENTION_DAYS"
    log_info "MySQL: $MYSQL_HOST:$MYSQL_PORT"
    log_info "資料庫: $DATABASE_NAME"
    log_info "============================================"

    if backup_database "$DATABASE_NAME"; then
        BACKUP_SUCCESS=true
    else
        BACKUP_SUCCESS=false
    fi

    cleanup_old_backups

    if [ "$BACKUP_SUCCESS" = true ]; then
        log_info "資料庫備份完成"
        exit 0
    else
        log_error "資料庫備份失敗"
        exit 1
    fi
}

main
