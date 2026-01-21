#!/bin/bash

# 清理 work 資料夾腳本
# 用於刪除遠程服務器上的 work 資料夾內容

# 配置（與 deploy-to-server.sh 保持一致）
SERVER_USER="root"
SERVER_HOST="38.54.89.136"
SERVER_PASSWORD="!Welly775"  # 與 deploy-to-server.sh 保持一致
WORK_DIR="/root/project/work"

# 檢查是否使用 sshpass
if command -v sshpass &> /dev/null; then
    SSH_CMD="sshpass -p '${SERVER_PASSWORD}' ssh"
else
    SSH_CMD="ssh"
    echo "提示：如果 SSH 需要密碼，請安裝 sshpass: brew install hudochenkov/sshpass/sshpass"
fi

echo "=========================================="
echo "清理 work 資料夾"
echo "=========================================="
echo ""
echo "⚠️  警告：此操作將刪除以下目錄的所有內容："
echo "   $WORK_DIR"
echo ""
read -p "確定要繼續嗎？(yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "操作已取消"
    exit 0
fi

echo ""
echo "正在連接到服務器並清理..."

# 執行清理（root 用戶不需要 sudo）
if command -v sshpass &> /dev/null; then
    sshpass -p "${SERVER_PASSWORD}" ssh -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_HOST} << 'EOF'
        WORK_DIR="/root/project/work"
        
        echo "當前 work 資料夾內容："
        ls -la "$WORK_DIR" 2>/dev/null || echo "資料夾不存在"
        
        echo ""
        echo "正在刪除 $WORK_DIR 下的所有內容..."
        
        # root 用戶直接刪除，不需要 sudo
        rm -rf "$WORK_DIR"/* 2>/dev/null || true
        rm -rf "$WORK_DIR"/.[!.]* 2>/dev/null || true  # 刪除隱藏文件（排除 . 和 ..）
        
        echo ""
        echo "清理完成！"
        echo "剩餘內容："
        ls -la "$WORK_DIR" 2>/dev/null || echo "資料夾為空或不存在"
EOF
else
    ssh -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_HOST} << 'EOF'
        WORK_DIR="/root/project/work"
        
        echo "當前 work 資料夾內容："
        ls -la "$WORK_DIR" 2>/dev/null || echo "資料夾不存在"
        
        echo ""
        echo "正在刪除 $WORK_DIR 下的所有內容..."
        
        # root 用戶直接刪除，不需要 sudo
        rm -rf "$WORK_DIR"/* 2>/dev/null || true
        rm -rf "$WORK_DIR"/.[!.]* 2>/dev/null || true  # 刪除隱藏文件（排除 . 和 ..）
        
        echo ""
        echo "清理完成！"
        echo "剩餘內容："
        ls -la "$WORK_DIR" 2>/dev/null || echo "資料夾為空或不存在"
EOF
fi

echo ""
echo "=========================================="
echo "清理完成"
echo "=========================================="

