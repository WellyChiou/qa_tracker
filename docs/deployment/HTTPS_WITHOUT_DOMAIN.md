# 沒有網域時使用 HTTPS 的方案

## 🎯 方案 1：使用 ngrok（最簡單，推薦測試用）

### 優點
- ✅ 完全免費
- ✅ 5 分鐘內完成設置
- ✅ LINE Bot 完全支援
- ✅ 提供有效的 HTTPS 證書

### 缺點
- ⚠️ 免費版 URL 每次重啟會變動
- ⚠️ 不適合長期生產環境

### 安裝步驟

#### Windows 系統

1. **下載 ngrok**
   - 前往 https://ngrok.com/download
   - 下載 Windows 版本
   - 解壓縮到任意目錄（例如：`C:\ngrok`）

2. **註冊帳號（免費）**
   - 前往 https://dashboard.ngrok.com/signup
   - 註冊免費帳號
   - 獲取您的 authtoken

3. **配置 ngrok**
   ```cmd
   # 在命令提示字元中執行
   ngrok config add-authtoken YOUR_AUTH_TOKEN
   ```

4. **啟動 ngrok**
   ```cmd
   # 創建 HTTPS 隧道，指向後端 8080 端口
   ngrok http 8080
   ```

5. **獲取 HTTPS URL**
   - ngrok 會顯示類似這樣的 URL：
     ```
     Forwarding  https://xxxx-xxxx-xxxx.ngrok-free.app -> http://localhost:8080
     ```
   - 複製這個 HTTPS URL（例如：`https://abc123.ngrok-free.app`）

6. **更新 docker-compose.yml**
   ```yaml
   LINE_BOT_WEBHOOK_URL: https://abc123.ngrok-free.app/api/line/webhook
   ```

7. **在 LINE Developers Console 設置 Webhook**
   - Webhook URL: `https://abc123.ngrok-free.app/api/line/webhook`
   - 點擊 "Verify" 驗證

#### Linux 系統

```bash
# 1. 下載 ngrok
wget https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-amd64.tgz
tar xvzf ngrok-v3-stable-linux-amd64.tgz
sudo mv ngrok /usr/local/bin/

# 2. 配置 authtoken（從 https://dashboard.ngrok.com 獲取）
ngrok config add-authtoken YOUR_AUTH_TOKEN

# 3. 啟動 ngrok（在背景執行）
nohup ngrok http 8080 > /dev/null 2>&1 &

# 4. 查看 URL
curl http://localhost:4040/api/tunnels
```

### 保持 URL 不變（付費版）

如果您需要固定 URL，可以：
- 升級到 ngrok 付費版（約 $8/月）
- 或使用方案 2

---

## 🎯 方案 2：使用免費域名服務

### 選項 A：Freenom（免費 .tk/.ml/.ga 域名）

1. 前往 https://www.freenom.com
2. 註冊帳號
3. 搜尋並註冊免費域名（例如：`yourname.tk`）
4. 將域名 A 記錄指向 `38.54.89.136`
5. 使用 Let's Encrypt 申請免費 SSL 證書

### 選項 B：No-IP 或 DuckDNS（動態域名）

1. 註冊免費動態域名服務
2. 設置域名指向您的 IP
3. 使用 Let's Encrypt 申請 SSL 證書

---

## 🎯 方案 3：自簽名證書（不推薦）

⚠️ **警告**：LINE Bot **可能不接受**自簽名證書，因為瀏覽器和 LINE 會認為證書不可信。

### 如果仍想嘗試

```bash
# 創建證書目錄
mkdir -p nginx/ssl

# 生成自簽名證書
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/ssl/server.key \
  -out nginx/ssl/server.crt \
  -subj "/C=TW/ST=State/L=City/O=Organization/CN=38.54.89.136"
```

然後配置 Nginx 使用此證書（參考 HTTPS_SETUP.md）

---

## 🎯 方案 4：購買便宜域名（長期方案）

### 推薦域名註冊商

1. **Namecheap** - 約 $10-15/年
2. **GoDaddy** - 約 $12-20/年
3. **Cloudflare** - 約 $8-12/年（最便宜）

### 購買後

1. 將域名 A 記錄指向 `38.54.89.136`
2. 使用 Let's Encrypt 申請免費 SSL 證書
3. 配置 Nginx 反向代理

---

## 📊 方案比較

| 方案 | 費用 | 設置難度 | LINE Bot 支援 | 穩定性 | 推薦度 |
|------|------|----------|--------------|--------|--------|
| ngrok | 免費 | ⭐ 非常簡單 | ✅ 完全支援 | ⚠️ URL 會變 | ⭐⭐⭐⭐⭐ 測試用 |
| ngrok 付費 | $8/月 | ⭐ 非常簡單 | ✅ 完全支援 | ✅ 固定 URL | ⭐⭐⭐⭐ |
| 免費域名 | 免費 | ⭐⭐⭐ 中等 | ✅ 完全支援 | ✅ 穩定 | ⭐⭐⭐⭐ |
| 購買域名 | $10/年 | ⭐⭐⭐ 中等 | ✅ 完全支援 | ✅ 最穩定 | ⭐⭐⭐⭐⭐ |
| 自簽名證書 | 免費 | ⭐⭐ 簡單 | ❌ 可能不支援 | ✅ 穩定 | ⭐ 不推薦 |

---

## 🚀 快速開始（推薦：ngrok）

### 立即開始使用 ngrok

1. **下載並安裝 ngrok**
   ```bash
   # Windows: 從官網下載
   # Linux: 
   wget https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-amd64.tgz
   tar xvzf ngrok-v3-stable-linux-amd64.tgz
   sudo mv ngrok /usr/local/bin/
   ```

2. **註冊並獲取 authtoken**
   - 前往 https://dashboard.ngrok.com/signup
   - 註冊後獲取 authtoken

3. **配置 ngrok**
   ```bash
   ngrok config add-authtoken YOUR_AUTH_TOKEN
   ```

4. **啟動 ngrok**
   ```bash
   ngrok http 8080
   ```

5. **複製 HTTPS URL**
   - 例如：`https://abc123.ngrok-free.app`

6. **更新配置**
   ```yaml
   # docker-compose.yml
   LINE_BOT_WEBHOOK_URL: https://abc123.ngrok-free.app/api/line/webhook
   ```

7. **在 LINE Console 設置 Webhook**
   - URL: `https://abc123.ngrok-free.app/api/line/webhook`
   - 點擊 "Verify"

---

## ⚠️ 重要提醒

### ngrok 免費版限制

- URL 每次重啟會變動
- 需要定期更新 LINE Console 的 Webhook URL
- 如果服務器重啟，需要重新啟動 ngrok 並更新 URL

### 解決方案

1. **使用 ngrok 付費版**（固定 URL）
2. **使用 systemd 服務**（自動啟動 ngrok）
3. **購買便宜域名**（一次性解決）

---

## 🔧 自動啟動 ngrok（Linux）

創建 systemd 服務文件 `/etc/systemd/system/ngrok.service`：

```ini
[Unit]
Description=ngrok tunnel
After=network.target

[Service]
Type=simple
User=root
ExecStart=/usr/local/bin/ngrok http 8080
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

啟動服務：
```bash
sudo systemctl enable ngrok
sudo systemctl start ngrok
```

---

## 📝 總結

**沒有網域也可以使用 HTTPS！**

**推薦方案**：
- **測試階段**：使用 ngrok（免費，5 分鐘設置）
- **生產環境**：購買便宜域名 + Let's Encrypt（$10/年，最穩定）

**不推薦**：自簽名證書（LINE Bot 可能不接受）

