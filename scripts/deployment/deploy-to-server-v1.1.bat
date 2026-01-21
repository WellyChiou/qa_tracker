@echo off
REM ==========================================
REM 一鍵部署腳本 (Windows 版本 - unified)
REM 目標：與 Mac/Linux 版本同步（Single Source of Truth = remote_deploy.sh）
REM 流程：打包 -> 上傳 tar.gz -> 上傳 /tmp/remote_deploy.sh -> 遠端執行 remote_deploy.sh
REM
REM 注意：
REM 1) Windows 內建 scp/ssh 無法在命令列安全傳遞密碼，建議使用 SSH Key
REM 2) 本腳本不內嵌密碼（避免外洩）；若你要用密碼登入，請在提示時手動輸入
REM ==========================================

chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

REM ====== 配置區（需要改就改這裡）======
set SERVER_IP=38.54.89.136
set SERVER_USER=root
set PROJECT_NAME=docker-vue-java-mysql
set REMOTE_PATH=/root/project/work
set ARCHIVE_NAME=%PROJECT_NAME%.tar.gz
set REMOTE_DEPLOY=scripts\deployment\remote_deploy.sh
REM =========================================

REM 取得腳本所在目錄並查找專案根目錄
set SCRIPT_DIR=%~dp0
REM 移除尾部的反斜線（如果有的話）
if "%SCRIPT_DIR:~-1%"=="\" set SCRIPT_DIR=%SCRIPT_DIR:~0,-1%

REM 向上查找專案根目錄（直到找到 docker-compose.yml）
cd /d "%SCRIPT_DIR%"
:find_root
if exist "docker-compose.yml" goto found_root
cd ..
REM 檢查是否已到達根目錄（無法再向上）
set CURRENT_CD=%CD%
cd ..
if "%CD%"=="%CURRENT_CD%" (
    echo [ERROR] 找不到 docker-compose.yml。請確認在專案目錄中執行。
    echo    目前: %SCRIPT_DIR%
    pause
    exit /b 1
)
cd /d "%CURRENT_CD%"
goto find_root

:found_root
for %%I in ("%CD%") do set CURRENT_DIR=%%~nxI
set PROJECT_ROOT=%CD%
REM 取得專案根目錄的父目錄（用於打包）
cd ..
set PARENT_DIR=%CD%
cd /d "%PROJECT_ROOT%"

echo ==========================================
echo 🚀 開始一鍵部署（Windows - unified）
echo ==========================================
echo Server: %SERVER_USER%@%SERVER_IP%
echo Remote Path: %REMOTE_PATH%
echo Project: %PROJECT_NAME%
echo.

REM ====== Step 1: 檢查必要工具 ======
echo Step 1/4: 檢查必要工具...
where tar >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到 tar 命令（Windows 10 1803+ 內建；或裝 Git for Windows）
    pause
    exit /b 1
)
where scp >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到 scp 命令（Windows 10 1809+ OpenSSH 內建；或裝 Git for Windows）
    pause
    exit /b 1
)
where ssh >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到 ssh 命令（Windows 10 1809+ OpenSSH 內建）
    pause
    exit /b 1
)

if not exist "%PROJECT_ROOT%\%REMOTE_DEPLOY%" (
    echo [ERROR] 找不到 %REMOTE_DEPLOY%（請確認檔案存在）
    pause
    exit /b 1
)

echo [OK] 工具檢查完成
echo.

REM ====== Step 2: 打包 ======
echo Step 2/4: 打包專案...

REM 清除舊壓縮檔（在父目錄中）
cd /d "%PARENT_DIR%"
if exist "%ARCHIVE_NAME%" del "%ARCHIVE_NAME%"

REM 切到父目錄打包（讓 tar 內路徑是 docker-vue-java-mysql/...）
REM 使用 ustar 格式讓 Linux 端解壓更穩
REM 排除：VCS、node_modules、target、log、dist、tar.gz、自家系統檔
REM 注意：使用 %CURRENT_DIR% 以確保排除規則與實際打包的目錄名稱一致
tar --format=ustar -czf "%ARCHIVE_NAME%" ^
    --exclude="%CURRENT_DIR%/.git" ^
    --exclude="%CURRENT_DIR%/node_modules" ^
    --exclude="%CURRENT_DIR%/**/node_modules" ^
    --exclude="%CURRENT_DIR%/target" ^
    --exclude="%CURRENT_DIR%/**/target" ^
    --exclude="%CURRENT_DIR%/.DS_Store" ^
    --exclude="%CURRENT_DIR%/**/.DS_Store" ^
    --exclude="%CURRENT_DIR%/*.log" ^
    --exclude="%CURRENT_DIR%/**/*.log" ^
    --exclude="%CURRENT_DIR%/frontend/dist" ^
    --exclude="%CURRENT_DIR%/frontend-personal/dist" ^
    --exclude="%CURRENT_DIR%/frontend-church/dist" ^
    --exclude="%CURRENT_DIR%/frontend-church-admin/dist" ^
    --exclude="%CURRENT_DIR%/**/dist" ^
    --exclude="%CURRENT_DIR%/*.tar.gz" ^
    --exclude="%CURRENT_DIR%/local-letsencrypt" ^
    --exclude="%CURRENT_DIR%/local-letsencrypt/**" ^
    --exclude="%CURRENT_DIR%/docker-compose.local.yml" ^
    --exclude="%CURRENT_DIR%/docker-compose.override.yml" ^
    "%CURRENT_DIR%"

cd /d "%PROJECT_ROOT%"

REM 檢查壓縮檔是否在父目錄中
if not exist "%PARENT_DIR%\%ARCHIVE_NAME%" (
    echo [ERROR] 打包失敗：找不到 %ARCHIVE_NAME%
    pause
    exit /b 1
)

if %errorlevel% neq 0 (
    echo [ERROR] 打包失敗!
    pause
    exit /b 1
)

REM 計算壓縮檔大小（使用父目錄中的檔案）
for %%A in ("%PARENT_DIR%\%ARCHIVE_NAME%") do set ARCHIVE_SIZE=%%~zA
set /a ARCHIVE_SIZE_KB=!ARCHIVE_SIZE!/1024
if !ARCHIVE_SIZE_KB! geq 1024 (
    set /a ARCHIVE_SIZE_MB=!ARCHIVE_SIZE_KB!/1024
    echo [OK] 打包完成: %ARCHIVE_NAME% (!ARCHIVE_SIZE_MB! MB)
) else (
    echo [OK] 打包完成: %ARCHIVE_NAME% (!ARCHIVE_SIZE_KB! KB)
)
echo.

REM ====== Step 3: 上傳 ======
echo Step 3/4: 上傳到伺服器...
echo 你可能會被要求輸入密碼（建議改用 SSH Key 免輸入）
echo.

ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL %SERVER_USER%@%SERVER_IP% "mkdir -p %REMOTE_PATH%"
if %errorlevel% neq 0 (
    echo [ERROR] 無法連線到遠端伺服器（請確認 IP/帳號/網路/金鑰或密碼）
    pause
    exit /b 1
)

scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL "%PARENT_DIR%\%ARCHIVE_NAME%" %SERVER_USER%@%SERVER_IP%:%REMOTE_PATH%/
if %errorlevel% neq 0 (
    echo [ERROR] 上傳壓縮檔失敗
    pause
    exit /b 1
)

scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL "%PROJECT_ROOT%\%REMOTE_DEPLOY%" %SERVER_USER%@%SERVER_IP%:/tmp/remote_deploy.sh
if %errorlevel% neq 0 (
    echo [ERROR] 上傳 remote_deploy.sh 失敗
    pause
    exit /b 1
)

echo [OK] 上傳完成
echo.

REM ====== Step 4: 遠端執行 ======
echo Step 4/4: 遠端部署中（remote_deploy.sh）...
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL %SERVER_USER%@%SERVER_IP% ^
 "sed -i 's/\r$//' /tmp/remote_deploy.sh && chmod +x /tmp/remote_deploy.sh && bash /tmp/remote_deploy.sh && rm -f /tmp/remote_deploy.sh"

if %errorlevel% neq 0 (
    echo [ERROR] 遠端部署失敗（請到伺服器查看 log）
    pause
    exit /b 1
)

echo.
echo ==========================================
echo ✅ 一鍵部署完成（Windows - unified）
echo ==========================================
echo.
echo 檢查服務狀態：
echo   ssh %SERVER_USER%@%SERVER_IP%
echo   cd %REMOTE_PATH%/%PROJECT_NAME%
echo   docker compose ps
echo.
pause
