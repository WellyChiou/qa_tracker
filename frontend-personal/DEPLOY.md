# 部署到虛擬主機指南

## 構建步驟

### 1. 安裝依賴（如果還沒安裝）

```bash
cd frontend
npm install
```

### 2. 構建生產版本

```bash
npm run build
```

這會創建一個 `dist/` 目錄，包含所有優化後的靜態文件。

### 3. 檢查構建結果

構建完成後，`dist/` 目錄應該包含：
- `index.html` - 主 HTML 文件
- `assets/` - 編譯後的 JS、CSS 和其他資源文件

## 部署到虛擬主機

### 方法 1: 直接上傳 dist 目錄

1. **構建項目**
   ```bash
   npm run build
   ```

2. **上傳 dist 目錄內容**
   - 將 `dist/` 目錄內的所有文件上傳到虛擬主機的 web 根目錄（通常是 `public_html/` 或 `www/`）
   - 確保 `index.html` 在根目錄

3. **配置後端 API 地址**
   - 如果後端 API 不在同一域名，需要修改 `src/composables/useApi.js` 中的 `API_BASE_URL`
   - 或者使用環境變量（見下方配置）

### 方法 2: 使用環境變量配置 API 地址

1. **創建環境變量文件**

創建 `.env.production` 文件：
```env
VITE_API_BASE_URL=https://your-backend-domain.com/api
```

2. **修改 useApi.js 使用環境變量**

```javascript
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 
  `${window.location.protocol}//${window.location.hostname}:8080/api`
```

3. **重新構建**
```bash
npm run build
```

## 虛擬主機配置

### Nginx 配置示例

如果使用 Nginx，需要配置：

```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /path/to/dist;
    index index.html;

    # 處理 Vue Router 的歷史模式
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理（如果需要）
    location /api {
        proxy_pass http://your-backend-server:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 靜態資源緩存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

### Apache 配置示例

如果使用 Apache，創建 `.htaccess` 文件在 `dist/` 目錄：

```apache
<IfModule mod_rewrite.c>
  RewriteEngine On
  RewriteBase /
  RewriteRule ^index\.html$ - [L]
  RewriteCond %{REQUEST_FILENAME} !-f
  RewriteCond %{REQUEST_FILENAME} !-d
  RewriteRule . /index.html [L]
</IfModule>
```

## 重要注意事項

1. **後端 API 地址**
   - 確保後端 API 可以從虛擬主機訪問
   - 如果後端在不同域名，需要配置 CORS
   - 或者使用反向代理

2. **HTTPS**
   - 生產環境建議使用 HTTPS
   - 確保 API 請求也使用 HTTPS

3. **文件權限**
   - 確保 web 服務器有讀取權限
   - 通常文件權限設置為 644，目錄為 755

4. **測試**
   - 部署後測試所有功能
   - 檢查瀏覽器控制台是否有錯誤
   - 確認 API 請求是否正常

## 快速部署命令

```bash
# 1. 構建
npm run build

# 2. 壓縮 dist 目錄（方便上傳）
cd dist
tar -czf ../dist.tar.gz .
# 或使用 zip
zip -r ../dist.zip .

# 3. 上傳 dist.tar.gz 或 dist.zip 到虛擬主機
# 4. 在虛擬主機上解壓到 web 根目錄
```

## 故障排除

### 問題：頁面空白
- 檢查 `index.html` 是否在正確位置
- 檢查瀏覽器控制台的錯誤信息
- 確認資源路徑是否正確

### 問題：路由不工作
- 確保配置了 URL 重寫規則（見上方 Nginx/Apache 配置）
- Vue Router 使用歷史模式，需要服務器支持

### 問題：API 請求失敗
- 檢查後端 API 是否可訪問
- 檢查 CORS 配置
- 檢查 API_BASE_URL 配置是否正確

