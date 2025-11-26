# 虛擬主機部署指南

## 📦 將專案上傳到虛擬主機

### 方法 1: 使用 SCP (推薦)

在本地電腦執行：

```bash
# 進入專案目錄
cd /Users/wellychiou/my-github/docker-vue-java-mysql

# 將整個專案資料夾上傳到虛擬主機
scp -r . username@your-server-ip:/path/to/destination/

# 例如：
# scp -r . user@192.168.1.100:/home/user/docker-vue-java-mysql/
```

### 方法 2: 使用 Git

如果虛擬主機有 Git：

```bash
# 在本地先初始化 Git（如果還沒有）
cd /Users/wellychiou/my-github/docker-vue-java-mysql
git init
git add .
git commit -m "Initial commit"

# 在虛擬主機上
git clone your-repo-url
```

### 方法 3: 使用壓縮檔

```bash
# 在本地打包
cd /Users/wellychiou/my-github
tar -czf docker-vue-java-mysql.tar.gz docker-vue-java-mysql/

# 上傳到虛擬主機
scp docker-vue-java-mysql.tar.gz username@your-server-ip:/path/to/destination/

# 在虛擬主機上解壓
ssh username@your-server-ip
cd /path/to/destination/
tar -xzf docker-vue-java-mysql.tar.gz
```

## 🖥️ 在虛擬主機上設置

### 1. SSH 連接到虛擬主機

```bash
ssh username@your-server-ip
```

### 2. 確認 Docker 和 Docker Compose 已安裝

```bash
docker --version
docker-compose --version
```

如果沒有安裝，請參考以下安裝步驟：

#### 安裝 Docker (Ubuntu/Debian)

```bash
# 更新套件列表
sudo apt update

# 安裝必要的套件
sudo apt install -y apt-transport-https ca-certificates curl gnupg lsb-release

# 添加 Docker 官方 GPG key
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# 設置穩定版倉庫
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 安裝 Docker Engine
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# 啟動 Docker
sudo systemctl start docker
sudo systemctl enable docker

# 將當前用戶加入 docker 群組（避免每次都要 sudo）
sudo usermod -aG docker $USER
# 需要重新登入才會生效
```

#### 安裝 Docker Compose (如果使用舊版本)

```bash
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 3. 進入專案目錄

```bash
cd /path/to/docker-vue-java-mysql
```

### 4. 修改配置（如果需要）

#### 修改端口（如果 80、8080、3306 已被佔用）

編輯 `docker-compose.yml`：

```yaml
frontend:
  ports:
    - "3000:80"  # 改為其他端口，例如 3000

backend:
  ports:
    - "8081:8080"  # 改為其他端口，例如 8081

mysql:
  ports:
    - "3307:3306"  # 改為其他端口，例如 3307
```

#### 修改前端 API 地址

如果您的虛擬主機有域名，需要修改 `frontend/app/index.html` 中的 API 地址：

```javascript
// 將 localhost 改為您的域名或 IP
fetch('http://your-domain.com:8080/api/hello')
// 或
fetch('http://your-server-ip:8080/api/hello')
```

### 5. 啟動服務

```bash
# 構建並啟動所有服務
docker-compose up -d --build

# -d 參數表示在背景執行（detached mode）
```

### 6. 檢查服務狀態

```bash
# 查看所有容器狀態
docker-compose ps

# 查看日誌
docker-compose logs

# 查看特定服務日誌
docker-compose logs backend
docker-compose logs frontend
docker-compose logs mysql
```

### 7. 配置防火牆

如果虛擬主機有防火牆，需要開放端口：

```bash
# Ubuntu/Debian (ufw)
sudo ufw allow 80/tcp
sudo ufw allow 8080/tcp
sudo ufw allow 3306/tcp
sudo ufw reload

# CentOS/RHEL (firewalld)
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --permanent --add-port=3306/tcp
sudo firewall-cmd --reload
```

## 🌐 訪問您的應用

### 如果使用 IP 地址

- **前端**: http://your-server-ip
- **後端 API**: http://your-server-ip:8080/api/hello

### 如果使用域名

- **前端**: http://your-domain.com
- **後端 API**: http://your-domain.com:8080/api/hello

### 配置 Nginx 反向代理（可選，推薦）

如果您想使用標準端口（80/443）且不想暴露後端端口，可以設置 Nginx 反向代理：

```nginx
# /etc/nginx/sites-available/your-app
server {
    listen 80;
    server_name your-domain.com;

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

## 🔧 常用命令

```bash
# 停止所有服務
docker-compose down

# 停止並刪除資料卷（會清除資料庫資料）
docker-compose down -v

# 重新啟動服務
docker-compose restart

# 查看資源使用情況
docker stats

# 進入容器內部（除錯用）
docker-compose exec backend bash
docker-compose exec mysql mysql -u appuser -p testdb
```

## ⚠️ 注意事項

1. **安全性**：
   - 修改預設密碼（在 `docker-compose.yml` 中）
   - 不要將 MySQL 的 3306 端口對外開放（僅內部使用）
   - 考慮使用 SSL/TLS 證書

2. **資源限制**：
   - 確保虛擬主機有足夠的記憶體和 CPU
   - 可以考慮在 `docker-compose.yml` 中添加資源限制

3. **資料備份**：
   - 定期備份 MySQL 資料卷
   - 使用 `docker-compose exec mysql mysqldump -u root -p testdb > backup.sql`

4. **日誌管理**：
   - 定期清理日誌，避免佔用過多空間
   - 可以使用日誌輪轉工具

## 🐛 故障排除

### 端口被佔用

```bash
# 查看端口使用情況
sudo netstat -tulpn | grep :80
sudo netstat -tulpn | grep :8080

# 停止佔用端口的服務或修改 docker-compose.yml 中的端口
```

### 容器無法啟動

```bash
# 查看詳細日誌
docker-compose logs --tail=100

# 檢查 Docker 狀態
sudo systemctl status docker
```

### 無法連接資料庫

```bash
# 檢查 MySQL 容器是否正常運行
docker-compose ps mysql

# 檢查 MySQL 日誌
docker-compose logs mysql

# 測試資料庫連接
docker-compose exec mysql mysql -u appuser -papppassword testdb
```

## 📝 下一步

部署成功後，您可以：

1. 配置域名和 SSL 證書
2. 設置自動備份
3. 配置監控和日誌收集
4. 開始開發您的應用功能

祝部署順利！🎉

