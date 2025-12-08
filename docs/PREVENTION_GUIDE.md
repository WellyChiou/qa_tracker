# 前端白屏問題預防指南

## 概述

本文檔說明如何設置預防機制，避免前端白屏問題再次發生。

## 快速設置

執行一鍵設置腳本：

```bash
cd /root/project/work/docker-vue-java-mysql
./setup-prevention.sh
```

這個腳本會自動設置：
- ✅ 前端監控（每 5 分鐘檢查一次）
- ✅ 系統資源監控（每小時檢查一次）
- ✅ 定期 Docker 清理（每天凌晨 2 點）

## 預防機制詳解

### 1. 前端自動監控

**功能**：定期檢查前端容器狀態和文件完整性，發現問題時自動修復。

**設置**：
```bash
# 手動添加到 crontab
crontab -e

# 添加以下行（每 5 分鐘檢查一次）
*/5 * * * * cd /root/project/work/docker-vue-java-mysql && ./monitor-frontend.sh
```

**查看日誌**：
```bash
tail -f /var/log/frontend-monitor.log
```

### 2. 系統資源監控

**功能**：監控磁盤空間和 Docker 資源使用，自動清理避免資源耗盡。

**設置**：
```bash
# 手動添加到 crontab
crontab -e

# 添加以下行（每小時檢查一次）
0 * * * * cd /root/project/work/docker-vue-java-mysql && ./monitor-system.sh
```

**查看日誌**：
```bash
tail -f /var/log/system-monitor.log
```

**自動清理觸發條件**：
- 磁盤使用率 ≥ 90%：立即執行 Docker 清理
- 磁盤使用率 ≥ 80%：發出警告
- 發現異常退出的容器：自動重啟

### 3. 定期 Docker 清理

**功能**：定期清理未使用的 Docker 資源，釋放磁盤空間。

**設置**：
```bash
# 手動添加到 crontab
crontab -e

# 添加以下行（每天凌晨 2 點執行）
0 2 * * * cd /root/project/work/docker-vue-java-mysql && docker system prune -f && docker image prune -a -f
```

**手動執行**：
```bash
./cleanup-docker.sh
```

### 4. 容器健康檢查

已在 `docker-compose.yml` 中配置健康檢查：

```yaml
healthcheck:
  test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost/"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 10s
```

Docker 會定期檢查容器健康狀態，如果健康檢查失敗，會觸發容器重啟。

### 5. 容器重啟策略

已設置 `restart: unless-stopped`，確保容器在異常退出時自動重啟。

### 6. 資源限制

已為前端容器設置記憶體限制，防止資源耗盡：

```yaml
deploy:
  resources:
    limits:
      memory: 512M
    reservations:
      memory: 256M
```

## 監控流程

```
┌─────────────────┐
│  前端監控      │  (每 5 分鐘)
│ monitor-frontend│
└────────┬────────┘
         │
         ├─→ 檢查容器狀態
         ├─→ 檢查文件完整性
         ├─→ 測試可訪問性
         │
         └─→ 發現問題 → 自動修復
         
┌─────────────────┐
│  系統監控      │  (每小時)
│ monitor-system  │
└────────┬────────┘
         │
         ├─→ 檢查磁盤空間
         ├─→ 檢查 Docker 資源
         ├─→ 檢查容器狀態
         │
         └─→ 磁盤 ≥ 90% → 自動清理
```

## 手動操作

### 查看所有監控任務

```bash
crontab -l
```

### 查看監控日誌

```bash
# 前端監控日誌
tail -f /var/log/frontend-monitor.log

# 系統監控日誌
tail -f /var/log/system-monitor.log
```

### 手動執行監控

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

### 移除監控任務

```bash
crontab -e
# 刪除相關行
```

## 最佳實踐

### 1. 定期檢查日誌

建議每週檢查一次監控日誌，確保系統正常運行：

```bash
# 查看最近 100 行日誌
tail -100 /var/log/frontend-monitor.log
tail -100 /var/log/system-monitor.log
```

### 2. 監控磁盤空間

定期檢查磁盤使用情況：

```bash
df -h
docker system df
```

### 3. 備份重要數據

雖然前端是靜態文件，但建議定期備份：

```bash
# 備份 SSL 證書
tar -czf certbot-backup-$(date +%Y%m%d).tar.gz certbot/conf/

# 備份配置
tar -czf config-backup-$(date +%Y%m%d).tar.gz nginx/ docker-compose.yml
```

### 4. 設置告警通知

可以擴展監控腳本，在發現問題時發送郵件或 LINE 通知：

```bash
# 在 monitor-frontend.sh 中添加
if [ $ISSUES -gt 0 ]; then
    # 發送郵件
    echo "前端服務異常" | mail -s "前端告警" your-email@example.com
    
    # 或發送 LINE 通知（需要配置 LINE Notify）
    curl -X POST https://notify-api.line.me/api/notify \
        -H "Authorization: Bearer YOUR_TOKEN" \
        -d "message=前端服務異常，請檢查"
fi
```

## 故障排查

### 監控腳本不執行

1. 檢查 crontab 是否正確設置：
   ```bash
   crontab -l
   ```

2. 檢查腳本權限：
   ```bash
   ls -l *.sh
   chmod +x *.sh
   ```

3. 檢查日誌：
   ```bash
   # 查看 cron 日誌
   grep CRON /var/log/syslog
   # 或
   journalctl -u cron
   ```

### 監控腳本執行失敗

1. 檢查腳本路徑是否正確
2. 檢查腳本是否有執行權限
3. 檢查日誌文件是否可寫入
4. 手動執行腳本查看錯誤信息

## 相關文檔

- [前端故障排除指南](./FRONTEND_TROUBLESHOOTING.md)
- [快速修復指南](../FRONTEND_QUICK_FIX.md)

## 總結

通過設置這些預防機制，可以：

1. ✅ **自動檢測問題**：定期檢查前端狀態
2. ✅ **自動修復問題**：發現問題時自動重建容器
3. ✅ **預防資源耗盡**：定期清理 Docker 資源
4. ✅ **監控系統健康**：及時發現磁盤空間不足等問題
5. ✅ **減少人工干預**：大部分問題可以自動解決

建議在部署後立即執行 `setup-prevention.sh` 設置所有預防機制。

