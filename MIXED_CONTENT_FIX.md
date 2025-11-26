# 🔒 Mixed Content 錯誤解決方案

## ❓ 問題說明

**錯誤訊息：**
```
Mixed Content: The page at 'https://...' was loaded over HTTPS, 
but requested an insecure resource 'http://...'. 
This request has been blocked.
```

**原因：**
- 原系統在 GitHub Pages 上（HTTPS）
- 新系統 API 是 HTTP
- 瀏覽器安全政策不允許 HTTPS 頁面請求 HTTP 資源

---

## ✅ 解決方案

### 方法 1: 在本地打開 tracker.html（最簡單，推薦）

**步驟：**

1. 下載 `tracker.html` 到本地電腦
2. 用瀏覽器直接打開（使用 `file://` 協議）
3. 這樣就不會有 Mixed Content 問題

**優點：**
- ✅ 最簡單，不需要任何設定
- ✅ 立即可以使用
- ✅ 不需要修改伺服器

**缺點：**
- ⚠️ 需要下載檔案到本地

---

### 方法 2: 在新系統設置 HTTPS（最佳長期方案）

#### 使用 Let's Encrypt 免費 SSL 證書

```bash
# SSH 到虛擬主機
ssh root@38.54.89.136

# 安裝 Certbot
sudo apt update
sudo apt install -y certbot

# 申請證書（如果有域名）
sudo certbot certonly --standalone -d your-domain.com

# 或使用 IP（需要其他方式，見下方）
```

#### 使用 Nginx 反向代理 + SSL

1. 安裝 Nginx
2. 配置反向代理
3. 設置 SSL 證書

**詳細步驟：**

```bash
# 安裝 Nginx
sudo apt install -y nginx

# 配置反向代理（/etc/nginx/sites-available/qa-tracker）
server {
    listen 80;
    server_name 38.54.89.136;  # 或您的域名
    
    # 前端
    location / {
        proxy_pass http://localhost:80;
    }
    
    # 後端 API
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

### 方法 3: 使用 Cloudflare Tunnel（推薦，免費）

如果您的虛擬主機可以安裝 Cloudflare Tunnel：

```bash
# 安裝 cloudflared
wget https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64
chmod +x cloudflared-linux-amd64
sudo mv cloudflared-linux-amd64 /usr/local/bin/cloudflared

# 建立隧道
cloudflared tunnel create qa-tracker

# 運行隧道（會提供 HTTPS URL）
cloudflared tunnel run qa-tracker
```

---

### 方法 4: 臨時解決方案 - 允許混合內容（不推薦，僅測試用）

**⚠️ 警告：這會降低安全性，僅用於測試！**

#### Chrome/Edge

1. 點擊網址列左側的鎖頭圖示
2. 點擊「網站設定」
3. 將「不安全內容」改為「允許」
4. 重新整理頁面

#### Firefox

1. 在網址列輸入 `about:config`
2. 搜尋 `security.mixed_content.block_active_content`
3. 設為 `false`
4. 重新整理頁面

---

### 方法 5: 修改代碼使用相對協議（臨時方案）

修改 `tracker.html` 中的 API 呼叫，使用相對協議：

```javascript
// 原來的（會導致 Mixed Content）
const apiBaseUrl = 'http://38.54.89.136:8080';

// 改為（自動使用當前頁面協議）
const apiBaseUrl = window.location.protocol === 'https:' 
  ? 'https://38.54.89.136:8080'  // 需要 HTTPS
  : 'http://38.54.89.136:8080';
```

但這需要新系統支援 HTTPS。

---

## 🎯 推薦方案

### 短期（立即使用）

**使用方法 1：在本地打開 tracker.html**

1. 從 GitHub 下載 `tracker.html`
2. 用瀏覽器打開
3. 輸入 API 地址：`http://38.54.89.136:8080`
4. 開始匯入

### 長期（生產環境）

**設置 HTTPS：**
- 如果有域名：使用 Let's Encrypt
- 如果只有 IP：使用 Cloudflare Tunnel 或 Nginx + 自簽證書

---

## 📝 快速操作指南

### 立即解決（5 分鐘）

1. **下載 tracker.html**
   ```bash
   # 在本地電腦
   cd ~/Downloads
   # 從 GitHub 下載 tracker.html
   ```

2. **用瀏覽器打開**
   - 直接雙擊 `tracker.html`
   - 或右鍵 → 開啟方式 → 瀏覽器

3. **執行匯入**
   - 登入 Firebase
   - 點擊「匯入到新系統」
   - 輸入：`http://38.54.89.136:8080`
   - 開始匯入

---

## 🔧 如果使用方法 1 仍有問題

### 檢查 CORS 設定

確保後端的 CORS 配置允許所有來源：

```java
// 應該已經在 CorsConfig.java 中設定
config.addAllowedOriginPattern("*");
```

### 檢查防火牆

```bash
# 確認 8080 端口已開放
sudo ufw status
sudo ss -tulpn | grep 8080
```

---

## 💡 其他選項

### 使用代理服務

如果不想設置 HTTPS，可以使用代理服務：
- ngrok（免費，有時間限制）
- Cloudflare Tunnel（免費，無限制）

---

## ✅ 總結

**最快解決方法：**
1. 下載 `tracker.html` 到本地
2. 用瀏覽器直接打開（file:// 協議）
3. 執行匯入

**長期解決方案：**
- 在新系統設置 HTTPS
- 或使用 Cloudflare Tunnel

這樣就可以避免 Mixed Content 錯誤了！

