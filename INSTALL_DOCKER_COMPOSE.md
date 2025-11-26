# Docker Compose 安裝指南

## 方法 1: 安裝 Docker Compose Plugin（推薦，新版本）

這是官方推薦的方式，使用 `docker compose`（注意沒有連字號）而不是 `docker-compose`。

### 步驟：

```bash
# 1. 更新套件列表
sudo apt update

# 2. 安裝 Docker Compose Plugin
sudo apt install -y docker-compose-plugin

# 3. 驗證安裝（注意：是 docker compose，不是 docker-compose）
docker compose version
```

安裝完成後，使用方式稍有不同：
- 舊版：`docker-compose up`
- 新版：`docker compose up`（注意沒有連字號）

## 方法 2: 安裝獨立版本的 Docker Compose（舊方式）

如果您偏好使用 `docker-compose` 指令（有連字號），可以使用以下方式：

### 步驟：

```bash
# 1. 下載最新版本的 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# 2. 添加執行權限
sudo chmod +x /usr/local/bin/docker-compose

# 3. 驗證安裝
docker-compose --version
```

## 如果遇到問題

### 問題 1: curl 命令找不到

```bash
# 安裝 curl
sudo apt update
sudo apt install -y curl
```

### 問題 2: 權限不足

確保使用 `sudo` 執行安裝命令。

### 問題 3: 下載速度慢（在中國大陸）

可以使用國內鏡像：

```bash
# 使用國內鏡像下載
sudo curl -L "https://get.daocloud.io/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
docker-compose --version
```

## 驗證安裝

安裝完成後，執行：

```bash
# 方法 1 的驗證方式
docker compose version

# 方法 2 的驗證方式
docker-compose --version
```

應該會看到類似以下的輸出：
```
Docker Compose version v2.x.x
```

## 使用方式

### 如果安裝了方法 1（Plugin 版本）

```bash
# 啟動服務
docker compose up -d --build

# 停止服務
docker compose down

# 查看日誌
docker compose logs
```

### 如果安裝了方法 2（獨立版本）

```bash
# 啟動服務
docker-compose up -d --build

# 停止服務
docker-compose down

# 查看日誌
docker-compose logs
```

## 注意事項

- **方法 1（Plugin）**：是官方推薦的新方式，與 Docker 整合更好
- **方法 2（獨立版本）**：如果您習慣使用 `docker-compose` 指令，可以使用這個方式
- 兩種方式功能相同，只是指令稍有不同（有無連字號）

## 下一步

安裝完成後，回到專案目錄執行：

```bash
cd ~/project/work
docker compose up -d --build
# 或
docker-compose up -d --build
```

