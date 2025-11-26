# Ubuntu 22.04 防火牆配置

## 安裝和啟用 UFW

### 步驟 1: 安裝 UFW（如果沒有安裝）

```bash
sudo apt update
sudo apt install -y ufw
```

### 步驟 2: 檢查 UFW 狀態

```bash
sudo ufw status
```

如果顯示 `Status: inactive`，需要啟用它。

### 步驟 3: 啟用 UFW

```bash
# 啟用 UFW
sudo ufw enable

# 確認狀態
sudo ufw status
```

### 步驟 4: 開放所需端口

```bash
# 開放 80 端口（HTTP）
sudo ufw allow 80/tcp

# 開放 8080 端口（後端 API）
sudo ufw allow 8080/tcp

# 如果需要 SSH，確保 22 端口已開放（通常預設已開放）
sudo ufw allow 22/tcp

# 重新載入規則
sudo ufw reload
```

### 步驟 5: 驗證規則

```bash
# 查看所有規則
sudo ufw status verbose

# 應該會看到類似：
# Status: active
# To                         Action      From
# --                         ------      ----
# 22/tcp                     ALLOW       Anywhere
# 80/tcp                     ALLOW       Anywhere
# 8080/tcp                   ALLOW       Anywhere
```

## 常用 UFW 命令

```bash
# 查看狀態
sudo ufw status

# 查看詳細狀態
sudo ufw status verbose

# 查看編號的規則列表
sudo ufw status numbered

# 刪除規則（使用編號）
sudo ufw delete [規則編號]

# 刪除規則（使用規則內容）
sudo ufw delete allow 80/tcp

# 禁用 UFW（不推薦）
sudo ufw disable

# 重新啟用
sudo ufw enable

# 重新載入規則
sudo ufw reload
```

## 注意事項

1. **啟用 UFW 前確保 SSH 端口已開放**，否則可能無法遠程連接
2. 如果已經通過 SSH 連接，啟用 UFW 不會中斷現有連接
3. 建議先開放 SSH 端口（22），再啟用 UFW

## 如果遇到問題

### 問題 1: 無法安裝 ufw

```bash
# 更新套件列表
sudo apt update

# 清理套件緩存
sudo apt clean

# 重新安裝
sudo apt install --reinstall ufw
```

### 問題 2: 啟用後無法連接

如果啟用 UFW 後無法通過 SSH 連接，可能需要：

1. 通過雲服務商的 VNC 或控制台登入
2. 或者聯繫服務商重置防火牆規則

### 問題 3: 規則不生效

```bash
# 重新載入規則
sudo ufw reload

# 或重啟 UFW
sudo ufw disable
sudo ufw enable
```

