# 🚀 快速部署指南

## 前置條件檢查

在開始之前，確認您的虛擬主機已安裝：

```bash
# 檢查 Docker
docker --version

# 檢查 Docker Compose（注意：沒有連字號）
docker compose version
```

如果都顯示版本號，就可以開始了！

---

## 步驟 1: 將專案上傳到虛擬主機

### 方法 A: 使用 SCP（從您的 Mac 執行）

```bash
# 在您的 Mac 上執行
cd /Users/wellychiou/my-github/docker-vue-java-mysql

# 上傳整個專案資料夾到虛擬主機
scp -r . root@your-server-ip:/root/project/work/docker-vue-java-mysql/
```

**注意**：將 `your-server-ip` 替換為您的實際伺服器 IP 或域名。

### 方法 B: 使用壓縮檔（推薦，較快）

```bash
# 在您的 Mac 上執行
cd /Users/wellychiou/my-github
tar -czf docker-vue-java-mysql.tar.gz docker-vue-java-mysql/

# 上傳壓縮檔
scp docker-vue-java-mysql.tar.gz root@your-server-ip:/root/project/work/

# 然後 SSH 到虛擬主機解壓
ssh root@your-server-ip
cd /root/project/work
tar -xzf docker-vue-java-mysql.tar.gz
```

### 方法 C: 使用 Git（如果虛擬主機有 Git）

```bash
# 在本地先初始化 Git（如果還沒有）
cd /Users/wellychiou/my-github/docker-vue-java-mysql
git init
git add .
git commit -m "Initial commit"

# 在虛擬主機上
git clone your-repo-url
```

---

## 步驟 2: SSH 連接到虛擬主機

```bash
ssh root@your-server-ip
```

---

## 步驟 3: 進入專案目錄

```bash
cd /root/project/work/docker-vue-java-mysql
```

確認檔案都在：
```bash
ls -la
```

應該會看到：
- `docker-compose.yml`
- `frontend-personal/`
- `frontend-church/`
- `frontend-church-admin/`
- `backend-personal/`
- `backend-church/`
- `README.md`
等檔案

---

## 步驟 4: 檢查並配置端口（如果需要）

### 檢查端口是否被佔用

**方法 1: 使用 ss 命令（推薦，現代 Linux 系統）**

```bash
# 檢查 80 端口（前端）
sudo ss -tulpn | grep :80

# 檢查 8080 端口（後端）
sudo ss -tulpn | grep :8080

# 檢查 3306 端口（MySQL）
sudo ss -tulpn | grep :3306

# 或一次檢查所有端口
sudo ss -tulpn | grep -E ':(80|8080|3306)'
```

**方法 2: 使用 lsof 命令**

```bash
# 檢查 80 端口
sudo lsof -i :80

# 檢查 8080 端口
sudo lsof -i :8080
```

**方法 3: 安裝 netstat（如果需要）**

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install -y net-tools

# 然後就可以使用 netstat 了
sudo netstat -tulpn | grep :80
```

如果端口被佔用，有兩個選擇：

**選擇 1**: 停止佔用端口的服務
**選擇 2**: 修改 `docker-compose.yml` 中的端口映射

例如，如果 80 端口被佔用，可以修改為 3000：
```yaml
frontend:
  ports:
    - "3000:80"  # 改為 3000
```

---

## 步驟 5: 配置防火牆（重要！）

### 檢查您的系統類型

```bash
# 檢查系統類型
cat /etc/os-release
```

### 根據系統類型配置防火牆

**Ubuntu/Debian 系統（使用 ufw）:**

```bash
# 檢查 ufw 是否安裝
which ufw

# 如果沒有，先安裝
sudo apt update
sudo apt install -y ufw

# 開放端口
sudo ufw allow 80/tcp
sudo ufw allow 8080/tcp
sudo ufw reload

# 檢查狀態
sudo ufw status
```

**CentOS/RHEL 系統（使用 firewalld）:**

```bash
# 檢查 firewalld 狀態
sudo systemctl status firewalld

# 如果沒有運行，啟動它
sudo systemctl start firewalld
sudo systemctl enable firewalld

# 開放端口
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload

# 檢查狀態
sudo firewall-cmd --list-ports
```

**如果沒有防火牆工具:**

有些系統可能沒有啟用防火牆，或者使用其他工具。您可以：

1. **先跳過這步，直接啟動服務測試**
2. **檢查雲服務商的安全組/防火牆規則**（這很重要！）
   - 阿里雲、騰訊雲、AWS 等都有安全組設置
   - 需要在雲控制台開放 80 和 8080 端口

**檢查是否有其他防火牆:**

```bash
# 檢查 iptables（傳統 Linux 防火牆）
sudo iptables -L -n | grep -E '(80|8080)'

# 如果使用 iptables，可以這樣開放端口
sudo iptables -A INPUT -p tcp --dport 80 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 8080 -j ACCEPT
sudo iptables-save
```

---

## 步驟 6: 啟動所有服務

```bash
# 構建並啟動所有容器（在背景執行）
docker compose up -d --build
```

這個命令會：
1. 下載所需的 Docker 映像（MySQL、Maven、OpenJDK、Nginx）
2. 構建 Java 後端應用
3. 啟動 MySQL 資料庫
4. 啟動 Java 後端
5. 啟動 Vue 前端

**第一次執行可能需要 3-5 分鐘**，請耐心等待。

---

## 步驟 7: 檢查服務狀態

```bash
# 查看所有容器是否正常運行
docker compose ps
```

應該會看到三個容器都是 `Up` 狀態：
- `mysql_db` - MySQL 資料庫
- `java_backend_personal` - 個人系統後端
- `java_backend_church` - 教會系統後端
- `vue_personal` - 個人系統前端
- `vue_frontend_church` - 教會前台
- `vue_frontend_church_admin` - 教會後台

### 查看日誌（如果有問題）

```bash
# 查看所有服務日誌
docker compose logs

# 查看特定服務日誌
docker compose logs backend-personal
docker compose logs backend-church
docker compose logs frontend-personal
docker compose logs mysql

# 即時查看日誌
docker compose logs -f
```

---

## 步驟 8: 驗證部署成功

### 方法 1: 在瀏覽器訪問

打開瀏覽器，訪問：

- **前端頁面**: `http://your-server-ip`
  - 應該會看到漂亮的 "Hello World!" 頁面
  - 如果修改了端口（例如改為 3000），則訪問 `http://your-server-ip:3000`

- **後端 API**: `http://your-server-ip/api/hello`
  - 應該會看到 JSON 回應：
  ```json
  {
    "message": "Hello World!",
    "status": "success",
    "service": "Java Spring Boot Backend"
  }
  ```

### 方法 2: 使用 curl 測試（在虛擬主機上）

```bash
# 測試前端
curl http://localhost

# 測試後端 API
curl http://localhost/api/hello
```

---

## 常見問題排除

### 問題 1: 容器無法啟動

```bash
# 查看詳細錯誤訊息
docker compose logs

# 檢查 Docker 是否正常運行
sudo systemctl status docker
```

### 問題 2: 端口被佔用

```bash
# 查看哪個程序佔用了端口（使用 ss）
sudo ss -tulpn | grep :80
sudo ss -tulpn | grep :8080

# 或使用 lsof
sudo lsof -i :80
sudo lsof -i :8080

# 停止佔用的服務或修改 docker-compose.yml 中的端口
```

### 問題 3: 無法從外部訪問

1. 檢查防火牆是否已開放端口
2. 檢查雲服務商的安全組/防火牆規則
3. 確認服務是否正常運行：`docker compose ps`

### 問題 4: 後端無法連接資料庫

```bash
# 檢查 MySQL 容器是否正常
docker compose ps mysql

# 查看 MySQL 日誌
docker compose logs mysql

# 測試資料庫連接
docker compose exec mysql mysql -u appuser -papppassword testdb
```

---

## 常用命令

```bash
# 停止所有服務
docker compose down

# 停止並刪除資料卷（會清除資料庫資料）
docker compose down -v

# 重新啟動服務
docker compose restart

# 重新構建並啟動
docker compose up -d --build

# 查看資源使用情況
docker stats

# 進入容器內部（除錯用）
docker compose exec backend-personal bash
docker compose exec backend-church bash
docker compose exec mysql bash
```

---

## 成功標誌

當您看到以下情況，代表部署成功：

✅ 瀏覽器訪問 `http://your-server-ip` 看到 "Hello World!" 頁面  
✅ 瀏覽器訪問 `http://your-server-ip/api/hello` 看到 JSON 回應  
✅ `docker compose ps` 顯示 personal / church 後端與前端容器都是 `Up` 狀態  

---

## 一鍵部署（推薦）

如果您使用 Windows 或 Mac，可以使用一鍵部署腳本：

- **Windows**: `deploy-to-server.bat`
- **Mac/Linux**: `deploy-to-server-v1.1.sh`

這些腳本會自動：
1. 打包專案
2. 上傳到伺服器
3. 執行遠端部署腳本
4. 設置預防機制（監控、自動修復）

詳細說明請參考：[一鍵部署（含預防機制）](./DEPLOYMENT_WITH_PREVENTION.md)

## 下一步

部署成功後，您可以：

1. 開始開發您的 Vue 前端應用
2. 擴展 Java 後端 API
3. 設計 MySQL 資料庫結構
4. 配置域名和 SSL 證書（參考 [HTTPS 完整指南](./HTTPS_COMPLETE_GUIDE.md)）

祝您部署順利！🎉
