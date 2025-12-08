# 前端白屏問題診斷與修復指南

## 問題描述

前端網站運行幾天後變成白屏，但後端沒有錯誤。執行 `docker compose restart` 無效，但重新執行 `deploy-to-server.sh` 後可以恢復。

## 問題原因分析

### 主要原因

1. **前端容器內靜態文件丟失**
   - 前端容器使用多階段構建，靜態文件在構建時生成並複製到容器內
   - 如果容器因為磁盤空間不足、系統重啟、或文件系統問題導致文件丟失
   - 單純重啟容器無法恢復已丟失的文件（因為文件是在構建時生成的）

2. **容器重啟策略的局限性**
   - `restart: unless-stopped` 只能重啟容器，無法修復容器內的文件問題
   - 如果文件已經丟失，重啟後仍然是空容器

3. **缺少健康檢查和自動恢復機制**
   - 沒有健康檢查來及時發現問題
   - 沒有自動修復機制來恢復服務

### 為什麼重新部署有效？

重新執行 `deploy-to-server.sh` 會：
- 重新構建前端容器（`docker compose up -d --build`）
- 重新執行 `npm run build` 生成靜態文件
- 重新複製文件到容器內

這就是為什麼重新部署可以解決問題，但重啟不行。

## 解決方案

### 1. 診斷工具

使用 `diagnose-frontend.sh` 腳本來檢查前端狀態：

```bash
./diagnose-frontend.sh
```

這個腳本會檢查：
- Docker 服務狀態
- 容器運行狀態
- 容器內文件完整性（index.html、assets 目錄等）
- 磁盤空間使用情況
- Docker 磁盤使用
- 容器日誌
- Nginx 代理狀態
- 前端可訪問性

### 2. 自動修復工具

當診斷發現問題時，使用 `fix-frontend.sh` 自動修復：

```bash
./fix-frontend.sh
```

這個腳本會：
- 停止前端容器
- 刪除前端容器
- 可選清理舊映像（釋放空間）
- 重新構建前端容器
- 驗證文件完整性
- 重啟 Nginx 代理

### 3. 自動監控（推薦）

設置定期監控，自動檢測並修復問題：

#### 方法 1: 使用 cron（推薦）

編輯 crontab：
```bash
crontab -e
```

添加以下行（每 5 分鐘檢查一次）：
```cron
*/5 * * * * cd /root/project/work/docker-vue-java-mysql && ./monitor-frontend.sh
```

或者每小時檢查一次：
```cron
0 * * * * cd /root/project/work/docker-vue-java-mysql && ./monitor-frontend.sh
```

#### 方法 2: 使用 systemd timer

創建 `/etc/systemd/system/frontend-monitor.service`：
```ini
[Unit]
Description=Frontend Monitor Service
After=docker.service

[Service]
Type=oneshot
WorkingDirectory=/root/project/work/docker-vue-java-mysql
ExecStart=/root/project/work/docker-vue-java-mysql/monitor-frontend.sh
User=root
```

創建 `/etc/systemd/system/frontend-monitor.timer`：
```ini
[Unit]
Description=Frontend Monitor Timer

[Timer]
OnBootSec=5min
OnUnitActiveSec=5min

[Install]
WantedBy=timers.target
```

啟用 timer：
```bash
systemctl enable frontend-monitor.timer
systemctl start frontend-monitor.timer
```

### 4. 健康檢查配置

已在 `docker-compose.yml` 中添加健康檢查：

```yaml
healthcheck:
  test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost/"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 10s
```

這可以幫助 Docker 檢測容器健康狀態。

## 預防措施

### 1. 定期清理 Docker 資源

定期清理未使用的 Docker 資源，避免磁盤空間不足：

```bash
# 清理未使用的容器、網路、映像
docker system prune -f

# 清理未使用的映像（包括 dangling 映像）
docker image prune -a -f

# 查看 Docker 磁盤使用
docker system df
```

### 2. 監控磁盤空間

設置磁盤空間監控，當使用率超過 80% 時發出警告：

```bash
# 檢查磁盤使用率
df -h

# 設置 cron 監控（每天檢查一次）
0 0 * * * df -h | awk 'NR==2 {if ($5+0 > 80) print "警告: 磁盤使用率超過 80%"}' | mail -s "磁盤空間警告" your-email@example.com
```

### 3. 定期備份

雖然前端是靜態文件，但建議定期備份配置和證書：

```bash
# 備份 SSL 證書
tar -czf certbot-backup-$(date +%Y%m%d).tar.gz certbot/conf/

# 備份 nginx 配置
tar -czf nginx-config-backup-$(date +%Y%m%d).tar.gz nginx/
```

## 手動排查步驟

如果自動修復無效，可以手動排查：

### 1. 檢查容器狀態

```bash
docker compose ps
docker ps -a | grep frontend
```

### 2. 檢查容器日誌

```bash
docker logs vue_frontend
docker logs vue_frontend_church
docker logs nginx_proxy
```

### 3. 檢查容器內文件

```bash
# 進入容器檢查文件
docker exec vue_frontend ls -la /usr/share/nginx/html/
docker exec vue_frontend test -f /usr/share/nginx/html/index.html && echo "文件存在" || echo "文件不存在"

# 檢查文件大小
docker exec vue_frontend du -sh /usr/share/nginx/html/
```

### 4. 檢查磁盤空間

```bash
df -h
docker system df
```

### 5. 手動重建容器

```bash
# 停止並刪除前端容器
docker compose stop frontend frontend-church
docker compose rm -f frontend frontend-church

# 重新構建
docker compose up -d --build frontend frontend-church

# 檢查狀態
docker compose ps
```

## 常見問題

### Q: 為什麼不直接使用 volume 掛載靜態文件？

A: 使用 volume 掛載需要：
1. 在伺服器上構建前端（需要 Node.js 環境）
2. 管理構建產物
3. 處理構建失敗的情況

使用 Docker 多階段構建可以：
- 在構建時生成文件
- 不依賴伺服器環境
- 更簡潔的部署流程

### Q: 可以設置容器自動重建嗎？

A: 可以，但需要權衡：
- 自動重建會消耗資源
- 可能在不必要的時候重建
- 建議使用監控腳本，只在發現問題時重建

### Q: 如何查看監控日誌？

A: 監控腳本的日誌保存在：
```bash
tail -f /var/log/frontend-monitor.log
```

## 相關文件

- `diagnose-frontend.sh` - 診斷腳本
- `fix-frontend.sh` - 修復腳本
- `monitor-frontend.sh` - 監控腳本
- `docker-compose.yml` - Docker Compose 配置（已添加健康檢查）

## 聯繫支持

如果問題持續存在，請提供以下信息：
1. `diagnose-frontend.sh` 的完整輸出
2. 容器日誌：`docker logs vue_frontend` 和 `docker logs vue_frontend_church`
3. 系統資源：`df -h` 和 `docker system df`
4. 系統日誌：`journalctl -u docker`（如果使用 systemd）

