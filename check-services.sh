#!/bin/bash

echo "=== Docker 容器狀態 ==="
docker ps -a

echo -e "\n=== 容器健康檢查 ==="
for container in nginx_proxy java_backend vue_personal vue_frontend_church mysql_db; do
    if docker ps | grep -q $container; then
        echo "✓ $container 正在運行"
    else
        echo "✗ $container 未運行"
        if docker ps -a | grep -q $container; then
            echo "  最後 5 行日誌："
            docker logs $container --tail 5 2>&1 | head -5
        fi
    fi
done

echo -e "\n=== 端口檢查 ==="
if command -v netstat &> /dev/null; then
    netstat -tlnp 2>/dev/null | grep -E ":80|:8080|:3306" || echo "無法檢查端口（需要 root 權限）"
elif command -v ss &> /dev/null; then
    ss -tlnp 2>/dev/null | grep -E ":80|:8080|:3306" || echo "無法檢查端口（需要 root 權限）"
else
    echo "無法檢查端口（netstat 或 ss 不可用）"
fi

echo -e "\n=== 系統資源 ==="
df -h | head -2
free -h 2>/dev/null | head -2 || vm_stat 2>/dev/null | head -5

echo -e "\n=== 後端健康檢查 ==="
curl -s --max-time 5 http://localhost:8080/api/hello 2>&1 | head -3 || echo "後端無法訪問"

echo -e "\n=== Nginx 配置檢查 ==="
docker exec nginx_proxy nginx -t 2>&1 || echo "Nginx 容器未運行或無法訪問"
