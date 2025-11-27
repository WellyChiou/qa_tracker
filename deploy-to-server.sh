#!/bin/bash

# 一鍵部署腳本
# 功能：打包專案 -> 上傳到虛擬主機 -> 解壓 -> 執行部署

set -e  # 遇到錯誤立即退出

# 配置
SERVER_IP="38.54.89.136"
SERVER_USER="root"
SERVER_PASSWORD="!Welly775"
PROJECT_NAME="docker-vue-java-mysql"
REMOTE_PATH="/root/project/work"
ARCHIVE_NAME="${PROJECT_NAME}.tar.gz"

# 顏色輸出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=========================================="
echo "開始一鍵部署流程"
echo "==========================================${NC}"

# 步驟 1: 檢查必要工具
echo ""
echo -e "${YELLOW}步驟 1: 檢查必要工具...${NC}"

# 檢查 sshpass 是否安裝
if ! command -v sshpass &> /dev/null; then
    echo -e "${YELLOW}⚠️  sshpass 未安裝，嘗試安裝...${NC}"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        if command -v brew &> /dev/null; then
            echo "使用 brew 安裝 sshpass..."
            brew install hudochenkov/sshpass/sshpass
        else
            echo -e "${RED}❌ 請先安裝 Homebrew，然後執行: brew install hudochenkov/sshpass/sshpass${NC}"
            echo "或者手動安裝 sshpass 後重新執行此腳本"
            exit 1
        fi
    else
        # Linux
        echo "使用 apt/yum 安裝 sshpass..."
        sudo apt-get install -y sshpass 2>/dev/null || sudo yum install -y sshpass 2>/dev/null || {
            echo -e "${RED}❌ 無法自動安裝 sshpass，請手動安裝後重新執行${NC}"
            exit 1
        }
    fi
fi

echo -e "${GREEN}✅ 工具檢查完成${NC}"

# 步驟 2: 打包專案
echo ""
echo -e "${YELLOW}步驟 2: 打包專案...${NC}"

# 獲取專案根目錄
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# 刪除舊的打包文件
if [ -f "../${ARCHIVE_NAME}" ]; then
    echo "刪除舊的打包文件..."
    rm "../${ARCHIVE_NAME}"
fi

# 打包專案（排除不需要的文件，使用兼容格式）
echo "正在打包專案..."
# 使用 --format=ustar 確保 Linux 兼容性，並確保目錄結構正確
cd ..
tar --format=ustar -czf "${ARCHIVE_NAME}" \
    --exclude="${PROJECT_NAME}/.git" \
    --exclude="${PROJECT_NAME}/node_modules" \
    --exclude="${PROJECT_NAME}/target" \
    --exclude="${PROJECT_NAME}/.DS_Store" \
    --exclude="${PROJECT_NAME}/*.log" \
    --exclude="${PROJECT_NAME}/frontend/dist" \
    "${PROJECT_NAME}/"
cd "${PROJECT_NAME}"

if [ ! -f "../${ARCHIVE_NAME}" ]; then
    echo -e "${RED}❌ 打包失敗！${NC}"
    exit 1
fi

ARCHIVE_SIZE=$(du -h "../${ARCHIVE_NAME}" | cut -f1)
echo -e "${GREEN}✅ 打包完成！文件大小: ${ARCHIVE_SIZE}${NC}"

# 步驟 3: 上傳到虛擬主機
echo ""
echo -e "${YELLOW}步驟 3: 上傳到虛擬主機 (${SERVER_USER}@${SERVER_IP})...${NC}"

# 創建遠程目錄（如果不存在）
echo "創建遠程目錄..."
sshpass -p "${SERVER_PASSWORD}" ssh -o StrictHostKeyChecking=no "${SERVER_USER}@${SERVER_IP}" \
    "mkdir -p ${REMOTE_PATH}"

# 上傳文件
echo "上傳打包文件..."
sshpass -p "${SERVER_PASSWORD}" scp -o StrictHostKeyChecking=no \
    "../${ARCHIVE_NAME}" \
    "${SERVER_USER}@${SERVER_IP}:${REMOTE_PATH}/"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ 上傳成功！${NC}"
else
    echo -e "${RED}❌ 上傳失敗！${NC}"
    exit 1
fi

# 步驟 4: 在遠程服務器上解壓和部署
echo ""
echo -e "${YELLOW}步驟 4: 在遠程服務器上解壓和部署...${NC}"

# 執行遠程命令
sshpass -p "${SERVER_PASSWORD}" ssh -o StrictHostKeyChecking=no "${SERVER_USER}@${SERVER_IP}" bash << EOF
    set -e
    
    REMOTE_PATH="${REMOTE_PATH}"
    PROJECT_NAME="${PROJECT_NAME}"
    ARCHIVE_NAME="${ARCHIVE_NAME}"
    
    cd "\$REMOTE_PATH"
    
    # 備份證書目錄（如果存在）
    CERT_BACKUP_DIR="/tmp/\${PROJECT_NAME}_cert_backup"
    if [ -d "\$PROJECT_NAME/certbot/conf" ]; then
        echo "備份 SSL 證書..."
        rm -rf "\$CERT_BACKUP_DIR"
        cp -r "\$PROJECT_NAME/certbot/conf" "\$CERT_BACKUP_DIR" 2>/dev/null || true
        echo "✅ 證書已備份"
    fi
    
    # 備份舊目錄（如果存在）
    if [ -d "\$PROJECT_NAME" ]; then
        echo "備份現有目錄..."
        if [ -d "\${PROJECT_NAME}_backup" ]; then
            rm -rf "\${PROJECT_NAME}_backup"
        fi
        mv "\$PROJECT_NAME" "\${PROJECT_NAME}_backup"
    fi
    
    # 解壓新文件（忽略 macOS 擴展屬性警告）
    echo "解壓文件..."
    tar -xzf "\$ARCHIVE_NAME" 2>&1 | grep -v "Ignoring unknown extended header" || true
    
    # 檢查解壓後的目錄結構
    echo "檢查解壓結果..."
    ls -la
    
    # 進入專案目錄（解壓後應該有 docker-vue-java-mysql 目錄）
    if [ -d "\$PROJECT_NAME" ]; then
        cd "\$PROJECT_NAME"
    else
        echo "錯誤：解壓後找不到 \$PROJECT_NAME 目錄"
        echo "當前目錄內容:"
        ls -la
        exit 1
    fi
    
    echo "當前目錄: \$(pwd)"
    
    # 恢復證書目錄（如果備份存在且新目錄中沒有證書）
    if [ -d "\$CERT_BACKUP_DIR" ] && [ ! -d "certbot/conf/live" ]; then
        echo "恢復 SSL 證書..."
        mkdir -p certbot/conf
        cp -r "\$CERT_BACKUP_DIR"/* certbot/conf/ 2>/dev/null || true
        echo "✅ 證書已恢復"
    fi
    
    # 檢查 deploy.sh 是否存在
    if [ ! -f "deploy.sh" ]; then
        echo "錯誤：找不到 deploy.sh"
        echo "當前目錄內容:"
        ls -la
        exit 1
    fi
    
    # 給 deploy.sh 執行權限
    echo "設置 deploy.sh 執行權限..."
    chmod +x deploy.sh
    
    # 執行部署腳本
    echo "執行部署腳本..."
    ./deploy.sh
    
    echo "✅ 遠程部署完成！"
EOF

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}=========================================="
    echo "✅ 一鍵部署完成！"
    echo "==========================================${NC}"
    echo ""
    echo "服務訪問地址："
    echo "  - 前端: http://wc-project.duckdns.org 或 http://${SERVER_IP}"
    echo "  - 後端 API: http://wc-project.duckdns.org/api 或 http://${SERVER_IP}/api"
    echo ""
    echo "⚠️  HTTPS 設置："
    echo "  如果已設置 HTTPS，請使用："
    echo "  - 前端: https://wc-project.duckdns.org"
    echo "  - 後端 API: https://wc-project.duckdns.org/api"
    echo "  - LINE Bot Webhook: https://wc-project.duckdns.org/api/line/webhook"
    echo ""
    echo "  如需設置 HTTPS，請執行："
    echo "    ssh ${SERVER_USER}@${SERVER_IP}"
    echo "    cd ${REMOTE_PATH}/${PROJECT_NAME}"
    echo "    ./setup-https-on-server.sh"
    echo ""
    echo "查看服務狀態："
    echo "  ssh ${SERVER_USER}@${SERVER_IP}"
    echo "  cd ${REMOTE_PATH}/${PROJECT_NAME}"
    echo "  docker compose ps"
    echo ""
else
    echo -e "${RED}❌ 遠程部署失敗！${NC}"
    exit 1
fi

