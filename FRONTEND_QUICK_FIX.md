# 前端白屏問題快速修復指南

## 問題症狀
- 前端網站變成白屏
- 後端沒有錯誤
- `docker compose restart` 無效
- 重新執行 `deploy-to-server.sh` 可以恢復

## 快速修復步驟

### 方法 1: 使用自動修復腳本（推薦）

```bash
cd /root/project/work/docker-vue-java-mysql
./fix-frontend.sh
```

### 方法 2: 手動修復

```bash
cd /root/project/work/docker-vue-java-mysql

# 停止並刪除前端容器
docker compose stop frontend frontend-church
docker compose rm -f frontend frontend-church

# 重新構建
docker compose up -d --build frontend frontend-church

# 重啟 Nginx
docker compose restart nginx
```

## 診斷問題

如果修復後仍有問題，執行診斷：

```bash
./diagnose-frontend.sh
```

## ⭐ 設置預防機制（強烈推薦）

**一鍵設置所有預防機制**：

```bash
cd /root/project/work/docker-vue-java-mysql
./setup-prevention.sh
```

這會自動設置：
- ✅ 前端監控（每 5 分鐘檢查一次，自動修復）
- ✅ 系統資源監控（每小時檢查一次，自動清理）
- ✅ 定期 Docker 清理（每天凌晨 2 點）

### 手動設置監控

編輯 crontab：
```bash
crontab -e
```

添加以下行：
```cron
# 前端監控（每 5 分鐘）
*/5 * * * * cd /root/project/work/docker-vue-java-mysql && ./monitor-frontend.sh

# 系統監控（每小時）
0 * * * * cd /root/project/work/docker-vue-java-mysql && ./monitor-system.sh

# Docker 清理（每天凌晨 2 點）
0 2 * * * cd /root/project/work/docker-vue-java-mysql && docker system prune -f && docker image prune -a -f
```

## 查看監控日誌

```bash
# 前端監控日誌
tail -f /var/log/frontend-monitor.log

# 系統監控日誌
tail -f /var/log/system-monitor.log
```

## 手動執行工具

```bash
# 前端監控
./monitor-frontend.sh

# 系統監控
./monitor-system.sh

# Docker 清理
./cleanup-docker.sh

# 前端診斷
./diagnose-frontend.sh

# 前端修復
./fix-frontend.sh
```

## 詳細說明

- **故障排除**：`docs/FRONTEND_TROUBLESHOOTING.md`
- **預防機制**：`docs/PREVENTION_GUIDE.md`

