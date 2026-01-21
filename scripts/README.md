# 腳本目錄說明

本目錄包含專案的所有 Shell 腳本，按功能分類組織。

## 目錄結構

```
scripts/
├── deployment/      # 部署相關腳本
├── monitoring/      # 監控相關腳本
├── maintenance/     # 維護相關腳本
├── migration/       # 資料庫遷移腳本
├── setup/          # 設定相關腳本
└── diagnostics/     # 診斷相關腳本
```

## 各目錄說明

### deployment/ - 部署腳本

用於部署專案到本地或遠端伺服器。

- **deploy.sh** - 本地部署腳本（在伺服器上執行）
- **deploy-local.sh** - 本地開發環境部署腳本
- **deploy-to-server-v1.0.sh** / **deploy-to-server-v1.0.bat** - 一鍵部署到伺服器（Mac/Linux 和 Windows 版本）
- **deploy-to-server-v1.1.sh** / **deploy-to-server-v1.1.bat** - 一鍵部署到伺服器（Mac/Linux 和 Windows 版本，統一版）
- **remote_deploy.sh** - 遠端部署腳本（在伺服器上執行）
- **verify_local.sh** - 驗證本地部署狀態

**使用方式**：
```bash
# 本地部署
./scripts/deployment/deploy-local.sh

# 或使用根目錄的符號連結
./deploy_local.sh

# 部署到伺服器（Mac/Linux）
./scripts/deployment/deploy-to-server-v1.1.sh

# 部署到伺服器（Windows）
scripts\deployment\deploy-to-server-v1.1.bat
```

### monitoring/ - 監控腳本

用於監控系統狀態和服務運行情況。

- **monitor-frontend.sh** - 前端監控腳本（檢查前端服務狀態）
- **monitor-system.sh** - 系統監控腳本（檢查系統資源使用情況）
- **check-services.sh** - 檢查服務狀態腳本

**使用方式**：
```bash
# 監控前端
./scripts/monitoring/monitor-frontend.sh

# 監控系統
./scripts/monitoring/monitor-system.sh
```

### maintenance/ - 維護腳本

用於日常維護和清理工作。

- **cleanup-work.sh** - 清理 work 資料夾
- **cleanup-docker.sh** - 清理 Docker 資源
- **cleanup-crontab.sh** - 清理 crontab 任務
- **check_mysql_storage.sh** - 檢查 MySQL 存儲配置
- **check_and_migrate_correct_volume.sh** - 檢查並遷移 MySQL volume
- **mysql_restore_database.sh** - 還原 MySQL 資料庫

**使用方式**：
```bash
# 清理 Docker 資源
./scripts/maintenance/cleanup-docker.sh

# 檢查 MySQL 存儲
./scripts/maintenance/check_mysql_storage.sh
```

### migration/ - 遷移腳本

用於資料庫遷移和資料轉移。

- **migrate_mysql_from_volume.sh** - 從 volume 遷移 MySQL 資料
- **migrate_from_correct_volume.sh** - 從正確的 volume 遷移資料
- **check_mysql_volumes.sh** - 檢查 MySQL volumes

**使用方式**：
```bash
# 遷移 MySQL 資料
./scripts/migration/migrate_mysql_from_volume.sh
```

### setup/ - 設定腳本

用於初始設定和配置。

- **setup-https.sh** - 設定 HTTPS（本地）
- **setup-https-on-server.sh** - 在伺服器上設定 HTTPS
- **setup-https-on-server-new.sh** - 在伺服器上設定 HTTPS（新版本）
- **setup-prevention.sh** - 設定預防機制（前端白屏預防等）
- **install-cron-hotfix.sh** - 安裝 cron 修復

**使用方式**：
```bash
# 設定 HTTPS
./scripts/setup/setup-https-on-server-new.sh

# 設定預防機制
./scripts/setup/setup-prevention.sh
```

### diagnostics/ - 診斷腳本

用於診斷和修復問題。

- **diagnose-frontend.sh** - 診斷前端問題
- **fix-frontend.sh** - 修復前端問題

**使用方式**：
```bash
# 診斷前端問題
./scripts/diagnostics/diagnose-frontend.sh

# 修復前端問題
./scripts/diagnostics/fix-frontend.sh
```

## 根目錄符號連結

為了方便使用，根目錄保留了最常用腳本的符號連結：

- **deploy_local.sh** → `scripts/deployment/deploy-local.sh`
- **verify_local.sh** → `scripts/deployment/verify_local.sh`

## 注意事項

1. 所有腳本都應該在專案根目錄執行，或使用絕對路徑
2. Windows 版本的腳本（.bat）與對應的 .sh 腳本功能相同，只是平台不同
3. 執行腳本前請確認有適當的權限
4. 某些腳本需要 root 權限或特定的環境變數

## 腳本開發規範

1. 使用 `#!/bin/bash` 作為 shebang
2. 使用 `set -e` 確保錯誤時立即退出
3. 添加適當的註解和錯誤處理
4. 使用 `git mv` 移動腳本以保留 Git 歷史
