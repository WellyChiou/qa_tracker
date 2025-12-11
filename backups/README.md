# 資料庫備份說明

## 概述

資料庫備份功能已整合到後端 Java 應用中，透過容器內執行的備份腳本自動備份 `qa_tracker` 和 `church` 兩個資料庫。

## 備份腳本位置

- **`backend/backup-database-container.sh`** - 容器內備份腳本
  - 由 Dockerfile 複製到容器內的 `/app/backup-database.sh`
  - 透過後端 Java 應用執行（排程任務或手動觸發）

## 使用方式

### 透過後台維護頁面

1. 登入教會後台
2. 前往「系統設定」→「系統維護」
3. 切換到「備份管理」標籤
4. 點擊「創建備份」按鈕手動執行備份
5. 查看備份列表、下載或刪除備份檔案

### 自動備份排程

備份會透過後端的排程任務自動執行，排程時間可在系統參數中設定。

## 配置參數

備份功能會從資料庫的 `system_settings` 表讀取以下配置：

- `backup.enabled` - 是否啟用自動備份（預設：true）
- `backup.mysql_service` - MySQL 服務名稱（預設：mysql）
- `backup.mysql_root_password` - MySQL root 密碼（預設：rootpassword）
- `backup.retention_days` - 備份保留天數（預設：7）

備份目錄由環境變數 `BACKUP_DIR` 設定（預設：`/app/backups`），對應主機目錄為 `/root/project/work/backups`。

## 備份檔案格式

備份檔案命名格式：`{database}_{YYYYMMDD_HHMMSS}.sql.gz`

例如：
- `qa_tracker_20241211_020000.sql.gz`
- `church_20241211_020000.sql.gz`

## 還原備份

### 方法 1：使用命令行

```bash
# 解壓縮備份檔案
gunzip qa_tracker_20241211_020000.sql.gz

# 還原資料庫
docker compose exec -T mysql mysql -u root -prootpassword qa_tracker < qa_tracker_20241211_020000.sql

# 或使用壓縮檔案直接還原
gunzip < qa_tracker_20241211_020000.sql.gz | docker compose exec -T mysql mysql -u root -prootpassword qa_tracker
```

### 方法 2：使用後台維護頁面

1. 登入教會後台
2. 前往「系統設定」→「系統維護」
3. 切換到「備份管理」標籤
4. 下載需要的備份檔案
5. 使用命令行還原

## 注意事項

1. **備份目錄權限**：確保備份目錄有寫入權限
2. **磁碟空間**：定期檢查備份目錄的磁碟空間
3. **備份驗證**：建議定期測試還原備份，確保備份檔案可用
4. **安全**：備份檔案包含敏感資料，請妥善保管

## 故障排除

### 備份失敗

1. 檢查 MySQL 容器是否運行：
```bash
docker compose ps mysql
```

2. 檢查備份目錄是否存在且有寫入權限：
```bash
ls -la /root/project/work/backups
```

3. 查看後端日誌：
```bash
docker compose logs backend | grep "備份"
```

### 備份檔案過大

如果備份檔案過大，可以考慮：
1. 只備份必要的資料表
2. 使用 `--where` 參數過濾資料
3. 增加備份頻率，減少單次備份量

## 相關文檔

- 系統參數設定：教會後台 → 系統設定 → 系統維護 → 系統參數
- 備份管理：教會後台 → 系統設定 → 系統維護 → 備份管理
- 排程任務管理：教會後台 → 系統設定 → 排程管理
