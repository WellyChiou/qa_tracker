# SSH 免密碼登入設置指南

本指南說明如何設置 SSH 免密碼登入到遠端虛擬主機，避免每次登入都需要輸入密碼。這在部署和管理服務器時非常有用。

## 前置需求

- macOS 系統（本指南以 macOS 為例）
- 已安裝 `sshpass`（用於自動輸入密碼）
- 遠端主機的 IP 地址、用戶名和密碼

## 步驟 1: 檢查是否已有 SSH 金鑰

```bash
ls -la ~/.ssh/
```

如果看到 `id_rsa` 和 `id_rsa.pub` 文件，說明您已經有 SSH 金鑰了，可以跳過步驟 2。

## 步驟 2: 生成 SSH 金鑰（如果沒有）

如果沒有 SSH 金鑰，請執行以下命令生成：

```bash
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"
```

按 Enter 使用默認路徑（`~/.ssh/id_rsa`），然後可以選擇設置密碼或直接按 Enter 使用空密碼。

## 步驟 3: 查看公鑰內容

```bash
cat ~/.ssh/id_rsa.pub
```

複製輸出的公鑰內容（從 `ssh-rsa` 開始到結尾）。

## 步驟 4: 安裝 sshpass（如果尚未安裝）

在 macOS 上使用 Homebrew 安裝：

```bash
brew install sshpass
```

## 步驟 5: 將公鑰複製到遠端主機

使用 `sshpass` 和 `ssh-copy-id` 自動將公鑰複製到遠端主機：

```bash
sshpass -p '您的密碼' ssh-copy-id -o StrictHostKeyChecking=no root@您的IP地址
```

**範例：**
```bash
sshpass -p '!Welly775' ssh-copy-id -o StrictHostKeyChecking=no root@38.54.89.136
```

**注意：** 如果密碼中包含特殊字符（如 `!`），請使用單引號包裹密碼。

## 步驟 6: 測試免密碼登入

執行以下命令測試是否設置成功：

```bash
ssh -o StrictHostKeyChecking=no root@您的IP地址 'echo "SSH 免密碼登入測試成功！"'
```

**範例：**
```bash
ssh -o StrictHostKeyChecking=no root@38.54.89.136 'echo "SSH 免密碼登入測試成功！"'
```

如果看到 "SSH 免密碼登入測試成功！" 的輸出，說明設置成功。

## 步驟 7: 正常使用

現在您可以直接使用以下命令登入，無需輸入密碼：

```bash
ssh root@您的IP地址
```

**範例：**
```bash
ssh root@38.54.89.136
```

## 進階：設置 SSH 配置檔（可選）

為了更方便地管理多個 SSH 連線，可以創建 SSH 配置檔：

1. 編輯或創建 `~/.ssh/config` 文件：

```bash
nano ~/.ssh/config
```

2. 添加以下內容：

```
Host myserver
    HostName 38.54.89.136
    User root
    IdentityFile ~/.ssh/id_rsa
    StrictHostKeyChecking no
```

3. 保存後，就可以使用簡短的命令登入：

```bash
ssh myserver
```

## 故障排除

### 問題 1: sshpass 命令找不到

**解決方案：** 安裝 sshpass
```bash
brew install sshpass
```

### 問題 2: 仍然需要輸入密碼

**可能原因：**
- 公鑰未正確複製到遠端主機
- 遠端主機的 `~/.ssh/authorized_keys` 文件權限不正確

**解決方案：**
1. 手動檢查遠端主機的 `~/.ssh/authorized_keys` 文件是否包含您的公鑰
2. 確保遠端主機的 `~/.ssh` 目錄權限為 700：
   ```bash
   ssh root@您的IP地址 'chmod 700 ~/.ssh'
   ```
3. 確保 `~/.ssh/authorized_keys` 文件權限為 600：
   ```bash
   ssh root@您的IP地址 'chmod 600 ~/.ssh/authorized_keys'
   ```

### 問題 3: 連接被拒絕

**可能原因：**
- IP 地址錯誤
- 遠端主機的 SSH 服務未運行
- 防火牆阻擋

**解決方案：**
1. 確認 IP 地址正確
2. 確認遠端主機的 SSH 服務正在運行
3. 檢查防火牆設置（參考 [Ubuntu 防火牆配置](../UBUNTU_FIREWALL.md)）

## 安全建議

1. **使用非 root 用戶：** 建議創建一個普通用戶進行 SSH 登入，而不是直接使用 root
2. **禁用密碼登入：** 設置完成後，可以在遠端主機上禁用密碼登入，只允許金鑰登入（更安全）
3. **保護私鑰：** 確保 `~/.ssh/id_rsa` 文件的權限為 600：
   ```bash
   chmod 600 ~/.ssh/id_rsa
   ```

## 相關文檔

- [快速部署指南](./QUICK_START.md) - 部署前建議先設置 SSH 免密碼登入
- [虛擬主機部署指南](./DEPLOY.md) - 詳細的部署說明
- [Ubuntu 防火牆配置](../UBUNTU_FIREWALL.md) - 防火牆相關設置
- [GitLab SSH 設置指南](../development/GITLAB_SSH_ALTERNATIVE.md) - GitLab 相關的 SSH 設置

