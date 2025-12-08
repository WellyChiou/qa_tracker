@echo off
REM Set code page to UTF-8
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

REM One-click deployment script (Windows version)
REM Function: Package project -> Upload to server -> Extract -> Deploy

REM Configuration
set SERVER_IP=38.54.89.136
set SERVER_USER=root
REM Note: If password contains ! character, use environment variable
set "SERVER_PASSWORD=!Welly775"
set PROJECT_NAME=docker-vue-java-mysql
set REMOTE_PATH=/root/project/work
set ARCHIVE_NAME=%PROJECT_NAME%.tar.gz

REM Get current directory name (for packaging)
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%"
for %%I in ("%CD%") do set CURRENT_DIR=%%~nxI

echo ==========================================
echo Starting deployment process
echo ==========================================
echo.

REM Step 1: Check required tools
echo Step 1: Checking required tools...
echo.

REM Check tar command (built-in Windows 10 1803+)
where tar >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] tar command not found
    echo Please ensure you are using Windows 10 1803 or later
    echo Or install Git for Windows which includes tar
    pause
    exit /b 1
)

REM Check scp command (built-in Windows 10 1809+ OpenSSH)
where scp >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] scp command not found
    echo Please ensure you are using Windows 10 1809 or later
    echo Or install Git for Windows which includes OpenSSH tools
    echo.
    echo If using PuTTY, ensure pscp.exe is in PATH
    pause
    exit /b 1
)

echo [SUCCESS] Tools check completed
echo.

REM Step 2: Package project
echo Step 2: Packaging project...
echo.

REM Ensure in script directory
cd /d "%SCRIPT_DIR%"

REM Delete old archive file
if exist "..\%ARCHIVE_NAME%" (
    echo Deleting old archive file...
    del /f /q "..\%ARCHIVE_NAME%"
)

REM Package project
echo Packaging project...
echo Current directory: %CURRENT_DIR%
echo Package as: %PROJECT_NAME%
cd /d ".."

REM Use tar to package (exclude unnecessary files)
tar -czf "%ARCHIVE_NAME%" ^
    --exclude="%CURRENT_DIR%\.git" ^
    --exclude="%CURRENT_DIR%\node_modules" ^
    --exclude="%CURRENT_DIR%\target" ^
    --exclude="%CURRENT_DIR%\.DS_Store" ^
    --exclude="%CURRENT_DIR%\*.log" ^
    --exclude="%CURRENT_DIR%\frontend\dist" ^
    --exclude="%CURRENT_DIR%\*.tar.gz" ^
    "%CURRENT_DIR%"

if %errorlevel% neq 0 (
    echo [ERROR] Packaging failed!
    pause
    exit /b 1
)

if not exist "%ARCHIVE_NAME%" (
    echo [ERROR] Packaging failed! Archive file not found
    pause
    exit /b 1
)

REM Get file size
for %%A in ("%ARCHIVE_NAME%") do set ARCHIVE_SIZE=%%~zA
set /a ARCHIVE_SIZE_KB=!ARCHIVE_SIZE!/1024
if !ARCHIVE_SIZE_KB! geq 1024 (
    set /a ARCHIVE_SIZE_MB=!ARCHIVE_SIZE_KB!/1024
    echo [SUCCESS] Packaging completed! File size: !ARCHIVE_SIZE_MB! MB
) else (
    echo [SUCCESS] Packaging completed! File size: !ARCHIVE_SIZE_KB! KB
)
echo.

REM Step 3: Upload to server
echo Step 3: Upload to server
echo Server: %SERVER_USER%@%SERVER_IP%
echo.

REM Create remote directory (if not exists)
echo Creating remote directory...
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL %SERVER_USER%@%SERVER_IP% "mkdir -p %REMOTE_PATH%"

REM Upload file
echo Uploading archive file...
echo Please enter password: %SERVER_PASSWORD%
echo.

REM Use scp to upload (requires SSH key or manual password entry)
REM Note: Windows scp does not support passing password in command line
REM Recommend using SSH key authentication

scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL "%ARCHIVE_NAME%" %SERVER_USER%@%SERVER_IP%:%REMOTE_PATH%/

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Upload failed!
    echo.
    echo Tip: If prompted for password, enter: %SERVER_PASSWORD%
    echo Or configure SSH key authentication to avoid entering password
    echo.
    pause
    exit /b 1
)

echo [SUCCESS] Upload successful!
echo.

REM Step 4: Extract and deploy on remote server
echo Step 4: Extract and deploy on remote server...
echo.
echo ==========================================
echo PASSWORD REQUIRED: %SERVER_PASSWORD%
echo ==========================================
echo Please enter the password above when prompted
echo.

REM Upload and execute remote deployment script (avoid quote escaping issues)
echo Uploading remote deployment script...
REM Ensure using remote_deploy.sh from script directory
scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL "%SCRIPT_DIR%remote_deploy.sh" %SERVER_USER%@%SERVER_IP%:/tmp/

if %errorlevel% neq 0 (
    echo [ERROR] Failed to upload remote script
    pause
    exit /b 1
)

REM Execute remote script and delete (fix Windows line ending issue first)
echo Executing remote deployment...
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL %SERVER_USER%@%SERVER_IP% "sed -i 's/\r$//' /tmp/remote_deploy.sh && chmod +x /tmp/remote_deploy.sh && bash /tmp/remote_deploy.sh && rm -f /tmp/remote_deploy.sh"

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Remote deployment failed!
    echo.
    echo Tip: If prompted for password, enter: %SERVER_PASSWORD%
    pause
    exit /b 1
)

echo.
echo ==========================================
echo [SUCCESS] Deployment completed!
echo ==========================================
echo.
echo Service URLs:
echo   - Frontend: http://wc-project.duckdns.org or http://%SERVER_IP%
echo   - Backend API: http://wc-project.duckdns.org/api or http://%SERVER_IP%/api
echo.
echo HTTPS Setup:
echo   If HTTPS is configured, use:
echo   - Frontend: https://wc-project.duckdns.org
echo   - Backend API: https://wc-project.duckdns.org/api
echo   - LINE Bot Webhook: https://wc-project.duckdns.org/api/line/webhook
echo.
echo   To setup HTTPS, execute:
echo     ssh %SERVER_USER%@%SERVER_IP%
echo     cd %REMOTE_PATH%/%PROJECT_NAME%
echo     ./setup-https-on-server.sh
echo.
echo Check service status:
echo   ssh %SERVER_USER%@%SERVER_IP%
echo   cd %REMOTE_PATH%/%PROJECT_NAME%
echo   docker compose ps
echo.
echo [SUCCESS] Prevention mechanisms have been automatically configured:
echo   - Frontend monitoring: Check every 5 minutes and auto-fix
echo   - System monitoring: Check every hour for resource usage
echo   - Auto cleanup: Clean Docker resources daily at 2 AM
echo.
echo View monitoring logs:
echo   ssh %SERVER_USER%@%SERVER_IP%
echo   tail -f /var/log/frontend-monitor.log
echo   tail -f /var/log/system-monitor.log
echo.

pause
