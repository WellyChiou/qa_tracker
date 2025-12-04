#!/bin/bash
# 手動初始化崗位人員配置腳本
# 用於在資料庫已存在的情況下初始化配置

echo "正在初始化崗位人員配置..."

# 執行 SQL 腳本（指定字符集為 utf8mb4）
docker compose exec -T mysql mysql -uroot -prootpassword --default-character-set=utf8mb4 church < mysql/init-position-config.sql

if [ $? -eq 0 ]; then
    echo "✅ 崗位人員配置初始化成功！"
else
    echo "❌ 初始化失敗，請檢查錯誤信息"
    exit 1
fi

