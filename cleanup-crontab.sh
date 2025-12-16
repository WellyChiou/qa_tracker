#!/bin/bash
# 清理 crontab 中的重複任務
# 移除所有舊的「自動部署腳本設置的監控任務」區塊和重複的日誌清理任務

set -e

echo "=========================================="
echo "開始清理 crontab 重複任務"
echo "=========================================="

# 讀取當前 crontab
CURRENT_CRON=$(crontab -l 2>/dev/null || echo "")

if [ -z "$CURRENT_CRON" ]; then
    echo "⚠️  crontab 為空，無需清理"
    exit 0
fi

# 備份原始 crontab
BACKUP_FILE="/tmp/crontab_backup_$(date +%Y%m%d_%H%M%S).txt"
echo "$CURRENT_CRON" > "$BACKUP_FILE"
echo "✅ 已備份原始 crontab 到: $BACKUP_FILE"
echo ""

# 使用 Python 來可靠地處理
cat > /tmp/cleanup_cron.py << 'PYEOF'
#!/usr/bin/env python3
import sys
import re

def clean_crontab(content):
    lines = content.rstrip().split('\n')
    result = []
    i = 0
    in_managed_block = False
    
    while i < len(lines):
        line = lines[i]
        
        # 檢測 managed 區塊開始
        if '# BEGIN project-work (managed)' in line:
            in_managed_block = True
            result.append(line)
            i += 1
            continue
        
        # 檢測 managed 區塊結束
        if '# END project-work (managed)' in line:
            in_managed_block = False
            result.append(line)
            i += 1
            continue
        
        # 在 managed 區塊中，保留所有內容
        if in_managed_block:
            result.append(line)
            i += 1
            continue
        
        # 不在 managed 區塊中，檢查是否為舊區塊
        # 檢測舊區塊開始：分隔線
        if re.match(r'^#\s*=+\s*$', line.strip()):
            # 檢查接下來的幾行是否包含舊區塊標記
            lookahead = i + 1
            found_old_marker = False
            while lookahead < len(lines) and lookahead <= i + 5:
                if lookahead < len(lines) and '自動部署腳本設置的監控任務' in lines[lookahead]:
                    found_old_marker = True
                    break
                # 如果遇到非註釋的 cron 任務，說明不是舊區塊
                if lookahead < len(lines) and lines[lookahead].strip() and not lines[lookahead].strip().startswith('#'):
                    if re.match(r'^[0-9\*\/]', lines[lookahead].strip()):
                        break
                lookahead += 1
            
            if found_old_marker:
                # 找到舊區塊，跳過整個區塊
                # 跳過開始的分隔線
                i += 1
                # 跳過區塊內容直到下一個分隔線或區塊結束
                while i < len(lines):
                    current = lines[i]
                    # 檢測區塊結束：另一個分隔線
                    if re.match(r'^#\s*=+\s*$', current.strip()):
                        # 跳過結束的分隔線
                        i += 1
                        break
                    # 跳過區塊中的所有行
                    i += 1
                continue
        
        # 不在 managed 區塊中，檢查是否為重複的項目任務
        # 這些任務應該只在 managed 區塊中存在
        if not in_managed_block:
            # 日誌清理任務
            if re.match(r'^0\s+3\s+\*\s+\*\s+\*\s+find.*\.log.*-mtime\s+\+7\s+-delete', line):
                # 跳過（managed 區塊中會有一個）
                i += 1
                continue
            
            # 前端監控任務
            if 'monitor-frontend.sh' in line:
                # 跳過（managed 區塊中會有一個）
                i += 1
                continue
            
            # 系統監控任務
            if 'monitor-system.sh' in line:
                # 跳過（managed 區塊中會有一個）
                i += 1
                continue
            
            # Docker 清理任務
            if 'docker system prune' in line or 'cleanup-docker.sh' in line:
                # 跳過（managed 區塊中會有一個）
                i += 1
                continue
        
        # 其他行（非項目相關的任務），正常添加
        result.append(line)
        i += 1
    
    return '\n'.join(result)

if __name__ == '__main__':
    content = sys.stdin.read()
    cleaned = clean_crontab(content)
    print(cleaned)
PYEOF

chmod +x /tmp/cleanup_cron.py

# 檢查是否有 Python
if ! command -v python3 &> /dev/null; then
    echo "❌ 錯誤: 需要 python3 來執行清理腳本"
    echo "請先安裝: apt-get install python3 或 yum install python3"
    exit 1
fi

# 執行清理
CLEANED_CRON=$(echo "$CURRENT_CRON" | python3 /tmp/cleanup_cron.py)

# 檢查清理後的 crontab 是否為空或只有註釋
if [ -z "$CLEANED_CRON" ] || [ -z "$(echo "$CLEANED_CRON" | grep -v '^#' | grep -v '^$')" ]; then
    echo "⚠️  警告: 清理後的 crontab 為空，保留原始 crontab"
    rm -f "$BACKUP_FILE" /tmp/cleanup_cron.py
    exit 0
fi

# 安裝清理後的 crontab
echo "$CLEANED_CRON" | crontab -

# 清理臨時文件
rm -f /tmp/cleanup_cron.py

echo "✅ crontab 清理完成！"
echo ""
echo "📋 清理後的 crontab："
crontab -l
echo ""
echo "💾 備份文件: $BACKUP_FILE"
echo ""
echo "如果發現問題，可以使用以下命令恢復："
echo "  crontab $BACKUP_FILE"
