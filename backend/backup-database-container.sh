#!/bin/sh

# ============================================
# 資料庫自動備份腳本（容器內版本）
# ============================================
# 此腳本會備份 qa_tracker 和 church 兩個資料庫
# 備份檔案會自動壓縮並根據配置清理舊備份
# 此版本用於在容器內執行，直接連接 MySQL，不使用 docker compose exec
# ============================================

# 不使用 set -e，以便正確捕獲錯誤和退出碼

# 顏色定義（簡化版，因為 sh 可能不支援所有顏色）
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

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

# 配置參數（從環境變數讀取，如果沒有則使用預設值）
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

# 創建備份目錄（如果不存在）
mkdir -p "$BACKUP_DIR"

# 時間戳記
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')

# 備份函數（根據成功測試案例重寫）
backup_database() {
    local db_name=$1
    # 為每個資料庫創建專屬資料夾
    local db_backup_dir="${BACKUP_DIR}/${db_name}"
    local backup_file="${db_backup_dir}/${db_name}_${TIMESTAMP}.sql"
    local compressed_file="${backup_file}.gz"
    
    log_info "開始備份資料庫: $db_name"
    
    # 檢查 mysqldump 命令是否存在
    if ! command -v mysqldump >/dev/null 2>&1; then
        log_error "mysqldump 命令不存在！請檢查 mysql-client 是否已安裝"
        return 1
    fi
    
    # 創建資料庫專屬備份目錄（如果不存在）
    mkdir -p "$db_backup_dir"
    
    # 檢查備份目錄權限
    if [ ! -w "$db_backup_dir" ]; then
        log_error "備份目錄沒有寫入權限: $db_backup_dir"
        return 1
    fi
    
    # 根據成功測試案例，使用 2>&1 將錯誤和標準輸出合併
    log_info "執行 mysqldump 命令..."
    mysqldump -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u root -p"$MYSQL_ROOT_PASSWORD" \
        --skip-ssl \
        --single-transaction \
        --routines \
        --triggers \
        --events \
        "$db_name" > "$backup_file" 2>&1
    local exit_code=$?
    
    # 檢查備份是否成功
    if [ $exit_code -ne 0 ]; then
        log_error "備份資料庫失敗: $db_name (mysqldump 退出碼: $exit_code)"
        
        # 診斷信息：檢查備份文件狀態
        log_error "診斷信息:"
        if [ -f "$backup_file" ]; then
            local file_size=$(ls -lh "$backup_file" 2>/dev/null | awk '{print $5}' || echo "unknown")
            log_error "  備份文件存在，大小: $file_size"
            
            # 讀取並輸出錯誤訊息（錯誤訊息會寫入備份文件）
            if [ -s "$backup_file" ]; then
                log_error "錯誤訊息:"
                # 只讀取前50行錯誤訊息，避免輸出過多
                head -50 "$backup_file" | while IFS= read -r line || [ -n "$line" ]; do
                    log_error "  $line"
                done
            else
                log_error "  備份文件為空"
            fi
        else
            log_error "  備份文件不存在"
        fi
        
        # 如果備份文件為空或不存在，嘗試直接執行 mysqldump 獲取錯誤訊息
        if [ ! -f "$backup_file" ] || [ ! -s "$backup_file" ]; then
            log_error "嘗試直接執行 mysqldump 獲取錯誤訊息..."
            mysqldump -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u root -p"$MYSQL_ROOT_PASSWORD" \
                --skip-ssl \
                --single-transaction \
                --routines \
                --triggers \
                --events \
                "$db_name" 2>&1 | head -20 | while IFS= read -r line || [ -n "$line" ]; do
                log_error "  $line"
            done
        fi
        
        # 測試 MySQL 連接
        log_error "測試 MySQL 連接..."
        local test_output=$(mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u root -p"$MYSQL_ROOT_PASSWORD" --skip-ssl -e "SELECT 1;" 2>&1)
        local mysql_exit_code=$?
        if [ $mysql_exit_code -eq 0 ]; then
            log_error "  MySQL 連接測試成功"
        else
            log_error "  MySQL 連接測試失敗 (退出碼: $mysql_exit_code):"
            echo "$test_output" | while IFS= read -r line || [ -n "$line" ]; do
                log_error "    $line"
            done
        fi
        
        # 刪除失敗的備份文件
        rm -f "$backup_file"
        return 1
    fi
    
    # 檢查備份檔案是否為空
    if [ ! -s "$backup_file" ]; then
        log_error "備份檔案為空，可能備份失敗: $db_name"
        rm -f "$backup_file"
        return 1
    fi
    
    # 檢查備份文件是否包含真正的錯誤訊息（排除警告訊息）
    # mysqldump 的警告訊息（如 "Deprecated"）會寫入備份文件，但這不是錯誤
    # 真正的錯誤通常包含 "error"、"failed"、"unknown" 等關鍵字
    if head -5 "$backup_file" | grep -qiE "mysqldump:.*(error|failed|unknown|access denied|cannot)" && ! head -5 "$backup_file" | grep -qi "Deprecated"; then
        log_error "備份文件包含錯誤訊息:"
        head -20 "$backup_file" | while IFS= read -r line || [ -n "$line" ]; do
            log_error "  $line"
        done
        rm -f "$backup_file"
        return 1
    fi
    
    # 檢查備份文件是否包含有效的 SQL 內容（至少應該有 CREATE TABLE 或 INSERT INTO）
    if ! grep -qiE "(CREATE TABLE|INSERT INTO|DROP TABLE)" "$backup_file"; then
        log_error "備份文件不包含有效的 SQL 內容，可能備份失敗"
        head -20 "$backup_file" | while IFS= read -r line || [ -n "$line" ]; do
            log_error "  $line"
        done
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
    
    local deleted_count=0
    
    # 使用 find 命令遞迴清理舊備份（支援子資料夾，Alpine Linux 支援）
    # 先計算要刪除的檔案數量，然後執行刪除
        deleted_count=$(find "$BACKUP_DIR" -name "*.sql.gz" -type f -mtime +$RETENTION_DAYS 2>/dev/null | wc -l)
    if [ $deleted_count -gt 0 ]; then
        find "$BACKUP_DIR" -name "*.sql.gz" -type f -mtime +$RETENTION_DAYS -delete 2>/dev/null
        log_info "已刪除 $deleted_count 個舊備份檔案"
    else
        log_info "沒有需要清理的舊備份"
    fi
    
    if [ $deleted_count -gt 0 ]; then
        log_info "已刪除 $deleted_count 個舊備份檔案"
    else
        log_info "沒有需要清理的舊備份"
    fi
}

# 主程序
main() {
    log_info "============================================"
    log_info "開始資料庫備份流程"
    log_info "============================================"
    log_info "備份目錄: $BACKUP_DIR"
    log_info "保留天數: $RETENTION_DAYS"
    log_info "MySQL 主機: $MYSQL_HOST:$MYSQL_PORT"
    log_info "============================================"
    
    # 備份 qa_tracker 資料庫
    if backup_database "qa_tracker"; then
        QA_BACKUP_SUCCESS=true
    else
        QA_BACKUP_SUCCESS=false
    fi
    
    # 備份 church 資料庫
    if backup_database "church"; then
        CHURCH_BACKUP_SUCCESS=true
    else
        CHURCH_BACKUP_SUCCESS=false
    fi
    
    # 清理舊備份
    cleanup_old_backups
    
    # 總結
    log_info "============================================"
    if [ "$QA_BACKUP_SUCCESS" = true ] && [ "$CHURCH_BACKUP_SUCCESS" = true ]; then
        log_info "所有資料庫備份完成"
        exit 0
    else
        log_error "部分資料庫備份失敗"
        exit 1
    fi
}

# 執行主程序
main

