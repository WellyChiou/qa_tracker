#!/bin/sh

# ============================================
# 資料庫自動備份腳本（容器內版本）
# ============================================
# 此腳本用於備份資料庫
# 用法：./backup-database-container.sh [db_name]
# 如果未指定 [db_name]，則預設備份 qa_tracker 和 church 兩個資料庫
# ============================================

# 日誌函數
log_info() {
    echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo "[ERROR] $(date '+%Y-%m-%d %H:%M:%S') - $1" >&2
}

log_warn() {
    echo "[WARN] $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# 配置參數
MYSQL_HOST=${MYSQL_HOST:-mysql}
MYSQL_PORT=${MYSQL_PORT:-3306}
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:-rootpassword}
BACKUP_DIR=${BACKUP_DIR:-/app/backups}
RETENTION_DAYS=${RETENTION_DAYS:-7}
BACKUP_ENABLED=${BACKUP_ENABLED:-true}

# 檢查是否啟用備份
if [ "$BACKUP_ENABLED" != "true" ]; then
    log_warn "自動備份已停用，跳過備份"
    exit 0
fi

# 創建備份目錄
mkdir -p "$BACKUP_DIR"

# 時間戳記
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')

# 備份單一資料庫函數
backup_database() {
    local db_name=$1
    local db_backup_dir="${BACKUP_DIR}/${db_name}"
    local backup_file="${db_backup_dir}/${db_name}_${TIMESTAMP}.sql"
    local compressed_file="${backup_file}.gz"
    
    log_info "開始備份資料庫: $db_name"
    
    # 檢查 mysqldump 命令是否存在
    if ! command -v mysqldump >/dev/null 2>&1; then
        log_error "mysqldump 命令不存在！請檢查 mysql-client 是否已安裝"
        return 1
    fi
    
    # 創建資料庫專屬備份目錄
    mkdir -p "$db_backup_dir"
    
    # 執行 mysqldump
    mysqldump -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u root -p"$MYSQL_ROOT_PASSWORD" \
        --skip-ssl \
        --single-transaction \
        --routines \
        --triggers \
        --events \
        "$db_name" > "$backup_file" 2>&1
    local exit_code=$?
    
    if [ $exit_code -ne 0 ]; then
        log_error "備份資料庫失敗: $db_name (mysqldump 退出碼: $exit_code)"
        
        # 嘗試讀取錯誤訊息
        if [ -s "$backup_file" ]; then
            log_error "錯誤訊息:"
            head -20 "$backup_file" | while IFS= read -r line || [ -n "$line" ]; do
                log_error "  $line"
            done
        fi
        
        rm -f "$backup_file"
        return 1
    fi
    
    # 檢查備份檔案是否為空
    if [ ! -s "$backup_file" ]; then
        log_error "備份檔案為空，可能備份失敗: $db_name"
        rm -f "$backup_file"
        return 1
    fi
    
    # 檢查是否包含關鍵錯誤
    if head -5 "$backup_file" | grep -qiE "mysqldump:.*(error|failed|unknown|access denied|cannot)" && ! head -5 "$backup_file" | grep -qi "Deprecated"; then
        log_error "備份文件包含錯誤訊息"
        rm -f "$backup_file"
        return 1
    fi
    
    # 壓縮備份檔案
    log_info "壓縮備份檔案..."
    if gzip "$backup_file"; then
        local file_size=$(du -h "$compressed_file" | cut -f1)
        log_info "備份成功: $compressed_file ($file_size)"
        return 0
    else
        log_error "壓縮備份檔案失敗: $backup_file"
        rm -f "$backup_file"
        return 1
    fi
}

# 清理舊備份
cleanup_old_backups() {
    log_info "清理 $RETENTION_DAYS 天前的舊備份..."
    
    local deleted_count=$(find "$BACKUP_DIR" -name "*.sql.gz" -type f -mtime +$RETENTION_DAYS 2>/dev/null | wc -l)
    
    if [ $deleted_count -gt 0 ]; then
        find "$BACKUP_DIR" -name "*.sql.gz" -type f -mtime +$RETENTION_DAYS -delete 2>/dev/null
        log_info "已刪除 $deleted_count 個舊備份檔案"
    else
        log_info "沒有需要清理的舊備份"
    fi
}

# 主程序
main() {
    local target_db=$1
    
    log_info "============================================"
    log_info "開始資料庫備份流程"
    log_info "============================================"
    
    if [ -n "$target_db" ]; then
        # 如果指定了資料庫，只備份該資料庫
        if backup_database "$target_db"; then
            log_info "$target_db 資料庫備份成功"
            cleanup_old_backups
            exit 0
        else
            log_error "$target_db 資料庫備份失敗"
            exit 1
        fi
    else
        # 如果未指定，備份預設的兩個資料庫
        log_info "未指定資料庫，執行全量備份"
        
        local success=true
        
        if ! backup_database "qa_tracker"; then
            success=false
        fi
        
        if ! backup_database "church"; then
            success=false
        fi
        
        cleanup_old_backups
        
        if [ "$success" = true ]; then
            log_info "所有資料庫備份完成"
            exit 0
        else
            log_error "部分資料庫備份失敗"
            exit 1
        fi
    fi
}

# 執行主程序，傳入第一個參數
main "$1"
