# 安全刪除 Volume 檢查清單

## ⚠️ 重要：刪除前必須確認

在刪除 volume 之前，請務必確認以下事項，確保資料安全。

## ✅ 安全檢查步驟

### 步驟 1: 確認當前 MySQL 使用的存儲位置

```bash
# 檢查 docker-compose.yml 配置
cd /root/project/work/docker-vue-java-mysql
grep -A 2 "volumes:" docker-compose.yml | grep mysql

# 應該看到：
# - /root/project/work/mysql:/var/lib/mysql
# 這表示使用的是主機目錄，不是 volume
```

### 步驟 2: 確認主機目錄有資料

```bash
# 檢查主機目錄大小（應該有資料）
du -sh /root/project/work/mysql

# 檢查目錄內容
ls -la /root/project/work/mysql | head -10

# 應該看到 MySQL 資料文件，如：
# ibdata1, ib_logfile0, qa_tracker/, 等
```

### 步驟 3: 確認 MySQL 正在使用主機目錄

```bash
# 檢查 MySQL 容器使用的 volume
docker inspect mysql_db | grep -A 10 "Mounts"

# 應該看到：
# "Source": "/root/project/work/mysql"
# 而不是 volume 名稱
```

### 步驟 4: 確認資料庫可以正常查詢

```bash
# 測試查詢資料
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;"
docker compose exec mysql mysql -u root -prootpassword qa_tracker -e "SELECT COUNT(*) FROM users;"
docker compose exec mysql mysql -u root -prootpassword qa_tracker -e "SELECT COUNT(*) FROM records;"
```

### 步驟 5: 確認 volume 沒有被當前容器使用

```bash
# 檢查哪些容器在使用 qa_tracker_mysql_data
docker ps -a --filter volume=qa_tracker_mysql_data

# 如果沒有輸出，表示沒有容器在使用
# 如果有輸出，需要先停止那些容器
```

### 步驟 6: 備份主機目錄（額外保險）

```bash
# 備份主機目錄（以防萬一）
tar -czf /root/project/work/mysql_safety_backup_$(date +%Y%m%d_%H%M%S).tar.gz -C /root/project/work mysql

# 確認備份成功
ls -lh /root/project/work/mysql_safety_backup_*.tar.gz
```

## ✅ 安全刪除流程

只有在**所有檢查都通過**後，才執行刪除：

```bash
# 1. 確認配置使用主機目錄（不是 volume）
grep "/root/project/work/mysql:/var/lib/mysql" docker-compose.yml

# 2. 確認主機目錄有資料
du -sh /root/project/work/mysql
# 應該顯示 > 100M（有資料）

# 3. 確認 MySQL 正常運行
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;"

# 4. 停止所有服務（確保沒有容器使用 volume）
docker compose down

# 5. 再次確認沒有容器使用該 volume
docker ps -a --filter volume=qa_tracker_mysql_data
# 應該沒有輸出

# 6. 刪除 volume
docker volume rm qa_tracker_mysql_data

# 7. 重新啟動服務（使用主機目錄）
docker compose up -d

# 8. 驗證資料還在
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;"
docker compose exec mysql mysql -u root -prootpassword qa_tracker -e "SELECT COUNT(*) FROM users;"
```

## 🔍 詳細檢查腳本

創建檢查腳本 `check_before_remove.sh`：

```bash
#!/bin/bash

echo "=========================================="
echo "刪除 Volume 前安全檢查"
echo "=========================================="
echo ""

# 檢查 1: docker-compose.yml 配置
echo "[1/6] 檢查 docker-compose.yml 配置..."
if grep -q "/root/project/work/mysql:/var/lib/mysql" docker-compose.yml; then
    echo "✅ 配置正確：使用主機目錄"
else
    echo "❌ 配置錯誤：可能仍在使用 volume"
    exit 1
fi

# 檢查 2: 主機目錄有資料
echo ""
echo "[2/6] 檢查主機目錄資料..."
SIZE=$(du -sh /root/project/work/mysql 2>/dev/null | cut -f1)
if [ -d "/root/project/work/mysql" ] && [ "$(ls -A /root/project/work/mysql 2>/dev/null)" ]; then
    echo "✅ 主機目錄存在且有資料：$SIZE"
else
    echo "❌ 主機目錄為空或不存在"
    exit 1
fi

# 檢查 3: MySQL 容器使用的存儲
echo ""
echo "[3/6] 檢查 MySQL 容器使用的存儲..."
MOUNT=$(docker inspect mysql_db 2>/dev/null | grep -A 5 "Mounts" | grep "Source" | grep "/root/project/work/mysql" || echo "")
if [ -n "$MOUNT" ]; then
    echo "✅ MySQL 使用主機目錄"
else
    echo "⚠️  無法確認 MySQL 使用的存儲位置"
fi

# 檢查 4: 資料庫可以查詢
echo ""
echo "[4/6] 檢查資料庫可訪問性..."
if docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;" &>/dev/null; then
    echo "✅ 資料庫可以正常查詢"
    DB_COUNT=$(docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;" 2>/dev/null | grep -v "Database\|information_schema\|mysql\|performance_schema\|sys" | wc -l)
    echo "   資料庫數量: $DB_COUNT"
else
    echo "⚠️  無法查詢資料庫（可能 MySQL 未運行）"
fi

# 檢查 5: volume 使用情況
echo ""
echo "[5/6] 檢查 volume 使用情況..."
USING_CONTAINERS=$(docker ps -a --filter volume=qa_tracker_mysql_data --format "{{.ID}}" 2>/dev/null | wc -l)
if [ "$USING_CONTAINERS" -eq 0 ]; then
    echo "✅ 沒有容器使用該 volume"
else
    echo "⚠️  有 $USING_CONTAINERS 個容器使用該 volume"
    docker ps -a --filter volume=qa_tracker_mysql_data
fi

# 檢查 6: 備份建議
echo ""
echo "[6/6] 備份建議..."
echo "建議先備份主機目錄："
echo "  tar -czf /root/project/work/mysql_safety_backup_\$(date +%Y%m%d_%H%M%S).tar.gz -C /root/project/work mysql"

echo ""
echo "=========================================="
if [ "$USING_CONTAINERS" -eq 0 ] && [ -d "/root/project/work/mysql" ]; then
    echo "✅ 檢查通過，可以安全刪除 volume"
    echo ""
    echo "執行以下命令刪除："
    echo "  docker compose down"
    echo "  docker volume rm qa_tracker_mysql_data"
    echo "  docker compose up -d"
else
    echo "⚠️  請先解決上述問題後再刪除"
fi
echo "=========================================="
```

## 🎯 最安全的做法

**如果您仍然擔心，可以：**

1. **保留 volume 不刪除**
   - 不會影響系統運行
   - 只佔用約 206MB 空間
   - 作為額外備份

2. **只刪除確認為空的 volume**
   ```bash
   # qa_tracker_mysql_data 是空的，可以安全刪除
   # 但需要先停止使用它的容器
   docker compose down
   docker volume rm qa_tracker_mysql_data
   docker compose up -d
   ```

3. **等待一段時間再刪除**
   - 讓系統運行幾天
   - 確認一切正常
   - 然後再考慮刪除

## 📋 總結

**刪除 volume 不會影響資料，因為：**
- ✅ 資料已經在主機目錄 `/root/project/work/mysql`
- ✅ `docker-compose.yml` 配置使用主機目錄
- ✅ MySQL 容器使用主機目錄，不是 volume
- ✅ Volume 只是舊的備份，不再被使用

**但為了安全，建議：**
1. 先執行檢查腳本確認
2. 備份主機目錄
3. 停止服務後再刪除
4. 重新啟動後驗證資料

