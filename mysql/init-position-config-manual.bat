@echo off
REM 手動初始化崗位人員配置腳本（Windows 版本）
REM 用於在資料庫已存在的情況下初始化配置

echo 正在初始化崗位人員配置...

REM 執行 SQL 腳本（指定字符集為 utf8mb4）
docker compose exec -T mysql mysql -uroot -prootpassword --default-character-set=utf8mb4 church < mysql\init-position-config.sql

if %ERRORLEVEL% EQU 0 (
    echo ✅ 崗位人員配置初始化成功！
) else (
    echo ❌ 初始化失敗，請檢查錯誤信息
    exit /b 1
)

