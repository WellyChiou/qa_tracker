# 🔧 故障排除指南

本文檔記錄了部署過程中遇到的常見問題及解決方案，方便日後快速查找和解決問題。

---

## 📚 快速分類

- Backend：第 5 節、錯誤碼與檢查清單
- Frontend：第 6 節、第 11 節（白屏問題專章）
- Docker / Deployment：第 1~4 節、第 7~10 節
- Others：第 10 節參考與速查命令

---

## 📋 目錄

1. [Docker Compose 安裝問題](#1-docker-compose-安裝問題)
2. [端口檢查工具問題](#2-端口檢查工具問題)
3. [防火牆配置問題](#3-防火牆配置問題)
4. [Docker 構建錯誤](#4-docker-構建錯誤)
5. [後端連接問題](#5-後端連接問題)
6. [前端未啟動問題](#6-前端未啟動問題)
7. [常用診斷命令](#7-常用診斷命令)
8. [常見錯誤碼和解決方案](#8-常見錯誤碼和解決方案)
9. [部署檢查清單](#9-部署檢查清單)
10. [聯繫和參考](#10-聯繫和參考)
11. [Frontend 白屏問題專章（已合併）](#11-frontend-白屏問題專章已合併)

---

## 1. Docker Compose 安裝問題

### 問題：`docker-compose: command not found`

**錯誤訊息：**
```bash
-bash: docker-compose: command not found
```

**解決方案：**

#### 方法 1: 安裝 Docker Compose Plugin（推薦）

```bash
# 更新套件列表
sudo apt update

# 安裝 Docker Compose Plugin
sudo apt install -y docker-compose-plugin

# 驗證安裝（注意：是 docker compose，沒有連字號）
docker compose version
```

**使用方式：**
- 舊版：`docker-compose up`
- 新版：`docker compose up`（注意沒有連字號）

#### 方法 2: 安裝獨立版本

```bash
# 下載最新版本
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# 添加執行權限
sudo chmod +x /usr/local/bin/docker-compose

# 驗證安裝
docker-compose --version
```

**參考文件：** `INSTALL_DOCKER_COMPOSE.md`

---

## 2. 端口檢查工具問題

### 問題：`netstat: command not found`

**錯誤訊息：**
```bash
sudo: netstat: command not found
```

**解決方案：**

#### 方法 1: 使用 `ss` 命令（推薦，現代 Linux 系統）

```bash
# 檢查 80 和 8080 端口
sudo ss -tulpn | grep -E ':(80|8080)'

# 分別檢查
sudo ss -tulpn | grep :80
sudo ss -tulpn | grep :8080
```

#### 方法 2: 使用 `lsof` 命令

```bash
# 檢查端口
sudo lsof -i :80
sudo lsof -i :8080
```

#### 方法 3: 安裝 netstat（如果需要）

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install -y net-tools

# 然後就可以使用 netstat
sudo netstat -tulpn | grep :80
```

**注意：** 在較新的 Linux 系統中，`ss` 是 `netstat` 的現代替代品，功能更強大。

---

## 3. 防火牆配置問題

### 問題：`ufw: command not found`（Ubuntu 22.04）

**錯誤訊息：**
```bash
sudo: ufw: command not found
```

**解決方案：**

#### Ubuntu/Debian 系統

```bash
# 1. 安裝 UFW
sudo apt update
sudo apt install -y ufw

# 2. 先確保 SSH 端口已開放（避免鎖定自己）
sudo ufw allow 22/tcp

# 3. 啟用 UFW
sudo ufw enable

# 4. 開放所需端口
sudo ufw allow 80/tcp
sudo ufw allow 8080/tcp
sudo ufw reload

# 5. 檢查狀態
sudo ufw status verbose
```

#### CentOS/RHEL 系統

```bash
# 檢查 firewalld 狀態
sudo systemctl status firewalld

# 啟動 firewalld
sudo systemctl start firewalld
sudo systemctl enable firewalld

# 開放端口
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload

# 檢查狀態
sudo firewall-cmd --list-ports
```

#### 如果沒有防火牆工具

1. **先跳過這步，直接啟動服務測試**
2. **檢查雲服務商的安全組/防火牆規則**（這很重要！）
   - 阿里雲、騰訊雲、AWS 等都有安全組設置
   - 需要在雲控制台開放 80 和 8080 端口

**參考文件：** `UBUNTU_FIREWALL.md`

---

## 4. Docker 構建錯誤

### 問題 1: `version` 屬性過時警告

**警告訊息：**
```
WARN[0000] /root/project/work/docker-compose.yml: the attribute `version` is obsolete
```

**解決方案：**

從 `docker-compose.yml` 第一行刪除 `version: '3.8'`

```yaml
# 刪除這行
# version: '3.8'

services:
  # ... 其他配置
```

**原因：** Docker Compose v2 不再需要 `version` 屬性。

---

### 問題 2: Maven 映像標籤不存在

**錯誤訊息：**
```
failed to resolve source metadata for docker.io/library/maven:3.8.6-openjdk-17-slim: not found
```

**解決方案：**

修改 `backend-personal/Dockerfile` 或 `backend-church/Dockerfile`：

```dockerfile
# 舊的（不存在）
# FROM maven:3.8.6-openjdk-17-slim AS build

# 新的（正確）
FROM maven:3.8-eclipse-temurin-17 AS build

# 運行階段也改為
FROM eclipse-temurin:17-jre-jammy
```

**原因：** 某些 Maven 映像標籤可能不存在或已被移除，使用 `eclipse-temurin` 是更可靠的選擇。

---

## 5. 後端連接問題

### 問題：前端顯示「後端無法連接」

**症狀：**
- 前端頁面正常顯示
- 後端狀態顯示「✗ 無法連接」
- 瀏覽器控制台可能有 CORS 錯誤

**解決方案：**

#### 步驟 1: 添加 CORS 配置

創建文件 `backend-personal/src/main/java/com/example/helloworld/config/CorsConfig.java` 或 `backend-church/src/main/java/com/example/helloworld/config/CorsConfig.java`：

```java
package com.example.helloworld.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 允許所有來源（生產環境建議指定具體域名）
        config.addAllowedOriginPattern("*");
        
        // 允許所有 HTTP 方法
        config.addAllowedMethod("*");
        
        // 允許所有請求頭
        config.addAllowedHeader("*");
        
        // 允許發送憑證（如果需要）
        config.setAllowCredentials(true);
        
        // 預檢請求的緩存時間（秒）
        config.setMaxAge(3600L);
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

#### 步驟 2: 重新構建後端

```bash
# 停止服務
docker compose down

# 重新構建後端
docker compose up -d --build backend-personal backend-church
```

#### 步驟 3: 檢查後端日誌

```bash
# 查看後端日誌
docker compose logs backend-personal
docker compose logs backend-church

# 持續查看日誌
docker compose logs -f backend-personal backend-church
```

#### 步驟 4: 測試後端 API

```bash
# 在虛擬主機上測試
curl http://localhost/api/hello

# 應該會看到 JSON 回應
```

**原因：** 瀏覽器的同源策略阻止了從 80 端口（前端）訪問 8080 端口（後端）的請求，需要後端配置 CORS 允許跨域請求。

---

## 6. 前端未啟動問題

### 問題：只啟動了後端，前端沒有啟動

**症狀：**
```bash
docker compose ps
# 只看到 mysql_db 和 java_backend，沒有 vue_frontend
```

**解決方案：**

#### 啟動所有服務

```bash
# 啟動所有服務（包括前端）
docker compose up -d

# 或重新構建並啟動
docker compose up -d --build
```

#### 檢查前端狀態

```bash
# 查看所有容器狀態
docker compose ps

# 查看前端日誌
docker compose logs frontend
```

#### 如果前端啟動失敗

**可能原因 1: 80 端口被佔用**

```bash
# 檢查 80 端口
sudo ss -tulpn | grep :80

# 如果被佔用，修改 docker-compose.yml
# 將 frontend 的端口改為 3000
frontend:
  ports:
    - "3000:80"
```

**可能原因 2: 前端構建失敗**

```bash
# 查看前端構建日誌
docker compose logs frontend

# 重新構建前端
docker compose up -d --build frontend
```

---

## 7. 常用診斷命令

### 檢查容器狀態

```bash
# 查看所有容器狀態
docker compose ps

# 查看特定容器狀態
docker compose ps backend-personal
docker compose ps backend-church
docker compose ps frontend-personal
docker compose ps mysql
```

### 查看日誌

```bash
# 查看所有服務日誌
docker compose logs

# 查看特定服務日誌
docker compose logs backend-personal
docker compose logs backend-church
docker compose logs frontend-personal
docker compose logs mysql

# 持續查看日誌（實時）
docker compose logs -f

# 查看最近 50 行日誌
docker compose logs --tail=50 backend-personal
docker compose logs --tail=50 backend-church
```

### 測試服務

```bash
# 測試前端
curl http://localhost

# 測試後端 API
curl http://localhost/api/hello

# 測試資料庫連接
docker compose exec mysql mysql -u appuser -papppassword testdb
```

### 容器內部檢查

```bash
# 進入後端容器
docker compose exec backend-personal bash
docker compose exec backend-church bash

# 進入 MySQL 容器
docker compose exec mysql bash

# 檢查端口監聽
docker compose exec backend-personal ss -tuln | grep 8080
docker compose exec backend-church ss -tuln | grep 8080
docker compose exec frontend-personal ss -tuln | grep 80
```

### 資源使用情況

```bash
# 查看容器資源使用
docker stats

# 查看特定容器資源使用
docker stats java_backend_personal java_backend_church vue_personal mysql_db
```

### 重啟服務

```bash
# 重啟所有服務
docker compose restart

# 重啟特定服務
docker compose restart backend-personal
docker compose restart backend-church
docker compose restart frontend-personal

# 停止所有服務
docker compose down

# 停止並刪除資料卷（會清除資料庫資料）
docker compose down -v
```

### 重新構建

```bash
# 重新構建所有服務
docker compose up -d --build

# 重新構建特定服務
docker compose up -d --build backend-personal backend-church
docker compose up -d --build frontend-personal
```

---

## 8. 常見錯誤碼和解決方案

### 錯誤：端口已被佔用

**錯誤訊息：**
```
Error: bind: address already in use
```

**解決方案：**
1. 找到佔用端口的程序：`sudo ss -tulpn | grep :80`
2. 停止該程序，或修改 `docker-compose.yml` 中的端口映射

### 錯誤：無法連接資料庫

**錯誤訊息：**
```
Communications link failure
```

**解決方案：**
1. 檢查 MySQL 容器是否正常：`docker compose ps mysql`
2. 檢查 MySQL 日誌：`docker compose logs mysql`
3. 確認後端等待 MySQL 就緒（使用 `depends_on` 和 `condition: service_healthy`）

### 錯誤：容器無法啟動

**錯誤訊息：**
```
Container exited with code 1
```

**解決方案：**
1. 查看容器日誌：`docker compose logs [service-name]`
2. 檢查 Dockerfile 配置
3. 檢查環境變數設置
4. 確認依賴服務已啟動

---

## 9. 部署檢查清單

### 部署前檢查

- [ ] Docker 已安裝：`docker --version`
- [ ] Docker Compose 已安裝：`docker compose version`
- [ ] 專案檔案已上傳到虛擬主機
- [ ] 端口檢查（80, 8080, 3306）是否可用
- [ ] 防火牆已配置（如果需要）

### 部署後檢查

- [ ] 所有容器都是 `Up` 狀態：`docker compose ps`
- [ ] 前端可以訪問：`curl http://localhost`
- [ ] 後端 API 可以訪問：`curl http://localhost/api/hello`
- [ ] 瀏覽器可以訪問前端頁面
- [ ] 前端可以成功連接後端（顯示「✓ 運行中」）

### 問題排查順序

1. 檢查容器狀態：`docker compose ps`
2. 查看日誌：`docker compose logs`
3. 測試服務：`curl http://localhost/api/hello`
4. 檢查端口：`sudo ss -tulpn | grep -E ':(80|8080)'`
5. 檢查防火牆：`sudo ufw status` 或 `sudo firewall-cmd --list-ports`
6. 檢查雲服務商安全組設置

---

## 10. 聯繫和參考

### 相關文件

- `README.md` - 專案說明
- `QUICK_START.md` - 快速部署指南
- `DEPLOY.md` - 詳細部署指南
- `INSTALL_DOCKER_COMPOSE.md` - Docker Compose 安裝指南
- `UBUNTU_FIREWALL.md` - Ubuntu 防火牆配置

### 有用的命令速查

```bash
# 一鍵檢查所有服務狀態
docker compose ps && echo "---" && docker compose logs --tail=10

# 一鍵重啟所有服務
docker compose restart

# 一鍵查看所有日誌
docker compose logs -f

# 清理所有未使用的資源
docker system prune -a
```

---

**最後更新：** 2024年
**維護者：** 請根據實際情況更新本文檔

---

## 11. Frontend 白屏問題專章（已合併）

> 以下內容由 `docs/FRONTEND_TROUBLESHOOTING.md` 原文合併，僅做章節整併，不改問題判斷邏輯。

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
