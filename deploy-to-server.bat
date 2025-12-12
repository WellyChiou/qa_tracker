@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

REM ==========================================
REM 一鍵部署腳本 (Windows 版本)
REM 功能：打包專案 -> 上傳到伺服器 -> 解壓 -> 執行部署
REM ==========================================

REM 配置
set SERVER_IP=38.54.89.136
set SERVER_USER=root
set "SERVER_PASSWORD=!Welly775"
set PROJECT_NAME=docker-vue-java-mysql
set REMOTE_PATH=/root/project/work
set ARCHIVE_NAME=%PROJECT_NAME%.tar.gz

REM 獲取當前目錄名稱（用於打包）
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%"
for %%I in ("%CD%") do set CURRENT_DIR=%%~nxI

REM 顏色輸出
set GREEN=[32m
set YELLOW=[33m
set RED=[31m
set NC=[0m

echo.
echo ==========================================
echo 開始一鍵部署流程
echo ==========================================
echo.

REM 步驟 1: 檢查必要工具
echo %YELLOW%步驟 1: 檢查必要工具...%NC%
echo.

REM 檢查 tar 命令
where tar >nul 2>&1
if %errorlevel% neq 0 (
    echo %RED%[ERROR] 未找到 tar 命令%NC%
    echo 請確保您已安裝 Git for Windows 並將其加入系統 PATH
    echo 下載網址: https://git-scm.com/download/win
    pause
    exit /b 1
)

REM 檢查 scp 命令
where scp >nul 2>&1
if %errorlevel% neq 0 (
    echo %RED%[ERROR] 未找到 scp 命令%NC%
    echo 請執行以下步驟：
    echo 1. 安裝 Git for Windows ^(如果尚未安裝^)
    echo    下載網址: https://git-scm.com/download/win
    echo 2. 或安裝 OpenSSH 客戶端
    echo    - 開啟 設定 ^> 應用程式 ^> 應用程式與功能 ^> 選擇性功能 ^> 新增功能
    echo    - 搜尋並安裝 "OpenSSH 用戶端"
    echo 3. 或使用 PuTTY 的 pscp.exe
    pause
    exit /b 1
)

echo %GREEN%[SUCCESS] 工具檢查完成%NC%
echo.

REM 步驟 2: 打包專案
echo %YELLOW%步驟 2: 打包專案...%NC%
echo.

cd /d "%SCRIPT_DIR%"

REM 刪除舊的打包文件
if exist "%ARCHIVE_NAME%" (
    echo 刪除舊的打包文件...
    del "%ARCHIVE_NAME%"
)

echo 正在打包專案...
cd /d "%SCRIPT_DIR%.."

tar --format=ustar -czf "%SCRIPT_DIR%\%ARCHIVE_NAME%" ^
    --exclude-vcs ^
    --exclude="%CURRENT_DIR%/.git" ^
    --exclude="%CURRENT_DIR%/node_modules" ^
    --exclude="%CURRENT_DIR%/target" ^
    --exclude="%CURRENT_DIR%/.DS_Store" ^
    --exclude="%CURRENT_DIR%/*.log" ^
    --exclude="%CURRENT_DIR%/frontend-personal/dist" ^
    --exclude="%CURRENT_DIR%/frontend-church/dist" ^
    --exclude="%CURRENT_DIR%/frontend-church-admin/dist" ^
    --exclude="%CURRENT_DIR%/*.tar.gz" ^
    "%CURRENT_DIR%"

cd /d "%SCRIPT_DIR%"

if %errorlevel% neq 0 (
    echo %RED%[ERROR] 打包失敗!%NC%
    pause
    exit /b 1
)

if not exist "%ARCHIVE_NAME%" (
    echo %RED%[ERROR] 打包失敗! 找不到歸檔文件%NC%
    pause
    exit /b 1
)

REM 獲取文件大小
for %%A in ("%ARCHIVE_NAME%") do set ARCHIVE_SIZE=%%~zA
set /a ARCHIVE_SIZE_KB=!ARCHIVE_SIZE!/1024
if !ARCHIVE_SIZE_KB! geq 1024 (
    set /a ARCHIVE_SIZE_MB=!ARCHIVE_SIZE_KB!/1024
    echo %GREEN%[SUCCESS] 打包完成! 文件大小: !ARCHIVE_SIZE_MB! MB%NC%
) else (
    echo %GREEN%[SUCCESS] 打包完成! 文件大小: !ARCHIVE_SIZE_KB! KB%NC%
)
echo.

REM 步驟 3: 上傳到伺服器
echo %YELLOW%步驟 3: 上傳到伺服器 (^!%SERVER_USER%^!@^!%SERVER_IP%^!)...%NC%
echo.

echo 創建遠程目錄...
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL %SERVER_USER%@%SERVER_IP% ^
    "mkdir -p %REMOTE_PATH% && mkdir -p %REMOTE_PATH%/%PROJECT_NAME%/logs/backend && chmod -R 777 %REMOTE_PATH%/%PROJECT_NAME%/logs"

if %errorlevel% neq 0 (
    echo.
    echo %RED%[ERROR] 無法連接到遠端伺服器%NC%
    echo 請確認：
    echo 1. 伺服器位址和帳號是否正確
    echo 2. 網路連線是否正常
    echo 3. 密碼是否正確
    pause
    exit /b 1
)

echo 上傳打包文件...
scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL "%ARCHIVE_NAME%" %SERVER_USER%@%SERVER_IP%:%REMOTE_PATH%/

if %errorlevel% neq 0 (
    echo.
    echo %RED%[ERROR] 上傳失敗!%NC%
    pause
    exit /b 1
)

echo %GREEN%[SUCCESS] 上傳成功!%NC%
echo.

REM 步驟 4: 在遠程服務器上解壓和部署
echo %YELLOW%步驟 4: 在遠程服務器上解壓和部署...%NC%
echo.

echo 上傳遠程部署腳本...
scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL "%SCRIPT_DIR%remote_deploy.sh" %SERVER_USER%@%SERVER_IP%:/tmp/

if %errorlevel% neq 0 (
    echo %RED%[ERROR] 上傳遠程腳本失敗%NC%
    pause
    exit /b 1
)

echo 執行遠程部署...
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL %SERVER_USER%@%SERVER_IP% ^
    "sed -i 's/\r$//' /tmp/remote_deploy.sh && chmod +x /tmp/remote_deploy.sh && bash /tmp/remote_deploy.sh && rm -f /tmp/remote_deploy.sh"

if %errorlevel% neq 0 (
    echo.
    echo %RED%[ERROR] 遠程部署失敗!%NC%
    pause
    exit /b 1
)

echo.
echo ==========================================
echo %GREEN%[SUCCESS] 一鍵部署完成!%NC%
echo ==========================================
echo.

echo 服務訪問地址：
echo   - 前端: http://power-light-church.duckdns.org 或 http://%SERVER_IP%
echo   - 後端 API: http://power-light-church.duckdns.org/api 或 http://%SERVER_IP%/api
echo.

echo HTTPS 設置：
echo   如果已設置 HTTPS，請使用：
echo   - 前端: https://power-light-church.duckdns.org
echo   - 後端 API: https://power-light-church.duckdns.org/api
echo   - LINE Bot Webhook: https://power-light-church.duckdns.org/api/line/webhook
echo.

echo   如需設置 HTTPS，請執行：
echo     ssh %SERVER_USER%@%SERVER_IP%
echo     cd %REMOTE_PATH%/%PROJECT_NAME%
echo     ./setup-https-on-server.sh
echo.

echo 查看服務狀態：
echo   ssh %SERVER_USER%@%SERVER_IP%
echo   cd %REMOTE_PATH%/%PROJECT_NAME%
echo   docker compose ps
echo.

echo %GREEN%[SUCCESS] 已自動配置預防機制：%NC%
echo   - 前端監控: 每 5 分鐘自動檢查並修復
echo   - 系統監控: 每小時檢查資源使用情況
echo   - 自動清理: 每天凌晨 2 點清理 Docker 資源
echo.

echo 查看監控日誌：
echo   # 查看最新的前端監控日誌
echo   ssh %SERVER_USER%@%SERVER_IP% 'ls -t /root/project/work/logs/frontend-monitor_*.log ^| head -1 ^| xargs tail -f'
echo   # 查看最新的系統監控日誌
echo   ssh %SERVER_USER%@%SERVER_IP% 'ls -t /root/project/work/logs/system-monitor_*.log ^| head -1 ^| xargs tail -f'
echo   # 查看最新的後端日誌
echo   ssh %SERVER_USER%@%SERVER_IP% 'ls -t /root/project/work/logs/backend/application*.log ^| head -1 ^| xargs tail -f'
echo.

pause
