#!/bin/bash

# 強制刪除正在使用的 volume

VOLUME_NAME="qa_tracker_mysql_data"

echo "=========================================="
echo "強制刪除 Volume: $VOLUME_NAME"
echo "=========================================="
echo ""

# 步驟 1: 找出使用該 volume 的容器
echo "[1/4] 找出使用該 volume 的容器..."
CONTAINER_IDS=$(docker ps -a --filter volume=$VOLUME_NAME --format "{{.ID}}")

if [ -z "$CONTAINER_IDS" ]; then
    echo "✅ 沒有容器使用該 volume"
else
    echo "找到以下容器使用該 volume:"
    docker ps -a --filter volume=$VOLUME_NAME --format "  {{.ID}} {{.Names}} {{.Status}}"
    echo ""
    
    # 步驟 2: 刪除這些容器
    echo "[2/4] 刪除使用該 volume 的容器..."
    for CONTAINER_ID in $CONTAINER_IDS; do
        echo "刪除容器: $CONTAINER_ID"
        docker rm -f "$CONTAINER_ID" 2>/dev/null || echo "  無法刪除容器 $CONTAINER_ID"
    done
    echo "✅ 容器已刪除"
fi

echo ""

# 步驟 3: 再次確認沒有容器使用該 volume
echo "[3/4] 確認沒有容器使用該 volume..."
REMAINING=$(docker ps -a --filter volume=$VOLUME_NAME --format "{{.ID}}" | wc -l)
if [ "$REMAINING" -eq 0 ]; then
    echo "✅ 確認沒有容器使用該 volume"
else
    echo "⚠️  仍有 $REMAINING 個容器使用該 volume"
    docker ps -a --filter volume=$VOLUME_NAME
    echo ""
    echo "嘗試強制刪除這些容器..."
    docker ps -a --filter volume=$VOLUME_NAME --format "{{.ID}}" | xargs -r docker rm -f
fi

echo ""

# 步驟 4: 刪除 volume
echo "[4/4] 刪除 volume..."
if docker volume rm "$VOLUME_NAME" 2>/dev/null; then
    echo "✅ Volume 已成功刪除"
else
    echo "❌ 刪除失敗，可能仍有容器在使用"
    echo ""
    echo "嘗試使用 --force 參數（如果支持）..."
    docker volume rm --force "$VOLUME_NAME" 2>/dev/null || {
        echo "無法刪除 volume"
        echo ""
        echo "請手動檢查："
        echo "  docker ps -a --filter volume=$VOLUME_NAME"
        echo "  docker volume inspect $VOLUME_NAME"
    }
fi

echo ""
echo "=========================================="
echo "完成"
echo "=========================================="
echo ""
echo "驗證："
docker volume ls | grep "$VOLUME_NAME" || echo "✅ Volume 已刪除"
