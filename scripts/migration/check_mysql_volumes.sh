#!/bin/bash

# 檢查所有 MySQL 相關的 volumes 並顯示內容大小

echo "=========================================="
echo "檢查所有 MySQL Volumes"
echo "=========================================="
echo ""

echo "所有 volumes："
docker volume ls
echo ""

echo "MySQL 相關的 volumes："
docker volume ls | grep -i mysql
echo ""

echo "專案相關的 volumes："
docker volume ls | grep -E "(docker|vue|java|mysql|backup)"
echo ""

echo "=========================================="
echo "檢查每個 volume 的大小和內容"
echo "=========================================="

for volume in $(docker volume ls -q | grep -i mysql); do
    echo ""
    echo "Volume: $volume"
    echo "詳細信息："
    docker volume inspect "$volume" | grep -E "(Mountpoint|Name)" || true
    
    # 檢查 volume 大小（通過臨時容器）
    echo "檢查內容..."
    SIZE=$(docker run --rm -v "$volume":/data alpine sh -c "du -sh /data 2>/dev/null | cut -f1" || echo "無法讀取")
    echo "大小: $SIZE"
    
    # 檢查是否有 MySQL 資料文件
    echo "檢查 MySQL 文件..."
    docker run --rm -v "$volume":/data alpine sh -c "ls -la /data 2>/dev/null | head -10" || echo "無法列出文件"
    echo "---"
done

echo ""
echo "=========================================="
echo "建議："
echo "=========================================="
echo "1. 檢查每個 volume 的 Mountpoint"
echo "2. 選擇包含實際資料的 volume"
echo "3. 使用正確的 volume 名稱重新執行遷移"

