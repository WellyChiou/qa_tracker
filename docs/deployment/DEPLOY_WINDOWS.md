# Windows 部署腳本使用說明

## 文件說明

- `deploy-to-server.bat` - 批處理文件版本（基本功能）
- `deploy-to-server.ps1` - PowerShell 版本（推薦使用，功能更強大）

## 系統要求

### 必要工具

1. **tar 命令**
   - Windows 10 1803+ 內建
   - 或安裝 Git for Windows（包含 tar 工具）

2. **scp 和 ssh 命令**
   - Windows 10 1809+ 內建 OpenSSH
   - 或安裝 Git for Windows（包含 OpenSSH 工具）

### 檢查工具是否安裝

在 PowerShell 或命令提示符中執行：

```powershell
# 檢查 tar
tar --version

# 檢查 scp
scp -V

# 檢查 ssh
ssh -V
```

## 使用方法

### 方法 1: 使用 PowerShell 腳本（推薦）

1. 右鍵點擊 `deploy-to-server.ps1`
2. 選擇「使用 PowerShell 執行」
3. 或在 PowerShell 中執行：
   ```powershell
   .\deploy-to-server.ps1
   ```

### 方法 2: 使用批處理文件

1. 雙擊 `deploy-to-server.bat`
2. 或在命令提示符中執行：
   ```cmd
   deploy-to-server.bat
   ```

## SSH 密碼認證

腳本執行時會提示輸入 SSH 密碼。預設密碼為：`!Welly775`

**注意**：Windows 的 OpenSSH 不支持直接在命令行傳遞密碼，需要手動輸入。

## 配置 SSH 密鑰認證（推薦）

為了避免每次都要輸入密碼，建議配置 SSH 密鑰認證：

### 步驟 1: 生成 SSH 密鑰（如果還沒有）

在 PowerShell 中執行：

```powershell
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"
```

按 Enter 使用預設路徑，或指定自定義路徑。

### 步驟 2: 複製公鑰到服務器

```powershell
# 方法 1: 使用 ssh-copy-id（如果可用）
ssh-copy-id root@38.54.89.136

# 方法 2: 手動複製
type $env:USERPROFILE\.ssh\id_rsa.pub | ssh root@38.54.89.136 "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"
```

輸入密碼：`!Welly775`

### 步驟 3: 測試 SSH 連接

```powershell
ssh root@38.54.89.136
```

如果不需要輸入密碼就能連接，說明配置成功。

## 使用 PuTTY（替代方案）

如果您使用 PuTTY 而不是 OpenSSH：

1. 下載並安裝 PuTTY：https://www.putty.org/
2. 將 PuTTY 安裝目錄添加到系統 PATH
3. 使用 `pscp` 和 `plink` 替代 `scp` 和 `ssh`

### 修改腳本使用 PuTTY

在腳本中將：
- `scp` 改為 `pscp`
- `ssh` 改為 `plink`

並添加 `-pw` 參數傳遞密碼：

```batch
pscp -pw %SERVER_PASSWORD% "%ARCHIVE_NAME%" %SERVER_USER%@%SERVER_IP%:%REMOTE_PATH%/
plink -pw %SERVER_PASSWORD% %SERVER_USER%@%SERVER_IP% "command"
```

## 故障排除

### 問題 1: 找不到 tar 命令

**解決方案**：
- 確保使用 Windows 10 1803 或更高版本
- 或安裝 Git for Windows：https://git-scm.com/download/win

### 問題 2: 找不到 scp/ssh 命令

**解決方案**：
- 確保使用 Windows 10 1809 或更高版本
- 或安裝 Git for Windows
- 或在「設定」>「應用程式」>「選用功能」中安裝 OpenSSH 用戶端

### 問題 3: SSH 連接被拒絕

**解決方案**：
- 檢查服務器 IP 是否正確
- 檢查防火牆設置
- 確認服務器 SSH 服務正在運行

### 問題 4: 上傳或執行遠程命令失敗

**解決方案**：
- 確認 SSH 密碼正確
- 檢查服務器磁盤空間
- 查看服務器日誌：`ssh root@38.54.89.136 "tail -f /var/log/syslog"`

## 腳本配置

如需修改配置，請編輯腳本中的以下變量：

```batch
set SERVER_IP=38.54.89.136
set SERVER_USER=root
set SERVER_PASSWORD=!Welly775
set PROJECT_NAME=docker-vue-java-mysql
set REMOTE_PATH=/root/project/work
```

或 PowerShell 版本：

```powershell
$SERVER_IP = "38.54.89.136"
$SERVER_USER = "root"
$SERVER_PASSWORD = "!Welly775"
$PROJECT_NAME = "docker-vue-java-mysql"
$REMOTE_PATH = "/root/project/work"
```

## 安全建議

⚠️ **重要**：腳本中包含明文密碼，請注意：

1. **不要將腳本提交到公開的 Git 倉庫**
2. **使用環境變量存儲敏感信息**（見下方）
3. **配置 SSH 密鑰認證**，避免在腳本中存儲密碼

### 使用環境變量（推薦）

修改腳本，從環境變量讀取密碼：

**批處理版本**：
```batch
set SERVER_PASSWORD=%DEPLOY_PASSWORD%
```

**PowerShell 版本**：
```powershell
$SERVER_PASSWORD = $env:DEPLOY_PASSWORD
```

然後在執行前設置環境變量：

```powershell
$env:DEPLOY_PASSWORD = "!Welly775"
.\deploy-to-server.ps1
```

