#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKUPS_ROOT="$REPO_ROOT/backups"

MODE=""
SYSTEM="all"

usage() {
  cat <<'EOF'
用法:
  ./scripts/flatten-backups.sh --dry-run [--system invest|church|personal|all]
  ./scripts/flatten-backups.sh --execute [--system invest|church|personal|all]

說明:
  --dry-run   只顯示將要搬移的檔案，不會變更任何檔案
  --execute   實際搬移檔案（不覆蓋既有檔案）
  --system    指定系統，預設 all

注意:
  1. 只處理 .sql.gz
  2. 目的地同名檔案存在時會標記 CONFLICT 並跳過
  3. 本工具不刪除舊資料夾
EOF
}

log() {
  printf '%s\n' "$*"
}

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

parse_args() {
  if [ "$#" -eq 0 ]; then
    usage
    exit 1
  fi

  while [ "$#" -gt 0 ]; do
    case "$1" in
      --dry-run)
        if [ -n "$MODE" ]; then
          fail "不可同時指定 --dry-run 與 --execute"
        fi
        MODE="dry-run"
        ;;
      --execute)
        if [ -n "$MODE" ]; then
          fail "不可同時指定 --dry-run 與 --execute"
        fi
        MODE="execute"
        ;;
      --system)
        shift
        if [ "$#" -eq 0 ]; then
          fail "--system 需要參數"
        fi
        case "$1" in
          invest|church|personal|all)
            SYSTEM="$1"
            ;;
          *)
            fail "不支援的 --system: $1 (可用: invest|church|personal|all)"
            ;;
        esac
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        fail "未知參數: $1"
        ;;
    esac
    shift
  done

  if [ -z "$MODE" ]; then
    fail "請指定 --dry-run 或 --execute"
  fi
}

systems_to_process() {
  if [ "$SYSTEM" = "all" ]; then
    printf '%s\n' invest church personal
  else
    printf '%s\n' "$SYSTEM"
  fi
}

old_dir_for() {
  case "$1" in
    invest) printf '%s\n' "$BACKUPS_ROOT/invest/invest" ;;
    church) printf '%s\n' "$BACKUPS_ROOT/church/church" ;;
    personal) printf '%s\n' "$BACKUPS_ROOT/personal/qa_tracker" ;;
    *) fail "內部錯誤: 未知系統 $1" ;;
  esac
}

new_dir_for() {
  case "$1" in
    invest) printf '%s\n' "$BACKUPS_ROOT/invest" ;;
    church) printf '%s\n' "$BACKUPS_ROOT/church" ;;
    personal) printf '%s\n' "$BACKUPS_ROOT/personal" ;;
    *) fail "內部錯誤: 未知系統 $1" ;;
  esac
}

process_system() {
  local system="$1"
  local old_dir new_dir
  local total=0 moved=0 skipped=0 conflict=0

  old_dir="$(old_dir_for "$system")"
  new_dir="$(new_dir_for "$system")"

  log ""
  log "=== 系統: $system ==="
  log "舊路徑: $old_dir"
  log "新路徑: $new_dir"

  if [ ! -d "$old_dir" ]; then
    log "[SKIP] 舊路徑不存在，無需搬移"
    return 0
  fi

  mkdir -p "$new_dir"

  while IFS= read -r src; do
    total=$((total + 1))
    local filename target
    filename="$(basename "$src")"
    target="$new_dir/$filename"

    if [ -e "$target" ]; then
      conflict=$((conflict + 1))
      log "[CONFLICT] 已存在，跳過: $target"
      continue
    fi

    if [ "$MODE" = "dry-run" ]; then
      moved=$((moved + 1))
      log "[PLAN] $src -> $target"
    else
      mv "$src" "$target"
      moved=$((moved + 1))
      log "[MOVED] $src -> $target"
    fi
  done < <(find "$old_dir" -maxdepth 1 -type f -name '*.sql.gz' | sort)

  skipped=$((total - moved - conflict))
  log "摘要($system): total=$total moved=$moved conflict=$conflict skipped=$skipped mode=$MODE"
}

main() {
  parse_args "$@"

  if [ ! -d "$BACKUPS_ROOT" ]; then
    fail "找不到 backups 目錄: $BACKUPS_ROOT"
  fi

  log "開始執行 backup 舊雙層搬平工具"
  log "模式: $MODE"
  log "系統: $SYSTEM"
  log "repo: $REPO_ROOT"

  while IFS= read -r system; do
    process_system "$system"
  done < <(systems_to_process)

  log ""
  if [ "$MODE" = "dry-run" ]; then
    log "Dry-run 完成（未實際搬移任何檔案）"
  else
    log "Execute 完成"
  fi
}

main "$@"
