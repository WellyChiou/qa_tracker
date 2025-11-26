# 🔌 ERR_CONNECTION_REFUSED 錯誤解決方案

## ❓ 錯誤說明

**錯誤訊息：**
```
GET http://38.54.89.136:8080/api/records/stats/in-progress 
net::ERR_CONNECTION_REFUSED
```

**原因：**
- 無法連接到虛擬主機的 8080 端口
- 可能是服務未運行、防火牆阻擋、或網路問題

---

## 🔍 診斷步驟

### 步驟 1: 檢查服務是否運行

在虛擬主機上執行：

```bash
# SSH 到虛擬主機
ssh root@38.54.89.136

# 檢查所有容器狀態
cd /root/project/work/docker-vue-java-mysql
docker compose ps
```

**應該看到：**
- `mysql_db` - Up
- `java_backend` - Up
- `vue_frontend` - Up

**如果後端沒有運行：**
```bash
# 啟動所有服務
docker compose up -d

# 或只啟動後端
docker compose up -d backend
```

### 步驟 2: 檢查後端日誌

```bash
# 查看後端日誌
docker compose logs backend

# 持續查看日誌
docker compose logs -f backend
```

**檢查是否有錯誤：**
- 資料庫連線錯誤
- 端口綁定錯誤
- 應用啟動失敗

### 步驟 3: 在虛擬主機上測試 API

```bash
# 在虛擬主機上測試（應該成功）
curl http://localhost:8080/api/records/stats/in-progress

# 或測試簡單的 API
curl http://localhost:8080/api/hello
```

**如果成功：** 表示服務正常，問題在網路或防火牆
**如果失敗：** 表示服務有問題，需要查看日誌

### 步驟 4: 檢查端口是否監聽

```bash
# 檢查 8080 端口是否在監聽
sudo ss -tulpn | grep 8080

# 或
sudo netstat -tulpn | grep 8080
```

**應該看到：**
```
tcp  0  0  0.0.0.0:8080  0.0.0.0:*  LISTEN  ...
```

### 步驟 5: 檢查防火牆

```bash
# 檢查 UFW 狀態
sudo ufw status

# 檢查是否開放 8080
sudo ufw status | grep 8080

# 如果沒有，開放端口
sudo ufw allow 8080/tcp
sudo ufw reload
```

### 步驟 6: 從本地測試連線

在您的 Mac 上執行：

```bash
# 測試端口是否開放
telnet 38.54.89.136 8080

# 或使用 curl
curl http://38.54.89.136:8080/api/records/stats/in-progress

# 或使用 nc
nc -zv 38.54.89.136 8080
```

**如果連線失敗：** 可能是防火牆或雲服務商安全組問題

---

## ✅ 解決方案

### 方案 1: 啟動服務（如果未運行）

```bash
# SSH 到虛擬主機
ssh root@38.54.89.136

# 進入專案目錄
cd /root/project/work/docker-vue-java-mysql

# 啟動所有服務
docker compose up -d --build

# 檢查狀態
docker compose ps

# 查看日誌
docker compose logs backend
```

### 方案 2: 檢查並修復後端錯誤

如果後端無法啟動，查看日誌：

```bash
# 查看詳細日誌
docker compose logs backend --tail=100

# 常見問題：
# 1. 資料庫連線失敗 → 檢查 MySQL 是否運行
# 2. 端口被佔用 → 檢查端口使用情況
# 3. 應用啟動失敗 → 檢查 Java 錯誤
```

### 方案 3: 開放防火牆端口

```bash
# Ubuntu/Debian (UFW)
sudo ufw allow 8080/tcp
sudo ufw reload
sudo ufw status

# 確認 8080 已開放
```

### 方案 4: 檢查雲服務商安全組

**重要：** 即使本地防火牆已開放，雲服務商的安全組也可能阻擋。

#### 阿里雲
1. 登入阿里雲控制台
2. 前往「ECS」→「網路與安全」→「安全組」
3. 找到您的實例的安全組
4. 添加入站規則：
   - 端口：8080
   - 協議：TCP
   - 來源：0.0.0.0/0（或您的 IP）

#### 騰訊雲
1. 登入騰訊雲控制台
2. 前往「雲伺服器」→「安全組」
3. 編輯安全組規則
4. 添加入站規則：端口 8080

#### AWS
1. 登入 AWS Console
2. 前往「EC2」→「Security Groups」
3. 編輯 Inbound Rules
4. 添加規則：Port 8080, Source 0.0.0.0/0

#### 其他雲服務商
類似操作，找到「安全組」或「防火牆規則」設定。

### 方案 5: 檢查 Docker 端口映射

確認 `docker-compose.yml` 中的端口映射正確：

```yaml
backend:
  ports:
    - "8080:8080"  # 確認這個映射存在
```

如果沒有，需要添加或修改。

---

## 🔧 快速診斷腳本

在虛擬主機上執行以下命令進行完整檢查：

```bash
#!/bin/bash
echo "=== 檢查 Docker 容器狀態 ==="
docker compose ps

echo -e "\n=== 檢查後端日誌（最後 20 行）==="
docker compose logs backend --tail=20

echo -e "\n=== 檢查端口監聽 ==="
sudo ss -tulpn | grep 8080

echo -e "\n=== 測試本地 API ==="
curl -s http://localhost:8080/api/records/stats/in-progress || echo "API 測試失敗"

echo -e "\n=== 檢查防火牆 ==="
sudo ufw status | grep 8080 || echo "8080 端口未在防火牆規則中"
```

---

## 🎯 逐步排查流程

### 1. 首先檢查服務狀態

```bash
ssh root@38.54.89.136
cd /root/project/work/docker-vue-java-mysql
docker compose ps
```

**如果後端不是 Up 狀態：**
```bash
docker compose up -d backend
docker compose logs backend
```

### 2. 在虛擬主機上測試

```bash
curl http://localhost:8080/api/records/stats/in-progress
```

**如果成功：** 繼續步驟 3
**如果失敗：** 查看日誌找出問題

### 3. 檢查防火牆

```bash
sudo ufw allow 8080/tcp
sudo ufw reload
```

### 4. 從本地測試

在您的 Mac 上：
```bash
curl http://38.54.89.136:8080/api/records/stats/in-progress
```

**如果成功：** 問題解決！
**如果失敗：** 檢查雲服務商安全組

### 5. 檢查雲服務商安全組

根據您的雲服務商，在控制台開放 8080 端口。

---

## 📝 常見錯誤和解決方案

### 錯誤 1: 容器無法啟動

**檢查：**
```bash
docker compose logs backend
```

**可能原因：**
- 資料庫未啟動
- 端口被佔用
- 應用程式錯誤

### 錯誤 2: 端口被佔用

**檢查：**
```bash
sudo ss -tulpn | grep 8080
sudo lsof -i :8080
```

**解決：**
- 停止佔用的程序
- 或修改 docker-compose.yml 使用其他端口

### 錯誤 3: 資料庫連線失敗

**檢查：**
```bash
docker compose ps mysql
docker compose logs mysql
```

**解決：**
- 確保 MySQL 容器運行
- 檢查資料庫連線配置

---

## ✅ 快速修復命令

如果服務未運行，執行：

```bash
# SSH 到虛擬主機
ssh root@38.54.89.136

# 進入專案目錄
cd /root/project/work/docker-vue-java-mysql

# 停止所有服務
docker compose down

# 重新啟動
docker compose up -d --build

# 等待服務啟動（約 30 秒）
sleep 30

# 檢查狀態
docker compose ps

# 測試 API
curl http://localhost:8080/api/records/stats/in-progress
```

---

## 🎯 檢查清單

- [ ] Docker 容器都在運行（`docker compose ps`）
- [ ] 後端日誌沒有錯誤（`docker compose logs backend`）
- [ ] 本地測試 API 成功（`curl http://localhost:8080/api/...`）
- [ ] 防火牆已開放 8080 端口（`sudo ufw status`）
- [ ] 雲服務商安全組已開放 8080 端口
- [ ] 從本地可以連線（`curl http://38.54.89.136:8080/api/...`）

---

## 💡 提示

**最常見的原因：**
1. 服務未啟動（最常見）
2. 雲服務商安全組未開放（第二常見）
3. 本地防火牆未開放

**建議檢查順序：**
1. 先檢查服務狀態
2. 再檢查防火牆
3. 最後檢查雲服務商安全組

祝您順利解決問題！🚀

