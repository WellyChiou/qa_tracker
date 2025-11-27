@echo off
REM HTTPS 設置腳本 (Windows)
REM 域名: wc-project.duckdns.org

echo 🚀 開始設置 HTTPS...

REM 1. 創建必要的目錄
echo 📁 創建目錄結構...
if not exist "nginx\conf.d" mkdir nginx\conf.d
if not exist "certbot\conf" mkdir certbot\conf
if not exist "certbot\www" mkdir certbot\www

REM 2. 檢查 DNS（需要安裝 dig 或使用 nslookup）
echo 🔍 檢查 DNS 設置...
echo    請確認在 DuckDNS 中已設置 IP 為 38.54.89.136
echo    網址: https://www.duckdns.org
pause

REM 3. 啟動 nginx（不包含 certbot）
echo 🔧 啟動 Nginx...
docker-compose up -d nginx

REM 等待 nginx 啟動
timeout /t 5 /nobreak >nul

REM 4. 申請 SSL 證書
echo 📜 申請 Let's Encrypt SSL 證書...
echo    請輸入您的郵箱地址（用於證書到期提醒）:
set /p EMAIL="郵箱: "

docker-compose run --rm certbot certonly --webroot --webroot-path=/var/www/certbot --email %EMAIL% --agree-tos --no-eff-email -d wc-project.duckdns.org

if %ERRORLEVEL% EQU 0 (
    echo ✅ SSL 證書申請成功！
    
    REM 5. 更新 nginx 配置為 HTTPS 版本
    echo 🔄 更新 Nginx 配置為 HTTPS...
    copy nginx\nginx-https.conf nginx\nginx.conf
    
    REM 6. 重啟服務
    echo 🔄 重啟服務...
    docker-compose restart nginx
    docker-compose down
    docker-compose up -d
    
    echo.
    echo 🎉 設置完成！
    echo.
    echo 📋 下一步：
    echo 1. 在 LINE Developers Console 設置 Webhook URL:
    echo    https://wc-project.duckdns.org/api/line/webhook
    echo.
    echo 2. 訪問您的應用：
    echo    前端: https://wc-project.duckdns.org
    echo    API: https://wc-project.duckdns.org/api
    echo.
) else (
    echo ❌ SSL 證書申請失敗
    echo    請檢查：
    echo    1. DNS 是否已生效
    echo    2. 80 端口是否開放
    echo    3. 防火牆設置
)

pause

