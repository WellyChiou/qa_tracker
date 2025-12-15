@echo off
REM Set code page to UTF-8
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

echo ==========================================
echo 開始一鍵部署流程
echo ==========================================
echo.

echo ==========================================
echo Starting deployment process
echo ==========================================
echo.

REM 步驟 1: 檢查必要工具
echo 步驟 1: 檢查必要工具...
echo.

REM 檢查 tar 命令 (Windows 10 1803+ 內建)
where tar >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到 tar 命令
    echo 請確保您已安裝 Git for Windows 並將其加入系統 PATH
    echo 下載網址: https://git-scm.com/download/win
    pause
    exit /b 1
)

REM 檢查 scp 命令 (Windows 10 1809+ OpenSSH 內建)
where scp >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到 scp 命令
    echo 請執行以下步驟：
    echo 1. 安裝 Git for Windows ^(如果尚未安裝^)
    echo    下載網址: https://git-scm.com/download/win
    echo 2. 或安裝 OpenSSH 客戶端：
    echo    - 開啟 設定 ^> 應用程式 ^> 應用程式與功能 ^> 選擇性功能 ^> 新增功能
    echo    - 搜尋並安裝 "OpenSSH 用戶端"
    echo 3. 或使用 PuTTY 的 pscp.exe ^(請確保在系統 PATH 中^)
    pause
    exit /b 1
)

echo [SUCCESS] 工具檢查完成
echo.

REM 步驟 2: 打包專案
echo 步驟 2: 打包專案...
echo.

REM 確保在腳本目錄
cd /d "%SCRIPT_DIR%"

REM 刪除舊的打包文件
if exist "%ARCHIVE_NAME%" (
    echo 刪除舊的打包文件: %ARCHIVE_NAME%
    del "%ARCHIVE_NAME%"
)

echo 正在打包專案...
echo 來源目錄: %CURRENT_DIR%
echo 輸出檔案: %ARCHIVE_NAME%
echo.

REM 切換到上一層目錄進行打包
cd /d "%SCRIPT_DIR%.."

REM 使用 tar 打包目錄 (排除不需要的檔案和目錄)
tar -czf "%SCRIPT_DIR%\%ARCHIVE_NAME%" ^
    --exclude="%CURRENT_DIR%/.git" ^
    --exclude="%CURRENT_DIR%/node_modules" ^
    --exclude="%CURRENT_DIR%/target" ^
    --exclude="%CURRENT_DIR%/.DS_Store" ^
    --exclude="%CURRENT_DIR%/*.log" ^
    --exclude="%CURRENT_DIR%/frontend/dist" ^
    --exclude="%CURRENT_DIR%/frontend-personal/dist" ^
    --exclude="%CURRENT_DIR%/*.tar.gz" ^
    "%CURRENT_DIR%"

REM 返回原始目錄
cd /d "%SCRIPT_DIR%"

if %errorlevel% neq 0 (
    echo [ERROR] 打包失敗!
    pause
    exit /b 1
)

if not exist "%ARCHIVE_NAME%" (
    echo [ERROR] 打包失敗! 找不到歸檔文件
    pause
    exit /b 1
)

REM 獲取文件大小
for %%A in ("%ARCHIVE_NAME%") do set ARCHIVE_SIZE=%%~zA
set /a ARCHIVE_SIZE_KB=!ARCHIVE_SIZE!/1024
if !ARCHIVE_SIZE_KB! geq 1024 (
    set /a ARCHIVE_SIZE_MB=!ARCHIVE_SIZE_KB!/1024
    echo [SUCCESS] 打包完成! 文件大小: !ARCHIVE_SIZE_MB! MB
) else (
    echo [SUCCESS] 打包完成! 文件大小: !ARCHIVE_SIZE_KB! KB
)
echo.

REM 步驟 3: 上傳到伺服器
echo 步驟 3: 上傳到伺服器
echo 伺服器: %SERVER_USER%@%SERVER_IP%
echo.

REM 創建遠程目錄（如果不存在）
echo 創建遠程目錄...
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL %SERVER_USER%@%SERVER_IP% "mkdir -p %REMOTE_PATH%"

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] 無法連接到遠端伺服器
    echo 請確認：
    echo 1. 伺服器位址和帳號是否正確
    echo 2. 網路連線是否正常
    echo 3. 密碼是否正確
    pause
    exit /b 1
)

REM 上傳文件
echo 上傳打包文件...
echo 請輸入密碼: %SERVER_PASSWORD%
echo.

REM 使用 scp 上傳 (需要 SSH 金鑰或手動輸入密碼)
REM 注意: Windows 的 scp 不支援在命令列中直接傳遞密碼
REM 建議使用 SSH 金鑰認證

scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL "%ARCHIVE_NAME%" %SERVER_USER%@%SERVER_IP%:%REMOTE_PATH%/

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] 上傳失敗!
    echo.
    echo 提示: 如果提示輸入密碼，請輸入: %SERVER_PASSWORD%
    echo 或者配置 SSH 金鑰認證以避免輸入密碼
    echo.
    pause
    exit /b 1
)

echo [SUCCESS] 上傳成功!
echo.

REM 步驟 4: 在遠程服務器上解壓和部署
echo 步驟 4: 在遠程服務器上解壓和部署...
echo.

echo ==========================================
echo 需要密碼: %SERVER_PASSWORD%
echo ==========================================
echo 當提示輸入密碼時，請輸入上面的密碼
echo.

REM 上傳並執行遠程部署腳本 (避免引號轉義問題)
echo 上傳遠程部署腳本...
REM 確保使用腳本目錄中的 remote_deploy.sh
scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL "%SCRIPT_DIR%remote_deploy.sh" %SERVER_USER%@%SERVER_IP%:/tmp/

if %errorlevel% neq 0 (
    echo [ERROR] 上傳遠程腳本失敗
    pause
    exit /b 1
)

REM 執行遠程腳本並刪除 (先修正 Windows 換行符問題)
echo 執行遠程部署...
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL %SERVER_USER%@%SERVER_IP% "sed -i 's/\r$//' /tmp/remote_deploy.sh && chmod +x /tmp/remote_deploy.sh && bash /tmp/remote_deploy.sh && rm -f /tmp/remote_deploy.sh"

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] 遠程部署失敗!
    echo.
    echo 提示: 如果提示輸入密碼，請輸入: %SERVER_PASSWORD%
    pause
    exit /b 1
)

echo.
echo ==========================================
echo [SUCCESS] 一鍵部署完成!
echo ==========================================
echo.

echo 服務網址:
echo   - 前台: http://power-light-church.duckdns.org 或 http://%SERVER_IP%
echo   - 後台 API: http://power-light-church.duckdns.org/api 或 http://%SERVER_IP%/api
echo.

echo HTTPS 設置:
echo   如果已配置 HTTPS，請使用:
echo   - 前台: https://power-light-church.duckdns.org
echo   - 後台 API: https://power-light-church.duckdns.org/api
echo.

echo   要設置 HTTPS，請執行:
echo     ssh %USERNAME%@%SERVER_IP%
echo     cd %REMOTE_PATH%/%PROJECT_NAME%
echo     ./setup-https-on-server.sh
echo.

echo 檢查服務狀態:
echo   ssh %USERNAME%@%SERVER_IP%
echo   cd %REMOTE_PATH%/%PROJECT_NAME%
echo   docker compose ps
echo.

echo [SUCCESS] 已自動配置預防機制:
echo   - 前台監控: 每 5 分鐘檢查並自動修復
echo   - 系統監控: 每小時檢查資源使用情況
echo   - 自動清理: 每天凌晨 2 點清理 Docker 資源
echo.

echo 查看監控日誌:
echo   ssh %USERNAME%@%SERVER_IP%
echo   tail -f /var/log/frontend-monitor.log
echo   tail -f /var/log/system-monitor.log
echo.

pause
