# 圖片上傳存儲方案說明

## ⚠️ 重要：Docker 重新部署時的文件持久化問題

### 當前狀況

目前 `docker-compose.yml` 中：
- ✅ **MySQL 資料庫**：有使用 `mysql_data` volume，資料會持久化
- ❌ **後端容器**：**沒有 volume**，如果文件存儲在容器內，重新部署會**消失**

### 問題說明

如果圖片上傳到容器內的目錄（例如 `/app/uploads/`），當執行以下操作時，文件會消失：
- `docker compose down` 後重新 `docker compose up`
- `docker compose up --build`（重新構建）
- 刪除並重建容器

## 💡 解決方案

### 方案 1: 使用 Docker Volume（推薦）

**優點：**
- 文件持久化，重新部署不會丟失
- 與資料庫一樣的持久化機制
- 備份方便（備份 volume 目錄）

**實施方式：**

1. **在 `docker-compose.yml` 中添加 volume：**
```yaml
backend:
  volumes:
    - church_uploads:/app/uploads/church
    # 或者映射到主機目錄
    - ./uploads/church:/app/uploads/church
```

2. **在 volumes 區塊定義：**
```yaml
volumes:
  mysql_data:
  church_uploads:  # 新增
```

### 方案 2: 映射到主機目錄（推薦，更易備份）

**優點：**
- 文件存儲在主機文件系統
- 可以直接訪問和備份
- 重新部署不會丟失

**實施方式：**

在 `docker-compose.yml` 中：
```yaml
backend:
  volumes:
    - ./uploads/church:/app/uploads/church
```

文件會存儲在專案目錄的 `uploads/church/` 資料夾中。

### 方案 3: 使用 Nginx 提供靜態文件服務

**優點：**
- 文件存儲在主機，由 Nginx 直接提供服務
- 性能好，不需要經過後端
- 可以配置緩存

**實施方式：**

1. 在 `docker-compose.yml` 中：
```yaml
backend:
  volumes:
    - ./uploads/church:/app/uploads/church

nginx:
  volumes:
    - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
    - ./uploads/church:/usr/share/nginx/html/uploads/church:ro  # 新增
```

2. 在 Nginx 配置中添加：
```nginx
location /uploads/church/ {
    alias /usr/share/nginx/html/uploads/church/;
    expires 30d;
    add_header Cache-Control "public, immutable";
}
```

### 方案 4: 使用對象存儲服務（適合生產環境）

**優點：**
- 不依賴本地文件系統
- 可以擴展和備份
- 支援 CDN 加速

**選項：**
- AWS S3
- 阿里雲 OSS
- Google Cloud Storage
- 自建 MinIO

## 📋 推薦實施方案

### 階段 1：本地存儲（開發/測試）

使用 **方案 2（映射到主機目錄）** + **方案 3（Nginx 提供服務）**

**文件結構：**
```
qa_tracker/
├── uploads/
│   └── church/
│       ├── activities/      # 活動圖片
│       └── sunday-messages/ # 主日信息圖片
├── docker-compose.yml
└── ...
```

**優點：**
- 簡單易用
- 文件在主機，容易備份
- Nginx 直接提供服務，性能好

### 階段 2：生產環境（可選）

如果圖片很多或需要更好的性能，可以考慮：
- 使用對象存儲服務
- 或使用專用的文件服務器

## 🔧 實施步驟

### 1. 修改 docker-compose.yml

```yaml
backend:
  volumes:
    - ./uploads/church:/app/uploads/church  # 新增

nginx:
  volumes:
    - ./uploads/church:/usr/share/nginx/html/uploads/church:ro  # 新增
```

### 2. 創建上傳目錄

```bash
mkdir -p uploads/church/activities
mkdir -p uploads/church/sunday-messages
```

### 3. 配置 Nginx

在 Nginx 配置中添加靜態文件服務規則。

### 4. 實現上傳 API

後端 API 將文件保存到 `/app/uploads/church/` 目錄，返回 URL 為 `/uploads/church/...`

## 📝 注意事項

1. **文件權限**：確保容器有寫入權限
2. **文件清理**：考慮實現定期清理未使用的圖片
3. **備份策略**：將 `uploads/` 目錄納入備份計劃
4. **文件大小限制**：在 Nginx 和 Spring Boot 中設置上傳大小限制
5. **安全性**：驗證文件類型和大小，防止惡意文件上傳

## 🎯 總結

**推薦方案：使用主機目錄映射 + Nginx 提供服務**

這樣可以：
- ✅ 文件持久化，重新部署不會丟失
- ✅ 容易備份（備份 `uploads/` 目錄）
- ✅ 性能好（Nginx 直接提供服務）
- ✅ 簡單易維護

