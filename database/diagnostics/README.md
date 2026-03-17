# 診斷 SQL 腳本說明

本目錄包含用於診斷和檢查系統狀態的 SQL 腳本。

## 檔案說明

### 檢查腳本

- **`check_config.sql`** - 檢查 LINE Bot 配置
  - 檢查每日提醒配置
  - 檢查用戶的 LINE ID

- **`check_members.sql`** - 檢查用戶和費用記錄
  - 檢查用戶表中的用戶信息
  - 檢查費用記錄中的 member 分佈
  - 檢查今天的費用記錄

- **`check_old_jobs.sql`** - 檢查舊的定時任務
  - 檢查舊的費用提醒任務
  - 查看當前有效的費用提醒任務

- **`check-frontend-menus.sql`** - 檢查前台菜單配置
  - 檢查所有前台菜單（包括啟用和未啟用的）
  - 檢查前台菜單的權限設定
  - 檢查前台菜單的 URL 設定
  - 統計前台菜單數量

- **`diagnose_scheduled_jobs.sql`** - 診斷定時任務系統
  - 檢查當前資料庫中的任務
  - 檢查任務執行記錄
  - 檢查是否存在孤立的執行記錄（任務已被刪除但記錄還在）

### 清理腳本

- **`cleanup_orphaned_executions.sql`** - 清理孤立的執行記錄
  - 刪除任務已被刪除但執行記錄還在的孤立記錄
  - 重新檢查任務狀態

- **`remove_old_jobs.sql`** - 移除舊的定時任務
  - 刪除舊的重複任務
  - 確認刪除後的任務列表

## 使用方法

### 執行單個檢查腳本

```bash
# 檢查配置
docker compose exec mysql mysql -u root -prootpassword qa_tracker < mysql/diagnostics/check_config.sql

# 檢查成員
docker compose exec mysql mysql -u root -prootpassword qa_tracker < mysql/diagnostics/check_members.sql

# 診斷定時任務
docker compose exec mysql mysql -u root -prootpassword church < mysql/diagnostics/diagnose_scheduled_jobs.sql
```

### 執行所有檢查

使用 `run-all-checks.sql` 可以一次性執行所有檢查：

```bash
docker compose exec mysql mysql -u root -prootpassword qa_tracker < mysql/diagnostics/run-all-checks.sql
docker compose exec mysql mysql -u root -prootpassword church < mysql/diagnostics/run-all-checks.sql
```

## 注意事項

1. **備份資料庫**：執行清理腳本前，請先備份資料庫
2. **確認環境**：某些腳本針對特定資料庫（`qa_tracker` 或 `church`），請確認使用正確的資料庫
3. **只讀操作**：檢查腳本都是只讀的，不會修改資料
4. **清理操作**：清理腳本會修改資料，執行前請確認

