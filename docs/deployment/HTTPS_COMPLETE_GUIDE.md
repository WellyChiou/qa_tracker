# HTTPS 完整設置指南

## 📋 目錄

1. [快速開始](#快速開始)
2. [方案選擇](#方案選擇)
3. [詳細設置步驟](#詳細設置步驟)
4. [後續部署](#後續部署)
5. [故障排除](#故障排除)

---

## 🚀 快速開始

### 當前配置（已完成）

- **域名**：`wc-project.duckdns.org`
- **郵箱**：`chiou713@gmail.com`
- **SSL 證書**：已申請（有效期至 2026-02-25）
- **HTTPS 狀態**：✅ 已啟用

### 一鍵部署流程

#### 首次部署（需要設置 HTTPS）

```bash
# 1. 本地執行
deploy-to-server.bat

# 2. 服務器上設置 HTTPS（僅首次）
ssh root@38.54.89.136
cd /root/project/work/docker-vue-java-mysql
./setup-https-on-server.sh
```

#### 後續部署（自動保留 HTTPS）

```bash
# 只需要執行
deploy-to-server.bat
```

部署腳本會自動：
- ✅ 保留 SSL 證書
- ✅ 自動切換到 HTTPS 配置
- ✅ 重啟服務

---

## 🎯 方案選擇

### 方案 1：DuckDNS（當前使用）✅

- **域名**：`wc-project.duckdns.org`
- **優點**：免費、永久有效、設置簡單
- **狀態**：已設置並運行中

### 方案 2：ngrok（測試用）

- **適用**：快速測試
- **缺點**：免費版 URL 會變動
- **文檔**：見 `HTTPS_WITHOUT_DOMAIN.md`

### 方案 3：購買域名

- **適用**：生產環境
- **推薦**：Cloudflare（$8-12/年）
- **文檔**：見 `FREE_DOMAIN_SETUP.md`

---

## 📝 詳細設置步驟

### 步驟 1：確認域名設置

1. 前往 https://www.duckdns.org
2. 確認 `wc-project.duckdns.org` 的 IP 為 `38.54.89.136`
3. 如果還沒設置，請設置並點擊 "update ip"

### 步驟 2：申請 SSL 證書

```bash
# 停止 nginx（讓 certbot 使用 80 端口）
docker compose stop nginx

# 申請證書
docker compose run --rm -p 80:80 --entrypoint='' certbot sh -c \
  'certbot certonly --standalone --email chiou713@gmail.com --agree-tos --no-eff-email -d wc-project.duckdns.org'

# 啟動 nginx
docker compose up -d nginx
```

### 步驟 3：切換到 HTTPS 配置

```bash
# 複製 HTTPS 配置
cp nginx/nginx-https.conf nginx/nginx.conf

# 重啟 nginx
docker compose restart nginx
```

### 步驟 4：驗證 HTTPS

```bash
# 測試 HTTPS
curl -I https://wc-project.duckdns.org

# 應該返回 HTTP/2 200
```

### 步驟 5：設置 LINE Bot Webhook

1. 前往 https://developers.line.biz/console/
2. 選擇您的 Channel
3. 設置 Webhook URL：`https://wc-project.duckdns.org/api/line/webhook`
4. 點擊 "Verify" 驗證
5. 啟用 "Use webhook"

---

## 🔄 後續部署

### 自動保留機制

1. **SSL 證書**：存儲在 `certbot/conf/`，不會被刪除
2. **自動檢測**：部署腳本會自動檢測證書並切換配置
3. **自動續期**：Certbot 每 12 小時檢查並自動續期

### 部署流程

```bash
# 只需要執行一鍵部署
deploy-to-server.bat
```

部署腳本會自動：
- 檢查證書是否存在
- 如果存在，使用 HTTPS 配置
- 如果不存在，使用 HTTP 配置

### 證書自動續期

Certbot 服務會自動續期證書：
- **檢查頻率**：每 12 小時
- **續期時機**：證書到期前 30 天
- **無需手動操作**

---

## 🔧 故障排除

### 問題 1：證書申請失敗

**症狀**：DNS 查詢超時

**解決**：
```bash
# 使用 standalone 模式（需要先停止 nginx）
docker compose stop nginx
docker compose run --rm -p 80:80 --entrypoint='' certbot sh -c \
  'certbot certonly --standalone --email chiou713@gmail.com --agree-tos --no-eff-email -d wc-project.duckdns.org'
docker compose up -d nginx
```

### 問題 2：nginx 無法啟動

**症狀**：證書文件不存在

**解決**：
```bash
# 檢查證書是否存在
ls -la certbot/conf/live/wc-project.duckdns.org/

# 如果不存在，重新申請
# 如果存在但 nginx 仍報錯，檢查文件權限
```

### 問題 3：403 Forbidden 錯誤

**症狀**：登入時出現 403

**解決**：
- 確認後端 CORS 配置包含域名
- 檢查 `SecurityConfig.java` 中的 `allowedOrigins`
- 確認已包含 `https://wc-project.duckdns.org`

### 問題 4：502 Bad Gateway

**症狀**：API 請求返回 502

**解決**：
```bash
# 檢查後端是否正常運行
docker compose ps backend

# 檢查後端日誌
docker compose logs backend

# 等待後端完全啟動（約 30-60 秒）
```

### 問題 5：證書過期

**症狀**：瀏覽器顯示證書無效

**解決**：
```bash
# 手動續期
docker compose run --rm certbot renew

# 重啟 nginx
docker compose restart nginx
```

---

## 📁 文件結構

### 配置文件

```
nginx/
├── nginx.conf              # 當前使用的配置（自動切換）
├── nginx-https.conf        # HTTPS 配置（證書存在時使用）
└── nginx-http-only.conf    # HTTP 配置（申請證書時使用）

certbot/
├── conf/                   # SSL 證書存儲（自動保留）
│   └── live/
│       └── wc-project.duckdns.org/
│           ├── fullchain.pem
│           └── privkey.pem
└── www/                    # Let's Encrypt 驗證文件
```

### 設置腳本

```
setup-https.sh              # Linux/Mac 本地設置（已棄用）
setup-https.bat            # Windows 本地設置（已棄用）
setup-https-on-server.sh   # 服務器端設置（推薦使用）
```

### 文檔

```
docs/deployment/
├── HTTPS_COMPLETE_GUIDE.md    # 本文件（完整指南）
├── HTTPS_PERSISTENCE.md        # 持久化說明
├── DOCKER_HTTPS_SETUP.md      # Docker 環境設置
├── FREE_DOMAIN_SETUP.md        # 免費域名設置
└── HTTPS_WITHOUT_DOMAIN.md    # 無域名方案
```

---

## ✅ 檢查清單

### 首次設置

- [ ] 域名已設置（DuckDNS）
- [ ] DNS 已生效
- [ ] SSL 證書已申請
- [ ] Nginx 已切換到 HTTPS 配置
- [ ] HTTPS 訪問正常
- [ ] LINE Bot Webhook 已設置

### 後續部署

- [ ] 執行 `deploy-to-server.bat`
- [ ] 確認證書自動保留
- [ ] 確認配置自動切換
- [ ] 測試 HTTPS 訪問

---

## 🎯 總結

### 關鍵要點

1. **HTTPS 設定一次即可**，後續部署自動保留
2. **證書自動續期**，無需手動操作
3. **部署腳本自動檢測**，無需手動切換配置
4. **一鍵部署可用**，無需額外步驟

### 快速參考

| 操作 | 首次 | 後續 |
|------|------|------|
| 設置 HTTPS | ✅ 需要 | ❌ 不需要 |
| 申請證書 | ✅ 需要 | ❌ 不需要 |
| 一鍵部署 | ✅ 可用 | ✅ 可用 |
| 證書續期 | - | ✅ 自動 |

---

## 📞 需要協助？

如果遇到問題：
1. 查看本指南的「故障排除」章節
2. 檢查服務日誌：`docker compose logs`
3. 確認證書狀態：`ls -la certbot/conf/live/wc-project.duckdns.org/`

---

**最後更新**：2025-11-27  
**當前狀態**：✅ HTTPS 已啟用並運行中

